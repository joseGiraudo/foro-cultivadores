package com.forum.posts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
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

    // Lista no vacía para los IN (:authorIds)/(:categoryIds) de las queries nativas cuando ese filtro
    // NO aplica (su flag lo corto-circuita); evita un IN vacío inválido en SQL.
    private static final List<Long> PLACEHOLDER_IDS = List.of(-1L);

    private final PostRepository postRepository;
    private final CategoryService categoryService;
    private final UserService userService;
    private final ForumPolicy policy;
    private final PostVoteStats postVoteStats;
    private final PostCommentStats postCommentStats;
    private final PostImages postImages;
    private final PostImageWriter postImageWriter;
    private final PostSaveStats postSaveStats;
    private final FollowedAuthors followedAuthors;

    PostService(PostRepository postRepository, CategoryService categoryService, UserService userService,
                ForumPolicy policy, PostVoteStats postVoteStats, PostCommentStats postCommentStats,
                PostImages postImages, PostImageWriter postImageWriter, PostSaveStats postSaveStats,
                FollowedAuthors followedAuthors) {
        this.postRepository = postRepository;
        this.categoryService = categoryService;
        this.userService = userService;
        this.policy = policy;
        this.postVoteStats = postVoteStats;
        this.postCommentStats = postCommentStats;
        this.postImages = postImages;
        this.postImageWriter = postImageWriter;
        this.postSaveStats = postSaveStats;
        this.followedAuthors = followedAuthors;
    }

    @Transactional
    public PostResponse create(AuthUser principal, CreatePostRequest request) {
        return create(principal, request, List.of());
    }

    /**
     * Crea el post y, en la misma transacción, guarda las imágenes recibidas (consultas con fotos).
     * Si algo falla, todo revierte. Las imágenes se validan por tamaño acá (política) y por tipo real
     * dentro del writer (415). El orden de subida define la position (orden de la galería).
     */
    @Transactional
    public PostResponse create(AuthUser principal, CreatePostRequest request, List<byte[]> images) {
        principal.ensureCanWrite();
        requireCreationRole(principal, request.postType());
        if (images.size() > policy.maxImagesPerPost()) {
            throw ApiException.conflict("El post supera el máximo de imágenes permitidas");
        }
        long maxBytes = policy.maxImageFileSize().toBytes();
        for (byte[] image : images) {
            if (image.length > maxBytes) {
                throw ApiException.payloadTooLarge("Una imagen supera el tamaño máximo permitido");
            }
        }

        Set<Category> categories = categoryService.getEntitiesByIds(request.categoryIds());
        User author = userService.getEntityById(principal.getId());
        Post post = new Post(request.title(), request.content(), author, request.postType(), categories);
        Post saved = postRepository.save(post);
        for (byte[] image : images) {
            postImageWriter.store(saved.getId(), image);
        }
        return toResponse(saved, principal);
    }

    @Transactional(readOnly = true)
    public PostResponse get(Long id, AuthUser principal) {
        return toResponse(loadVisible(id, principal), principal);
    }

    /**
     * Tipos de post que el actor puede crear según su rol (regla configurable por tipo). La UI lo usa
     * para no ofrecer tipos que el backend igual rechazaría. Anónimo → vacío (crear requiere sesión).
     */
    public List<PostType> creatableTypes(AuthUser principal) {
        if (principal == null) {
            return List.of();
        }
        return Arrays.stream(PostType.values())
                .filter(type -> principal.getRole().isAtLeast(policy.postCreationMinRole(type)))
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> list(List<Long> categoryIds, Long authorId, PostType postType,
                                          ContentStatus statusFilter, String q, boolean following,
                                          AuthUser principal, Pageable pageable) {
        // Solo los moderadores ven estados distintos de ACTIVE y pueden filtrar por estado.
        ContentStatus effectiveStatus = isModerator(principal) ? statusFilter : ContentStatus.ACTIVE;

        // Feed "siguiendo": restringe a autores que el actor sigue. Sin sesión o sin seguidos → vacío.
        List<Long> followedIds = null;
        if (following) {
            if (principal == null) {
                return new PageImpl<>(List.of(), pageable, 0);
            }
            followedIds = followedAuthors.authorsFollowedBy(principal.getId());
            if (followedIds.isEmpty()) {
                return new PageImpl<>(List.of(), pageable, 0);
            }
        }

        boolean filterByAuthors = followedIds != null;
        List<Long> authorIdsParam = filterByAuthors ? followedIds : PLACEHOLDER_IDS;
        boolean hasCategories = categoryIds != null && !categoryIds.isEmpty();
        List<Long> categoryIdsParam = hasCategories ? categoryIds : PLACEHOLDER_IDS;
        String postTypeName = postType == null ? null : postType.name();
        String statusName = effectiveStatus == null ? null : effectiveStatus.name();

        Page<Post> page;
        if (StringUtils.hasText(q)) {
            // Búsqueda full-text: el orden lo fija ts_rank, así que se descarta el sort del Pageable.
            Pageable searchPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
            page = postRepository.search(q, authorId, postTypeName, statusName,
                    filterByAuthors, authorIdsParam, hasCategories, categoryIdsParam, searchPageable);
        } else if (hasCategories) {
            // Unión de categorías, priorizando los posts que matchean más de las elegidas (luego fecha).
            Pageable rankPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
            page = postRepository.rankByCategories(categoryIds, authorId, postTypeName, statusName,
                    filterByAuthors, authorIdsParam, rankPageable);
        } else {
            Specification<Post> spec = PostSpecifications.filter(authorId, postType, effectiveStatus, followedIds);
            page = postRepository.findAll(spec, pageable);
        }

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
        Map<Long, Long> saveCounts = postSaveStats.saveCountsByPost(ids);

        return posts.stream().map(post -> PostSummaryResponse.of(post,
                scores.getOrDefault(post.getId(), 0),
                counts.getOrDefault(post.getId(), 0L),
                myVotes.get(post.getId()),
                thumbnails.get(post.getId()),
                saved.contains(post.getId()),
                saveCounts.getOrDefault(post.getId(), 0L))).toList();
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
        long saveCount = postSaveStats.saveCountsByPost(ids).getOrDefault(post.getId(), 0L);
        return PostResponse.of(post, votes, commentCount, myVote, postImages.imagesForPost(post.getId()), saved,
                saveCount);
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
