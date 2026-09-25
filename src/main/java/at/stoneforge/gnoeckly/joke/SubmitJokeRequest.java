package at.stoneforge.gnoeckly.joke;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Maximale Witzlaenge (Zeichen); die DB-Spalte {@code text} ist bewusst weiter (2000), damit kein Schema-Umbau noetig ist. */
public record SubmitJokeRequest(
        @Size(max = 120) String title,
        @NotBlank @Size(max = MAX_TEXT_LENGTH) String text,
        @NotNull UUID categoryId) {
    public static final int MAX_TEXT_LENGTH = 500;
}
