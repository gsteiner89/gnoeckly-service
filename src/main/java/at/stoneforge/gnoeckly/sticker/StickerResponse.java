package at.stoneforge.gnoeckly.sticker;

import java.time.Instant;
import java.util.UUID;

public record StickerResponse(UUID id, String slug, String name, String description, long price, String imageUrl,
                              boolean active, boolean soldOut, Integer stockTotal, int stockSold,
                              Instant availableFrom, Instant availableUntil, int sortOrder) {

    public static StickerResponse from(Sticker sticker) {
        String imageUrl = sticker.getImageKey() == null ? null : "/api/v1/public/stickers/" + sticker.getId() + "/image";
        return new StickerResponse(sticker.getId(), sticker.getSlug(), sticker.getName(), sticker.getDescription(),
                sticker.getPrice(), imageUrl, sticker.isActive(), sticker.isSoldOut(), sticker.getStockTotal(),
                sticker.getStockSold(), sticker.getAvailableFrom(), sticker.getAvailableUntil(), sticker.getSortOrder());
    }
}
