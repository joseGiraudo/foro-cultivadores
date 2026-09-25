package com.forum.votes;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.forum.users.AuthUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/posts/{postId}/vote")
class PostVoteController {

    private final PostVoteService postVoteService;

    PostVoteController(PostVoteService postVoteService) {
        this.postVoteService = postVoteService;
    }

    @PutMapping
    VoteResponse vote(@AuthenticationPrincipal AuthUser principal, @PathVariable Long postId,
                      @Valid @RequestBody VoteRequest request) {
        return postVoteService.vote(postId, principal, request.value());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal AuthUser principal, @PathVariable Long postId) {
        postVoteService.delete(postId, principal);
    }
}
