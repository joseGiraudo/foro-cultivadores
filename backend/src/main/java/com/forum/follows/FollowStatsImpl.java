package com.forum.follows;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.forum.users.FollowStats;

@Component
class FollowStatsImpl implements FollowStats {

    private final FollowRepository followRepository;

    FollowStatsImpl(FollowRepository followRepository) {
        this.followRepository = followRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> followerCounts(Collection<Long> userIds) {
        return toCountMap(userIds, followRepository::followerCounts);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> followingCounts(Collection<Long> userIds) {
        return toCountMap(userIds, followRepository::followingCounts);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> followingAmong(Long followerId, Collection<Long> targetIds) {
        if (followerId == null || targetIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(followRepository.followingAmong(followerId, targetIds));
    }

    private Map<Long, Long> toCountMap(Collection<Long> userIds,
                                       java.util.function.Function<Collection<Long>, java.util.List<Object[]>> query) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> counts = new LinkedHashMap<>();
        for (Object[] row : query.apply(userIds)) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }
}
