package com.forum.saves;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.forum.posts.PostSaveStats;

@Component
class PostSaveStatsImpl implements PostSaveStats {

    private final SavedPostRepository savedPostRepository;

    PostSaveStatsImpl(SavedPostRepository savedPostRepository) {
        this.savedPostRepository = savedPostRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> savedPostIds(Collection<Long> postIds, Long userId) {
        if (userId == null || postIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(savedPostRepository.savedAmong(userId, postIds));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> saveCountsByPost(Collection<Long> postIds) {
        if (postIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> counts = new LinkedHashMap<>();
        for (Object[] row : savedPostRepository.countByPostIds(postIds)) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }
}
