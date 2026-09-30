package com.forum.comments;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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

    @PostMapping("/{id}/pin")
    CommentResponse pin(@AuthenticationPrincipal AuthUser principal, @PathVariable Long id) {
        return commentService.pin(id, principal);
    }

    @DeleteMapping("/{id}/pin")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void unpin(@AuthenticationPrincipal AuthUser principal, @PathVariable Long id) {
        commentService.unpin(id, principal);
    }
}
