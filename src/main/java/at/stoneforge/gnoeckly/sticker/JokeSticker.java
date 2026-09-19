package at.stoneforge.gnoeckly.sticker;

import at.stoneforge.midgard.base.MidgardBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.envers.Audited;

import java.util.UUID;

/** Ein an einen Witz verliehener Sticker (wie ein Reddit-Award), mit optionaler Nachricht. */
@Entity
@Table(name = "gnoeckly_joke_sticker")
@Audited
public class JokeSticker extends MidgardBaseEntity {

    @Column(name = "joke_id", nullable = false, updatable = false)
    private UUID jokeId;

    @Column(name = "sticker_id", nullable = false, updatable = false)
    private UUID stickerId;

    @Column(name = "giver_id", nullable = false, updatable = false)
    private UUID giverId;

    @Column(length = 140)
    private String message;

    public UUID getJokeId() {
        return jokeId;
    }

    public void setJokeId(UUID jokeId) {
        this.jokeId = jokeId;
    }

    public UUID getStickerId() {
        return stickerId;
    }

    public void setStickerId(UUID stickerId) {
        this.stickerId = stickerId;
    }

    public UUID getGiverId() {
        return giverId;
    }

    public void setGiverId(UUID giverId) {
        this.giverId = giverId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
