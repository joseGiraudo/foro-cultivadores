package com.forum.posts;

/** Conteo de votos de un post desglosado en upvotes y downvotes. */
public record VoteCount(int upvotes, int downvotes) {

    public static final VoteCount ZERO = new VoteCount(0, 0);

    /** score neto (upvotes − downvotes). */
    public int score() {
        return upvotes - downvotes;
    }
}
