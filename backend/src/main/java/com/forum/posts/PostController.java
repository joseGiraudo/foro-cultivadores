package com.forum.posts;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.forum.shared.ContentStatus;
import com.forum.shared.PostType;
import com.forum.users.AuthUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/posts")
class PostController {

    private final PostService postService;

    PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PostResponse create(@AuthenticationPrincipal AuthUser principal, @Valid @RequestBody CreatePostRequest request) {
        return postService.create(principal, request);
    }

    @GetMapping
    Page<PostSummaryResponse> list(@AuthenticationPrincipal AuthUser principal,
                                   @RequestParam(required = false) Long categoryId,
                                   @RequestParam(required = false) Long authorId,
                                   @RequestParam(required = false) PostType postType,
                                   @RequestParam(required = false) ContentStatus status,
                                   @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return postService.list(categoryId, authorId, postType, status, principal, pageable);
    }

    @GetMapping("/{id}")
    PostResponse get(@AuthenticationPrincipal AuthUser principal, @PathVariable Long id) {
        return postService.get(id, principal);
    }

    @PatchMapping("/{id}")
    PostResponse update(@AuthenticationPrincipal AuthUser principal, @PathVariable Long id,
                        @Valid @RequestBody UpdatePostRequest request) {
        return postService.update(id, principal, request);
    }

    @PatchMapping("/{id}/status")
    PostResponse changeStatus(@AuthenticationPrincipal AuthUser principal, @PathVariable Long id,
                              @Valid @RequestBody ChangePostStatusRequest request) {
        return postService.changeStatus(id, principal, request.status());
    }
}
