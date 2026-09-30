package com.forum.posts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forum.categories.Category;
import com.forum.categories.CategoryService;
import com.forum.rules.ForumPolicy;
import com.forum.shared.ContentStatus;
import com.forum.shared.ContentStatusTransitions;
import com.forum.shared.PostType;
import com.forum.shared.VoteValue;
import com.forum.shared.error.ApiException;
import com.forum.users.AuthUser;
import com.forum.users.User;
import com.forum.users.UserService;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final CategoryService categoryService;
    private final UserService userService;
    private final ForumPolicy policy;
    private final PostVoteStats postVoteStats;
    private final PostCommentStats postCommentStats;
    private final PostImages postImages;
    private final PostSaveStats postSaveStats;

    PostService(PostRepository postRepository, CategoryService categoryService, UserService userService,
                ForumPolicy policy, PostVoteStats postVoteStats, PostCommentStats postCommentStats,
                PostImages postImages, PostSaveStats postSaveStats) {
        this.postRepository = postRepository;
        this.categoryService = categoryService;
        this.userService = userService;
        this.policy = policy;
        this.postVoteStats = postVoteStats;
        this.postCommentStats = postCommentStats;
        this.postImages = postImages;
        this.postSaveStats = postSaveStats;
    }

    @Transactional
    public PostResponse create(AuthUser principal, CreatePostRequest request) {
        principal.ensureCanWrite();
        requireCreationRole(principal, request.postType());

        Set<Category> categories = categoryService.getEntitiesByIds(request.categoryIds());
        User author = userService.getEntityById(principal.getId());
        Post post = new Post(request.title(), request.content(), author, request.postType(), categories);
        return toResponse(postRepository.save(post), principal);
    }

    @Transactional(readOnly = true)
    public PostResponse get(Long id, AuthUser principal) {
        return toResponse(loadVisible(id, principal), principal);
    }

    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> list(Long categoryId, Long authorId, PostType postType,
                                          ContentStatus statusFilter, AuthUser principal, Pageable pageable) {
        // Solo los moderadores ven estados distintos de ACTIVE y pueden filtrar por estado.
        ContentStatus effectiveStatus = isModerator(principal) ? statusFilter : ContentStatus.ACTIVE;
        Specification<Post> spec = PostSpecifications.filter(categoryId, authorId, postType, effectiveStatus);
        Page<Post> page = postRepository.findAll(spec, pageable);

        return new PageImpl<>(toSummaries(page.getContent(), principal), pageable, page.getTotalElements());
    }

    /**
     * Arma resúmenes para una lista de ids preservando su orden (los no visibles se descartan).
     * Pensado para otros módulos que ya tienen un orden propio (ej: posts guardados).
     */
    @Transactional(readOnly = true)
    public List<PostSummaryResponse> summariesForIds(List<Long> ids, AuthUser principal) {
        if (ids.isEmpty()) {
            return List.of();
        }
        boolean moderator = isModerator(principal);
        Map<Long, Post> byId = new HashMap<>();
        for (Post post : postRepository.findAllById(ids)) {
            if (post.getStatus() == ContentStatus.ACTIVE || moderator) {
                byId.put(post.getId(), post);
            }
        }
        List<Post> ordered = new ArrayList<>();
        for (Long id : ids) {
            Post post = byId.get(id);
            if (post != null) {
                ordered.add(post);
            }
        }
        return toSummaries(ordered, principal);
    }

    private List<PostSummaryResponse> toSummaries(List<Post> posts, AuthUser principal) {
        List<Long> ids = posts.stream().map(Post::getId).toList();
        Map<Long, Integer> scores = postVoteStats.scoresByPost(ids);
        Map<Long, Long> counts = postCommentStats.commentCountsByPost(ids);
        Map<Long, VoteValue> myVotes = principal == null ? Map.of() : postVoteStats.myVotesByPost(ids, principal.getId());
        Map<Long, String> thumbnails = postImages.thumbnailUrlByPost(ids);
        Set<Long> saved = principal == null ? Set.of() : postSaveStats.savedPostIds(ids, principal.getId());

        return posts.stream().map(post -> PostSummaryResponse.of(post,
                scores.getOrDefault(post.getId(), 0),
                counts.getOrDefault(post.getId(), 0L),
                myVotes.get(post.getId()),
                thumbnails.get(post.getId()),
                saved.contains(post.getId()))).toList();
    }

    @Transactional
    public PostResponse update(Long id, AuthUser principal, UpdatePostRequest request) {
        principal.ensureCanWrite();
        Post post = loadVisible(id, principal);
        if (!isAuthor(post, principal)) {
            throw ApiException.forbidden("Solo el autor puede editar el post");
        }
        if (post.getStatus() != ContentStatus.ACTIVE) {
            throw ApiException.conflict("Solo se puede editar un post activo");
        }
        Set<Category> categories = request.categoryIds() != null
                ? categoryService.getEntitiesByIds(request.categoryIds())
                : null;
        post.edit(request.title(), request.content(), categories);
        return toResponse(post, principal);
    }

    @Transactional
    public PostResponse changeStatus(Long id, AuthUser principal, ContentStatus target) {
        principal.ensureCanWrite();
        Post post = loadVisible(id, principal);
        boolean author = isAuthor(post, principal);
        boolean moderator = isModerator(principal);
        if (!author && !moderator) {
            throw ApiException.forbidden("No tenés permiso para cambiar el estado de este post");
        }
        ContentStatusTransitions.validate(post.getStatus(), target, author, moderator);
        post.changeStatus(target);
        return toResponse(post, principal);
    }

    /**
     * Devuelve la entidad post visible para el actor, o 404. Pensado también para otros módulos
     * (comentarios, votos, imágenes) que necesitan validar visibilidad del post.
     */
    @Transactional(readOnly = true)
    public Post loadVisible(Long id, AuthUser principal) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Post no encontrado"));
        if (post.getStatus() != ContentStatus.ACTIVE && !isModerator(principal)) {
            throw ApiException.notFound("Post no encontrado");
        }
        return post;
    }

    private PostResponse toResponse(Post post, AuthUser principal) {
        List<Long> ids = List.of(post.getId());
        VoteCount votes = postVoteStats.voteCountsByPost(ids).getOrDefault(post.getId(), VoteCount.ZERO);
        long commentCount = postCommentStats.commentCountsByPost(ids).getOrDefault(post.getId(), 0L);
        VoteValue myVote = principal == null ? null
                : postVoteStats.myVotesByPost(ids, principal.getId()).get(post.getId());
        boolean saved = principal != null && postSaveStats.savedPostIds(ids, principal.getId()).contains(post.getId());
        return PostResponse.of(post, votes, commentCount, myVote, postImages.imagesForPost(post.getId()), saved);
    }

    private void requireCreationRole(AuthUser principal, PostType type) {
        if (!principal.getRole().isAtLeast(policy.postCreationMinRole(type))) {
            throw ApiException.forbidden("Tu rol no permite crear posts de tipo " + type);
        }
    }

    private boolean isModerator(AuthUser principal) {
        return principal != null && principal.getRole().isAtLeast(policy.moderationMinRole());
    }

    private boolean isAuthor(Post post, AuthUser principal) {
        return principal != null && post.getAuthor().getId().equals(principal.getId());
    }
}
