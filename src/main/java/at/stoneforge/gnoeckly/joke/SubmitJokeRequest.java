package at.stoneforge.gnoeckly.joke;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SubmitJokeRequest(
        @Size(max = 120) String title,
        @NotBlank @Size(max = 2000) String text,
        @NotNull UUID categoryId) {
}
