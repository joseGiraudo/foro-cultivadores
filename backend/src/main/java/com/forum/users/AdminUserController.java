package com.forum.users;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/users")
class AdminUserController {

    private final UserService userService;

    AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @PatchMapping("/{id}/role")
    UserResponse changeRole(@AuthenticationPrincipal AuthUser admin, @PathVariable Long id,
                            @Valid @RequestBody ChangeRoleRequest request) {
        return userService.changeRole(admin, id, request.role());
    }

    @PatchMapping("/{id}/status")
    UserResponse changeStatus(@AuthenticationPrincipal AuthUser admin, @PathVariable Long id,
                              @Valid @RequestBody ChangeStatusRequest request) {
        return userService.changeStatus(admin, id, request.status());
    }

    @GetMapping
    Page<UserResponse> list(@AuthenticationPrincipal AuthUser admin,
                            @RequestParam(required = false) Role role,
                            @RequestParam(required = false) UserStatus status,
                            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return userService.list(role, status, admin, pageable);
    }
}
