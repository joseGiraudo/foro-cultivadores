package com.forum.follows;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.forum.posts.FollowedAuthors;

@Component
class FollowedAuthorsImpl implements FollowedAuthors {

    private final FollowRepository followRepository;

    FollowedAuthorsImpl(FollowRepository followRepository) {
        this.followRepository = followRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> authorsFollowedBy(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return followRepository.followingIds(userId);
    }
}
