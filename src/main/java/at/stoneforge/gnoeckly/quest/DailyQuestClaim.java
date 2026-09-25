package at.stoneforge.gnoeckly.quest;

import at.stoneforge.midgard.base.MidgardBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.envers.Audited;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Einloesung einer Daily Quest. Der Unique-Index {@code (user_id, quest_key, quest_date)} ist die
 * eigentliche Idempotenz-Garantie; der Fortschritt selbst wird nie gespeichert, sondern zur
 * Abrufzeit aus den Daten des Tages berechnet.
 */
@Entity
@Table(name = "gnoeckly_daily_quest_claim")
@Audited
public class DailyQuestClaim extends MidgardBaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "quest_key", nullable = false, updatable = false, length = 20)
    private String questKey;

    @Column(name = "quest_date", nullable = false, updatable = false)
    private LocalDate questDate;

    @Column(nullable = false, updatable = false)
    private long reward;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getQuestKey() {
        return questKey;
    }

    public void setQuestKey(String questKey) {
        this.questKey = questKey;
    }

    public LocalDate getQuestDate() {
        return questDate;
    }

    public void setQuestDate(LocalDate questDate) {
        this.questDate = questDate;
    }

    public long getReward() {
        return reward;
    }

    public void setReward(long reward) {
        this.reward = reward;
    }
}
