package at.stoneforge.gnoeckly.sticker;

import java.util.UUID;

/** Aggregiert pro Witz: welcher Sticker wie oft verliehen wurde (Sticker-Leiste in der Karte). */
public record JokeStickerSummary(UUID stickerId, String slug, String name, String imageUrl, long count) {
}
