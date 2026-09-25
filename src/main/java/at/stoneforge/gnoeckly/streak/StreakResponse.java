package at.stoneforge.gnoeckly.streak;

import java.time.LocalDate;

/**
 * Anzeigezustand der Serie zur Abrufzeit. {@code current} ist die effektive Serie (0 bei
 * {@link StreakState#BROKEN}); {@code nextMilestone} ist {@code null}, wenn alle Meilensteine erreicht sind.
 */
public record StreakResponse(int current, int longest, int freezes, int maxFreezes, long freezePrice,
                             LocalDate lastActiveDate, StreakState state,
                             Integer nextMilestone, Long nextMilestoneCoins) {
}
