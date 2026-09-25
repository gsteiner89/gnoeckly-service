package at.stoneforge.gnoeckly.push;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterPushTokenRequest(@NotBlank @Size(max = 512) String token, @NotNull PushToken.Platform platform) {
}
