package at.stoneforge.gnoeckly.sticker;

import at.stoneforge.midgard.base.MidgardBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.envers.Audited;

import java.util.UUID;

/**
 * Sammlung eines Users: pro Sticker eine Zeile mit {@code quantity} (aktuell besessen, sinkt beim
 * Verleihen) und {@code purchasedTotal} (jemals gekauft, fuer die Profil-Anzeige "Sammler").
 */
@Entity
@Table(name = "gnoeckly_user_sticker")
@Audited
public class UserSticker extends MidgardBaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "sticker_id", nullable = false, updatable = false)
    private UUID stickerId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "purchased_total", nullable = false)
    private int purchasedTotal;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getStickerId() {
        return stickerId;
    }

    public void setStickerId(UUID stickerId) {
        this.stickerId = stickerId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getPurchasedTotal() {
        return purchasedTotal;
    }

    public void setPurchasedTotal(int purchasedTotal) {
        this.purchasedTotal = purchasedTotal;
    }
}
