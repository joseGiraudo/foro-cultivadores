package com.forum.votes;

import org.springframework.stereotype.Component;

import com.forum.shared.VoteValue;

/**
 * Helper de tests para sembrar votos desde cualquier módulo. Vive en {@code com.forum.votes} para
 * usar los repositorios y entidades package-private; es público para inyectarlo en otros tests.
 */
@Component
public class TestVotes {

    private final PostVoteRepository postVoteRepository;
    private final CommentVoteRepository commentVoteRepository;

    TestVotes(PostVoteRepository postVoteRepository, CommentVoteRepository commentVoteRepository) {
        this.postVoteRepository = postVoteRepository;
        this.commentVoteRepository = commentVoteRepository;
    }

    public void votePost(Long userId, Long postId, VoteValue value) {
        postVoteRepository.save(new PostVote(userId, postId, value));
    }

    public void voteComment(Long userId, Long commentId, VoteValue value) {
        commentVoteRepository.save(new CommentVote(userId, commentId, value));
    }
}
