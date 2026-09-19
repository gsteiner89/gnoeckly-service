package at.stoneforge.gnoeckly.vote;

import java.util.UUID;

public record VoteResponse(UUID jokeId, int upvotes, int downvotes, int score, Integer myVote) {
}
