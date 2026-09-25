package at.stoneforge.gnoeckly.streak;

import at.stoneforge.midgard.base.MidgardBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.envers.Audited;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Tages-Serie eines Users. Komposition wie {@code UserProfile}: referenziert {@code midgard_user}
 * per {@code userId}. Der Zustand wird nur bei Aktivitaet fortgeschrieben ({@link StreakService#touch});
 * ob die Serie inzwischen gerissen ist, folgt zur Abrufzeit aus {@code lastActiveDate} (kein Job).
 */
@Entity
@Table(name = "gnoeckly_user_streak")
@Audited
public class UserStreak extends MidgardBaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "current_streak", nullable = false)
    private int currentStreak;

    @Column(name = "longest_streak", nullable = false)
    private int longestStreak;

    @Column(name = "last_active_date")
    private LocalDate lastActiveDate;

    @Column(nullable = false)
    private int freezes;

    /** Hoechster in der laufenden Serie bereits ausgezahlter Meilenstein; 0 nach einem Reset. */
    @Column(name = "last_milestone_claimed", nullable = false)
    private int lastMilestoneClaimed;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
    }

    public int getLongestStreak() {
        return longestStreak;
    }

    public void setLongestStreak(int longestStreak) {
        this.longestStreak = longestStreak;
    }

    public LocalDate getLastActiveDate() {
        return lastActiveDate;
    }

    public void setLastActiveDate(LocalDate lastActiveDate) {
        this.lastActiveDate = lastActiveDate;
    }

    public int getFreezes() {
        return freezes;
    }

    public void setFreezes(int freezes) {
        this.freezes = freezes;
    }

    public int getLastMilestoneClaimed() {
        return lastMilestoneClaimed;
    }

    public void setLastMilestoneClaimed(int lastMilestoneClaimed) {
        this.lastMilestoneClaimed = lastMilestoneClaimed;
    }
}
