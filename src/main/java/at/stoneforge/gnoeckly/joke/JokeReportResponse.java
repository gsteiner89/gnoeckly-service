package at.stoneforge.gnoeckly.joke;

import java.time.Instant;
import java.util.UUID;

public record JokeReportResponse(UUID id, UUID jokeId, String jokeText, UUID reporterId, String reporterNickname,
                                 String reason, Instant createdAt, Instant resolvedAt) {
}
