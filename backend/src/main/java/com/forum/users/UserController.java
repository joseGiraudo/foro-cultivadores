package com.forum.users;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/users")
class UserController {

    private final UserService userService;

    UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    UserResponse me(@AuthenticationPrincipal AuthUser principal) {
        return userService.getMe(principal);
    }

    @PatchMapping("/me")
    UserResponse updateMe(@AuthenticationPrincipal AuthUser principal, @Valid @RequestBody UpdateMeRequest request) {
        return userService.updateMe(principal, request);
    }

    @GetMapping("/{id}")
    UserResponse profile(@AuthenticationPrincipal AuthUser principal, @PathVariable Long id) {
        return userService.getPublicProfile(id, principal);
    }

    @GetMapping("/by-username/{username}")
    UserResponse profileByUsername(@AuthenticationPrincipal AuthUser principal,
                                   @PathVariable String username) {
        return userService.getPublicProfileByUsername(username, principal);
    }
}
