package com.forum.votes;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.forum.comments.CommentVoteStats;
import com.forum.shared.VoteValue;

@Component
class CommentVoteStatsImpl implements CommentVoteStats {

    private final CommentVoteRepository commentVoteRepository;

    CommentVoteStatsImpl(CommentVoteRepository commentVoteRepository) {
        this.commentVoteRepository = commentVoteRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Integer> scoresByComment(Collection<Long> commentIds) {
        if (commentIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Integer> scores = new LinkedHashMap<>();
        for (Object[] row : commentVoteRepository.scoreRows(commentIds, VoteValue.UPVOTE)) {
            scores.put((Long) row[0], ((Long) row[1]).intValue());
        }
        return scores;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, VoteValue> myVotesByComment(Collection<Long> commentIds, Long userId) {
        if (userId == null || commentIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, VoteValue> votes = new LinkedHashMap<>();
        List<Object[]> rows = commentVoteRepository.myVoteRows(userId, commentIds);
        for (Object[] row : rows) {
            votes.put((Long) row[0], (VoteValue) row[1]);
        }
        return votes;
    }
}
