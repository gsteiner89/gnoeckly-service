package at.stoneforge.gnoeckly.push;

import at.stoneforge.midgard.base.MidgardBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.envers.Audited;

import java.time.Instant;
import java.util.UUID;

/**
 * FCM-/APNs-Geraete-Token eines Users. Der Token ist global eindeutig (Unique-Index); meldet sich
 * auf demselben Geraet ein anderer User an, wird er umgehaengt. Opt-out heisst: Token loeschen.
 */
@Entity
@Table(name = "gnoeckly_push_token")
@Audited
public class PushToken extends MidgardBaseEntity {

    public enum Platform {
        ANDROID, IOS
    }

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 512)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Platform platform;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }
}
