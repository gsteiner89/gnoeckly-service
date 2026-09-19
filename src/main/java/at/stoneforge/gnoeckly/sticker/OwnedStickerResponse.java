package at.stoneforge.gnoeckly.sticker;

import java.util.UUID;

public record OwnedStickerResponse(UUID stickerId, String slug, String name, String imageUrl, int quantity,
                                   int purchasedTotal) {
}
