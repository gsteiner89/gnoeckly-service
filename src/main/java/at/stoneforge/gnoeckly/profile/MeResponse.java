package at.stoneforge.gnoeckly.profile;

import at.stoneforge.gnoeckly.streak.StreakResponse;

import java.util.UUID;

/**
 * Alles, was die App nach Login/Refresh ueber den eingeloggten User braucht. Nickname und Balance
 * stehen NICHT im JWT - die UI laedt diesen Endpoint nach jedem Login, Refresh, Reward und Kauf neu.
 */
public record MeResponse(UUID userId, String email, String nickname, String bio, boolean superAdmin,
                         long balance, long karma, StreakResponse streak) {
}
