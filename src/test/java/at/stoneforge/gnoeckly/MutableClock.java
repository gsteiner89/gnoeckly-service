package at.stoneforge.gnoeckly;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/** Verstellbare Uhr fuer Tests, die den Tageswechsel (Streak, Daily Quests) steuern; feste Zeit 12:00 Vienna. */
public class MutableClock extends Clock {

    private static final ZoneId ZONE = ZoneId.of("Europe/Vienna");

    private volatile Instant now = Instant.now();

    public void setDate(LocalDate date) {
        now = date.atTime(12, 0).atZone(ZONE).toInstant();
    }

    public void setToday() {
        setDate(LocalDate.now(ZONE));
    }

    @Override
    public ZoneId getZone() {
        return ZONE;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return now;
    }
}
