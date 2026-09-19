package at.stoneforge.gnoeckly.vote;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** {@code value} ist +1 oder -1; 0 wird vom Service abgelehnt (Entfernen laeuft ueber DELETE). */
public record VoteRequest(@Min(-1) @Max(1) int value) {
}
