package at.stoneforge.gnoeckly.joke;

import java.time.Duration;
import java.time.Instant;

/** Zeitraum fuer Top-Feed und Rankings, bezogen auf {@code approvedAt}. */
public enum Period {
    DAY(Duration.ofDays(1)),
    WEEK(Duration.ofDays(7)),
    ALL(null);

    private final Duration window;

    Period(Duration window) {
        this.window = window;
    }

    /** {@code null} bedeutet: kein Zeitfilter. */
    public Instant sinceOrNull(Instant now) {
        return window == null ? null : now.minus(window);
    }
}
