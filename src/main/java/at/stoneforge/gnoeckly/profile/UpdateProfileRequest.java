package at.stoneforge.gnoeckly.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_]{3,20}$", message = "3-20 Zeichen, nur Buchstaben, Ziffern und _") String nickname,
        @Size(max = 280) String bio) {
}
