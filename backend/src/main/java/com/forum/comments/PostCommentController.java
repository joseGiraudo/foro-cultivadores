package com.forum.comments;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.forum.users.AuthUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/posts/{postId}/comments")
class PostCommentController {

    private final CommentService commentService;

    PostCommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CommentResponse create(@AuthenticationPrincipal AuthUser principal, @PathVariable Long postId,
                           @Valid @RequestBody CreateCommentRequest request) {
        return commentService.create(postId, principal, request);
    }

    @GetMapping
    List<CommentResponse> list(@AuthenticationPrincipal AuthUser principal, @PathVariable Long postId) {
        return commentService.list(postId, principal);
    }
}
