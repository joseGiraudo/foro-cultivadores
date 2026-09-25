package com.forum.follows;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forum.rules.ForumPolicy;
import com.forum.shared.error.ApiException;
import com.forum.users.AuthUser;
import com.forum.users.User;
import com.forum.users.UserResponse;
import com.forum.users.UserService;

@Service
public class FollowService {

    private final FollowRepository followRepository;
    private final UserService userService;
    private final ForumPolicy policy;

    FollowService(FollowRepository followRepository, UserService userService, ForumPolicy policy) {
        this.followRepository = followRepository;
        this.userService = userService;
        this.policy = policy;
    }

    /** Idempotente: si ya lo sigue, no hace nada. */
    @Transactional
    public void follow(Long targetId, AuthUser principal) {
        principal.ensureCanWrite();
        if (!principal.getRole().isAtLeast(policy.followMinRole())) {
            throw ApiException.forbidden("Tu rol no permite seguir usuarios");
        }
        if (targetId.equals(principal.getId())) {
            throw ApiException.badRequest("No podés seguirte a vos mismo");
        }
        User target = userService.getEntityById(targetId);
        if (!target.getRole().isAtLeast(policy.followTargetMinRole())) {
            throw ApiException.badRequest("No se puede seguir a este usuario");
        }
        if (!followRepository.existsByFollowerIdAndFollowingId(principal.getId(), targetId)) {
            followRepository.save(new Follow(principal.getId(), targetId));
        }
    }

    /** Idempotente: no falla aunque no lo siga. */
    @Transactional
    public void unfollow(Long targetId, AuthUser principal) {
        principal.ensureCanWrite();
        followRepository.deleteByFollowerIdAndFollowingId(principal.getId(), targetId);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> followers(Long userId, AuthUser principal, Pageable pageable) {
        userService.getEntityById(userId); // 404 si el usuario no existe
        Page<Follow> page = followRepository.findByFollowingId(userId, pageable);
        List<Long> ids = page.getContent().stream().map(Follow::getFollowerId).toList();
        return new PageImpl<>(userService.responsesForIds(ids, principal), pageable, page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> following(Long userId, AuthUser principal, Pageable pageable) {
        userService.getEntityById(userId);
        Page<Follow> page = followRepository.findByFollowerId(userId, pageable);
        List<Long> ids = page.getContent().stream().map(Follow::getFollowingId).toList();
        return new PageImpl<>(userService.responsesForIds(ids, principal), pageable, page.getTotalElements());
    }
}
