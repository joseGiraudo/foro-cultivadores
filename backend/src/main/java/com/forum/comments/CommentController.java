package com.forum.comments;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.forum.users.AuthUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/comments")
class CommentController {

    private final CommentService commentService;

    CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PatchMapping("/{id}")
    CommentResponse update(@AuthenticationPrincipal AuthUser principal, @PathVariable Long id,
                           @Valid @RequestBody UpdateCommentRequest request) {
        return commentService.update(id, principal, request);
    }

    @PatchMapping("/{id}/status")
    CommentResponse changeStatus(@AuthenticationPrincipal AuthUser principal, @PathVariable Long id,
                                 @Valid @RequestBody ChangeCommentStatusRequest request) {
        return commentService.changeStatus(id, principal, request.status());
    }
}
