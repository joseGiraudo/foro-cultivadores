package com.forum.users;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
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

    /** Baja de cuenta (anonimización). Cierra la sesión también. 204. */
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteMe(@AuthenticationPrincipal AuthUser principal, HttpServletRequest httpRequest) {
        userService.deleteMe(principal);
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
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
