package at.stoneforge.gnoeckly.joke;

import at.stoneforge.midgard.base.MidgardBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.envers.Audited;

import java.time.Instant;
import java.util.UUID;

/**
 * Meldung eines Witzes durch einen User. Pflicht fuer User-Generated-Content in den App-Stores
 * (Apple Guideline 1.2: Melden/Blockieren). Ein User kann denselben Witz nur einmal melden
 * (Unique-Index), der Admin sieht offene Meldungen unter {@code /api/admin/reports}.
 */
@Entity
@Table(name = "gnoeckly_joke_report")
@Audited
public class JokeReport extends MidgardBaseEntity {

    @Column(name = "joke_id", nullable = false, updatable = false)
    private UUID jokeId;

    @Column(name = "reporter_id", nullable = false, updatable = false)
    private UUID reporterId;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    public UUID getJokeId() {
        return jokeId;
    }

    public void setJokeId(UUID jokeId) {
        this.jokeId = jokeId;
    }

    public UUID getReporterId() {
        return reporterId;
    }

    public void setReporterId(UUID reporterId) {
        this.reporterId = reporterId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
