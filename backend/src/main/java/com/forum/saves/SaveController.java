package com.forum.saves;

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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.forum.posts.PostSummaryResponse;
import com.forum.users.AuthUser;

@RestController
class SaveController {

    private final SaveService saveService;

    SaveController(SaveService saveService) {
        this.saveService = saveService;
    }

    @PostMapping("/posts/{id}/save")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void save(@AuthenticationPrincipal AuthUser principal, @PathVariable Long id) {
        saveService.save(id, principal);
    }

    @DeleteMapping("/posts/{id}/save")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void unsave(@AuthenticationPrincipal AuthUser principal, @PathVariable Long id) {
        saveService.unsave(id, principal);
    }

    @GetMapping("/me/saved-posts")
    Page<PostSummaryResponse> savedPosts(@AuthenticationPrincipal AuthUser principal,
                                         @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return saveService.listSaved(principal, pageable);
    }
}
