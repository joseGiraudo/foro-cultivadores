package com.forum.votes;

import com.forum.shared.VoteValue;

import jakarta.validation.constraints.NotNull;

public record VoteRequest(
        @NotNull
        VoteValue value
) {
}
