package at.stoneforge.gnoeckly.streak;

public enum StreakState {
    /** Noch nie aktiv. */
    NONE,
    /** Heute bereits aktiv. */
    ACTIVE_TODAY,
    /** Serie laeuft, heute fehlt noch eine Aktivitaet (ggf. rettet ein Freeze verpasste Tage). */
    AT_RISK,
    /** Serie ist gerissen; die naechste Aktivitaet beginnt bei 1. */
    BROKEN
}
