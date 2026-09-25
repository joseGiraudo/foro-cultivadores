package com.forum.votes;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forum.comments.Comment;
import com.forum.comments.CommentService;
import com.forum.comments.CommentVoteStats;
import com.forum.rules.ForumPolicy;
import com.forum.shared.ContentStatus;
import com.forum.shared.VoteValue;
import com.forum.shared.error.ApiException;
import com.forum.users.AuthUser;

@Service
public class CommentVoteService {

    private final CommentVoteRepository commentVoteRepository;
    private final CommentService commentService;
    private final CommentVoteStats commentVoteStats;
    private final ForumPolicy policy;

    CommentVoteService(CommentVoteRepository commentVoteRepository, CommentService commentService,
                       CommentVoteStats commentVoteStats, ForumPolicy policy) {
        this.commentVoteRepository = commentVoteRepository;
        this.commentService = commentService;
        this.commentVoteStats = commentVoteStats;
        this.policy = policy;
    }

    @Transactional
    public VoteResponse vote(Long commentId, AuthUser principal, VoteValue value) {
        principal.ensureCanWrite();
        if (!principal.getRole().isAtLeast(policy.commentVoteMinRole())) {
            throw ApiException.forbidden("Tu rol no permite votar comentarios");
        }
        Comment comment = commentService.getVisible(commentId, principal);
        if (comment.getPost().getStatus() != ContentStatus.ACTIVE) {
            throw ApiException.conflict("No se puede votar en un post que no está activo");
        }
        if (comment.getStatus() != ContentStatus.ACTIVE) {
            throw ApiException.conflict("No se puede votar un comentario que no está activo");
        }
        if (!policy.isSelfVoteAllowed() && comment.getAuthor().getId().equals(principal.getId())) {
            throw ApiException.forbidden("No podés votar tu propio comentario");
        }

        commentVoteRepository.findByUserIdAndCommentId(principal.getId(), commentId)
                .ifPresentOrElse(
                        existing -> existing.changeValue(value),
                        () -> commentVoteRepository.save(new CommentVote(principal.getId(), commentId, value)));

        int score = commentVoteStats.scoresByComment(List.of(commentId)).getOrDefault(commentId, 0);
        return new VoteResponse(score, value);
    }

    @Transactional
    public void delete(Long commentId, AuthUser principal) {
        principal.ensureCanWrite();
        commentVoteRepository.deleteByUserIdAndCommentId(principal.getId(), commentId);
    }
}
