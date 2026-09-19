package at.stoneforge.gnoeckly.joke;

import at.stoneforge.gnoeckly.sticker.JokeStickerSummary;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * {@code myVote} ist +1/-1 oder null (kein Vote bzw. anonym), {@code myFavorite} ist bei anonym false. {@code rejectionReason} wird nur in
 * der Eigen- und Admin-Sicht befuellt. {@code boosted} ist zur Antwortzeit ausgewertet
 * ({@code boostedUntil > now}), es gibt keinen Ablauf-Job.
 */
public record JokeResponse(UUID id, String title, String text, UUID categoryId, String categoryName,
                           UUID authorId, String authorNickname, JokeStatus status, int upvotes, int downvotes,
                           int score, boolean boosted, Instant boostedUntil, Instant approvedAt, Instant createdAt,
                           Integer myVote, String rejectionReason, List<JokeStickerSummary> stickers,
                           boolean myFavorite) {
}
