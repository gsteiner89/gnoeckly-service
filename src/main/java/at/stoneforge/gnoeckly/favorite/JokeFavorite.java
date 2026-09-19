package at.stoneforge.gnoeckly.favorite;

import at.stoneforge.midgard.base.MidgardBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.envers.Audited;

import java.util.UUID;

/** Lesezeichen eines Users auf einen Witz; genau eins pro User und Witz (Unique-Index). */
@Entity
@Table(name = "gnoeckly_joke_favorite")
@Audited
public class JokeFavorite extends MidgardBaseEntity {

    @Column(name = "joke_id", nullable = false, updatable = false)
    private UUID jokeId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

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
}
