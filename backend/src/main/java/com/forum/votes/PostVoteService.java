package com.forum.votes;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forum.posts.Post;
import com.forum.posts.PostService;
import com.forum.posts.PostVoteStats;
import com.forum.rules.ForumPolicy;
import com.forum.shared.ContentStatus;
import com.forum.shared.VoteValue;
import com.forum.shared.error.ApiException;
import com.forum.users.AuthUser;

@Service
public class PostVoteService {

    private final PostVoteRepository postVoteRepository;
    private final PostService postService;
    private final PostVoteStats postVoteStats;
    private final ForumPolicy policy;

    PostVoteService(PostVoteRepository postVoteRepository, PostService postService,
                    PostVoteStats postVoteStats, ForumPolicy policy) {
        this.postVoteRepository = postVoteRepository;
        this.postService = postService;
        this.postVoteStats = postVoteStats;
        this.policy = policy;
    }

    @Transactional
    public VoteResponse vote(Long postId, AuthUser principal, VoteValue value) {
        principal.ensureCanWrite();
        if (!principal.getRole().isAtLeast(policy.postVoteMinRole())) {
            throw ApiException.forbidden("Tu rol no permite votar posts");
        }
        Post post = postService.loadVisible(postId, principal);
        if (post.getStatus() != ContentStatus.ACTIVE) {
            throw ApiException.conflict("No se puede votar un post que no está activo");
        }
        if (!policy.isSelfVoteAllowed() && post.getAuthor().getId().equals(principal.getId())) {
            throw ApiException.forbidden("No podés votar tu propio post");
        }

        postVoteRepository.findByUserIdAndPostId(principal.getId(), postId)
                .ifPresentOrElse(
                        existing -> existing.changeValue(value),
                        () -> postVoteRepository.save(new PostVote(principal.getId(), postId, value)));

        int score = postVoteStats.scoresByPost(List.of(postId)).getOrDefault(postId, 0);
        return new VoteResponse(score, value);
    }

    @Transactional
    public void delete(Long postId, AuthUser principal) {
        principal.ensureCanWrite();
        postVoteRepository.deleteByUserIdAndPostId(principal.getId(), postId);
    }
}
