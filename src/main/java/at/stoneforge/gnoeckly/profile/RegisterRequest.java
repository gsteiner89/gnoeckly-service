package at.stoneforge.gnoeckly.profile;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_]{3,20}$", message = "3-20 Zeichen, nur Buchstaben, Ziffern und _") String nickname) {
}
