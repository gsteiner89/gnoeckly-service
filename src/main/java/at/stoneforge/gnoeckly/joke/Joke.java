package at.stoneforge.gnoeckly.joke;

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
 * Vote-Zaehler ({@code upvotes}/{@code downvotes}/{@code score}/{@code hotScore}) werden NICHT ueber
 * diese Entity, sondern ueber {@link JokeRepository#applyVoteDelta} atomar per SQL geaendert
 * (kein {@code @Version}-Konflikt bei gleichzeitigen Votes). Die Entity-Setter dafuer existieren
 * bewusst nur fuer die Initialisierung beim Approve. Die Vote-Historie liegt in
 * {@code gnoeckly_joke_vote(_aud)}, nicht in {@code gnoeckly_joke_aud}.
 */
@Entity
@Table(name = "gnoeckly_joke")
@Audited
public class Joke extends MidgardBaseEntity {

    @Column(name = "author_id", nullable = false, updatable = false)
    private UUID authorId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(length = 120)
    private String title;

    @Column(nullable = false, length = 2000)
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JokeStatus status = JokeStatus.PENDING;

    @Column(nullable = false)
    private int upvotes;

    @Column(nullable = false)
    private int downvotes;

    @Column(nullable = false)
    private int score;

    @Column(name = "hot_score", nullable = false)
    private double hotScore;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "boosted_until")
    private Instant boostedUntil;

    public UUID getAuthorId() {
        return authorId;
    }

    public void setAuthorId(UUID authorId) {
        this.authorId = authorId;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public JokeStatus getStatus() {
        return status;
    }

    public void setStatus(JokeStatus status) {
        this.status = status;
    }

    public int getUpvotes() {
        return upvotes;
    }

    public int getDownvotes() {
        return downvotes;
    }

    public int getScore() {
        return score;
    }

    public double getHotScore() {
        return hotScore;
    }

    public void setHotScore(double hotScore) {
        this.hotScore = hotScore;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public UUID getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(UUID approvedBy) {
        this.approvedBy = approvedBy;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Instant getBoostedUntil() {
        return boostedUntil;
    }

    public void setBoostedUntil(Instant boostedUntil) {
        this.boostedUntil = boostedUntil;
    }

    public boolean isBoostedAt(Instant now) {
        return boostedUntil != null && boostedUntil.isAfter(now);
    }
}
