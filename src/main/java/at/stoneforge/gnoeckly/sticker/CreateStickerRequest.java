package at.stoneforge.gnoeckly.sticker;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateStickerRequest(
        @NotBlank @Pattern(regexp = "^[a-z0-9-]{2,40}$") String slug,
        @NotBlank @Size(max = 80) String name,
        @Size(max = 280) String description,
        @Min(0) long price,
        @Min(1) Integer stockTotal,
        Instant availableFrom,
        Instant availableUntil,
        int sortOrder) {
}
