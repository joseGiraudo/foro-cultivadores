package com.forum.comments;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forum.posts.Post;
import com.forum.posts.PostService;
import com.forum.rules.CommentOrder;
import com.forum.rules.ForumPolicy;
import com.forum.shared.ContentStatus;
import com.forum.shared.ContentStatusTransitions;
import com.forum.shared.VoteValue;
import com.forum.shared.error.ApiException;
import com.forum.users.AuthUser;
import com.forum.users.User;
import com.forum.users.UserService;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostService postService;
    private final UserService userService;
    private final ForumPolicy policy;
    private final CommentVoteStats commentVoteStats;

    CommentService(CommentRepository commentRepository, PostService postService, UserService userService,
                   ForumPolicy policy, CommentVoteStats commentVoteStats) {
        this.commentRepository = commentRepository;
        this.postService = postService;
        this.userService = userService;
        this.policy = policy;
        this.commentVoteStats = commentVoteStats;
    }

    @Transactional
    public CommentResponse create(Long postId, AuthUser principal, CreateCommentRequest request) {
        principal.ensureCanWrite();
        if (!principal.getRole().isAtLeast(policy.commentMinRole())) {
            throw ApiException.forbidden("Tu rol no permite comentar");
        }
        Post post = postService.loadVisible(postId, principal);
        if (post.getStatus() != ContentStatus.ACTIVE) {
            throw ApiException.conflict("No se puede comentar un post que no está activo");
        }

        Comment parent = resolveParent(request.parentCommentId(), postId);
        User author = userService.getEntityById(principal.getId());
        Comment comment = commentRepository.save(new Comment(request.content(), author, post, parent));
        return single(comment, principal);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> list(Long postId, AuthUser principal) {
        Post post = postService.loadVisible(postId, principal);
        List<Comment> all = commentRepository.findByPostId(postId);

        List<Long> ids = all.stream().map(Comment::getId).toList();
        Map<Long, Integer> scores = commentVoteStats.scoresByComment(ids);
        Map<Long, VoteValue> myVotes = principal == null ? Map.of()
                : commentVoteStats.myVotesByComment(ids, principal.getId());

        CommentOrder order = policy.commentOrder(post.getPostType());
        return buildThread(all, order, scores, myVotes, isModerator(principal));
    }

    @Transactional
    public CommentResponse update(Long commentId, AuthUser principal, UpdateCommentRequest request) {
        principal.ensureCanWrite();
        Comment comment = load(commentId);
        if (!isAuthor(comment, principal)) {
            throw ApiException.forbidden("Solo el autor puede editar el comentario");
        }
        if (comment.getStatus() != ContentStatus.ACTIVE) {
            throw ApiException.conflict("Solo se puede editar un comentario activo");
        }
        comment.editContent(request.content());
        return single(comment, principal);
    }

    @Transactional
    public CommentResponse changeStatus(Long commentId, AuthUser principal, ContentStatus target) {
        principal.ensureCanWrite();
        Comment comment = load(commentId);
        boolean author = isAuthor(comment, principal);
        boolean moderator = isModerator(principal);
        if (!author && !moderator) {
            throw ApiException.forbidden("No tenés permiso para cambiar el estado de este comentario");
        }
        ContentStatusTransitions.validate(comment.getStatus(), target, author, moderator);
        comment.changeStatus(target);
        return single(comment, principal);
    }

    /**
     * Devuelve el comentario si el post al que pertenece es visible para el actor (404 si no).
     * Pensado para el módulo de votos, que luego valida que post y comentario estén ACTIVE.
     */
    @Transactional(readOnly = true)
    public Comment getVisible(Long commentId, AuthUser principal) {
        Comment comment = load(commentId);
        if (comment.getPost().getStatus() != ContentStatus.ACTIVE && !isModerator(principal)) {
            throw ApiException.notFound("Comentario no encontrado");
        }
        return comment;
    }

    private CommentResponse single(Comment comment, AuthUser principal) {
        List<Long> ids = List.of(comment.getId());
        Map<Long, Integer> scores = commentVoteStats.scoresByComment(ids);
        Map<Long, VoteValue> myVotes = principal == null ? Map.of()
                : commentVoteStats.myVotesByComment(ids, principal.getId());
        return toResponse(comment, 0, scores, myVotes, isModerator(principal));
    }

    private Comment resolveParent(Long parentCommentId, Long postId) {
        if (parentCommentId == null) {
            return null;
        }
        Comment parent = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> ApiException.badRequest("El comentario padre no existe"));
        if (!parent.getPost().getId().equals(postId)) {
            throw ApiException.badRequest("El comentario padre pertenece a otro post");
        }
        if (parent.getStatus() != ContentStatus.ACTIVE) {
            throw ApiException.badRequest("El comentario padre no está activo");
        }
        return parent;
    }

    private Comment load(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> ApiException.notFound("Comentario no encontrado"));
    }

    // --- Armado del hilo: lista plana en recorrido en profundidad ---

    private List<CommentResponse> buildThread(List<Comment> all, CommentOrder order, Map<Long, Integer> scores,
                                              Map<Long, VoteValue> myVotes, boolean isModerator) {
        Map<Long, List<Comment>> childrenByParent = new HashMap<>();
        List<Comment> roots = new ArrayList<>();
        for (Comment c : all) {
            Long parentId = parentId(c);
            if (parentId == null) {
                roots.add(c);
            } else {
                childrenByParent.computeIfAbsent(parentId, k -> new ArrayList<>()).add(c);
            }
        }

        Comparator<Comment> chronological =
                Comparator.comparing(Comment::getCreatedAt).thenComparing(Comment::getId);
        Comparator<Comment> rootComparator = order == CommentOrder.SCORE
                ? Comparator.comparingInt((Comment c) -> scores.getOrDefault(c.getId(), 0)).reversed()
                        .thenComparing(Comment::getCreatedAt).thenComparing(Comment::getId)
                : chronological;

        roots.sort(rootComparator);
        // Las respuestas (cualquier nivel debajo del primero) siempre van por createdAt ascendente.
        childrenByParent.values().forEach(list -> list.sort(chronological));

        List<CommentResponse> flattened = new ArrayList<>();
        for (Comment root : roots) {
            appendDepthFirst(root, 0, childrenByParent, scores, myVotes, isModerator, flattened);
        }
        return flattened;
    }

    private void appendDepthFirst(Comment comment, int depth, Map<Long, List<Comment>> childrenByParent,
                                  Map<Long, Integer> scores, Map<Long, VoteValue> myVotes,
                                  boolean isModerator, List<CommentResponse> out) {
        out.add(toResponse(comment, depth, scores, myVotes, isModerator));
        for (Comment child : childrenByParent.getOrDefault(comment.getId(), List.of())) {
            appendDepthFirst(child, depth + 1, childrenByParent, scores, myVotes, isModerator, out);
        }
    }

    private CommentResponse toResponse(Comment comment, int depth, Map<Long, Integer> scores,
                                       Map<Long, VoteValue> myVotes, boolean isModerator) {
        boolean showContent = comment.getStatus() == ContentStatus.ACTIVE || isModerator;
        return new CommentResponse(
                comment.getId(),
                showContent ? comment.getContent() : null,
                comment.getStatus(),
                CommentAuthor.of(comment.getAuthor()),
                parentId(comment),
                depth,
                scores.getOrDefault(comment.getId(), 0),
                myVotes.get(comment.getId()),
                comment.getCreatedAt(),
                comment.getUpdatedAt());
    }

    private Long parentId(Comment comment) {
        return comment.getParentComment() == null ? null : comment.getParentComment().getId();
    }

    private boolean isModerator(AuthUser principal) {
        return principal != null && principal.getRole().isAtLeast(policy.moderationMinRole());
    }

    private boolean isAuthor(Comment comment, AuthUser principal) {
        return principal != null && comment.getAuthor().getId().equals(principal.getId());
    }
}
