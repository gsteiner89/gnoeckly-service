package at.stoneforge.gnoeckly.joke;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectJokeRequest(@NotBlank @Size(max = 500) String reason) {
}
