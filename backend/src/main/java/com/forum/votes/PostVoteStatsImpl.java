package com.forum.votes;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.forum.posts.PostVoteStats;
import com.forum.posts.VoteCount;
import com.forum.shared.VoteValue;

@Component
class PostVoteStatsImpl implements PostVoteStats {

    private final PostVoteRepository postVoteRepository;

    PostVoteStatsImpl(PostVoteRepository postVoteRepository) {
        this.postVoteRepository = postVoteRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Integer> scoresByPost(Collection<Long> postIds) {
        if (postIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Integer> scores = new LinkedHashMap<>();
        for (Object[] row : postVoteRepository.scoreRows(postIds, VoteValue.UPVOTE)) {
            scores.put((Long) row[0], ((Long) row[1]).intValue());
        }
        return scores;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, VoteCount> voteCountsByPost(Collection<Long> postIds) {
        if (postIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, VoteCount> counts = new LinkedHashMap<>();
        for (Object[] row : postVoteRepository.countRows(postIds, VoteValue.UPVOTE)) {
            counts.put((Long) row[0], new VoteCount(((Long) row[1]).intValue(), ((Long) row[2]).intValue()));
        }
        return counts;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, VoteValue> myVotesByPost(Collection<Long> postIds, Long userId) {
        if (userId == null || postIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, VoteValue> votes = new LinkedHashMap<>();
        List<Object[]> rows = postVoteRepository.myVoteRows(userId, postIds);
        for (Object[] row : rows) {
            votes.put((Long) row[0], (VoteValue) row[1]);
        }
        return votes;
    }
}
