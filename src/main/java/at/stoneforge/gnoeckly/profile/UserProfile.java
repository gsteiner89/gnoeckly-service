package at.stoneforge.gnoeckly.profile;

import at.stoneforge.midgard.base.MidgardBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.envers.Audited;

import java.util.UUID;

/**
 * Oeffentliches Gesicht eines Users (Nickname, Bio). Komposition statt Vererbung (midgard
 * CLAUDE.md Regel 2): referenziert {@code midgard_user} per {@code userId}, midgards {@code User}
 * kennt keinen Nickname und E-Mail darf nie oeffentlich sichtbar sein.
 */
@Entity
@Table(name = "gnoeckly_user_profile")
@Audited
public class UserProfile extends MidgardBaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false, length = 20)
    private String nickname;

    @Column(length = 280)
    private String bio;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }
}
