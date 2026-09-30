package com.forum.saves;

import java.util.Collection;
import java.util.HashSet;
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
}
