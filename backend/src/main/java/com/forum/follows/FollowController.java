package com.forum.follows;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.forum.users.AuthUser;
import com.forum.users.UserResponse;

@RestController
@RequestMapping("/users/{id}")
class FollowController {

    private final FollowService followService;

    FollowController(FollowService followService) {
        this.followService = followService;
    }

    @PostMapping("/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void follow(@AuthenticationPrincipal AuthUser principal, @PathVariable Long id) {
        followService.follow(id, principal);
    }

    @DeleteMapping("/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void unfollow(@AuthenticationPrincipal AuthUser principal, @PathVariable Long id) {
        followService.unfollow(id, principal);
    }

    @GetMapping("/followers")
    Page<UserResponse> followers(@AuthenticationPrincipal AuthUser principal, @PathVariable Long id,
                                 @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return followService.followers(id, principal, pageable);
    }

    @GetMapping("/following")
    Page<UserResponse> following(@AuthenticationPrincipal AuthUser principal, @PathVariable Long id,
                                 @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return followService.following(id, principal, pageable);
    }
}
