package at.stoneforge.gnoeckly.sticker;

import java.time.Instant;
import java.util.UUID;

public record JokeStickerResponse(UUID id, UUID stickerId, String slug, String name, String imageUrl, UUID giverId,
                                  String giverNickname, String message, Instant createdAt) {
}
