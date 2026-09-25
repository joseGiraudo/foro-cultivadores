package com.forum.votes;

import com.forum.shared.VoteValue;

/** Resultado de votar: el score actualizado del contenido y el voto del usuario. */
record VoteResponse(int score, VoteValue myVote) {
}
