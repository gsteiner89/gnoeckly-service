package at.stoneforge.gnoeckly.vote;

import at.stoneforge.midgard.base.MidgardBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.envers.Audited;

import java.util.UUID;

/**
 * Genau ein Vote pro User und Witz (Unique-Index {@code (joke_id, user_id)}); {@code value} ist
 * +1 oder -1. Aenderung/Entfernen laeuft ueber {@link VoteService}, der die Zaehler am Witz atomar
 * nachzieht.
 */
@Entity
@Table(name = "gnoeckly_joke_vote")
@Audited
public class JokeVote extends MidgardBaseEntity {

    @Column(name = "joke_id", nullable = false, updatable = false)
    private UUID jokeId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false)
    private short value;

    public UUID getJokeId() {
        return jokeId;
    }

    public void setJokeId(UUID jokeId) {
        this.jokeId = jokeId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public short getValue() {
        return value;
    }

    public void setValue(short value) {
        this.value = value;
    }
}
