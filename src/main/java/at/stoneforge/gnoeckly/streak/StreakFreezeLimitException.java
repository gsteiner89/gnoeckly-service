package at.stoneforge.gnoeckly.streak;

/** HTTP 409 {@code STREAK_FREEZE_LIMIT}: es sind bereits so viele Freezes vorhanden wie erlaubt. */
public class StreakFreezeLimitException extends RuntimeException {

    public StreakFreezeLimitException(int maxFreezes) {
        super("Du besitzt bereits die maximale Anzahl an Streak-Freezes (" + maxFreezes + ").");
    }
}
