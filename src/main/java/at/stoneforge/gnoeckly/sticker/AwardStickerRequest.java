package at.stoneforge.gnoeckly.sticker;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AwardStickerRequest(@NotNull UUID stickerId, @Size(max = 140) String message) {
}
