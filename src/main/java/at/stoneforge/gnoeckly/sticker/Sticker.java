package at.stoneforge.gnoeckly.sticker;

import at.stoneforge.midgard.base.MidgardBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.envers.Audited;

import java.time.Instant;

/**
 * Admin-kuratierter Katalog-Eintrag des Sticker-Marktplatzes. Das Bild liegt in Midgards
 * {@code StorageService} (Namespace {@code stickers}), hier nur der Key. {@code stockTotal == null}
 * bedeutet unlimitiert; sonst begrenzt {@code stockSold} den Verkauf (DB-Check).
 */
@Entity
@Table(name = "gnoeckly_sticker")
@Audited
public class Sticker extends MidgardBaseEntity {

    @Column(nullable = false, length = 40)
    private String slug;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(length = 280)
    private String description;

    @Column(name = "image_key")
    private String imageKey;

    @Column(name = "image_content_type", length = 100)
    private String imageContentType;

    @Column(nullable = false)
    private long price;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "stock_total")
    private Integer stockTotal;

    @Column(name = "stock_sold", nullable = false)
    private int stockSold;

    @Column(name = "available_from")
    private Instant availableFrom;

    @Column(name = "available_until")
    private Instant availableUntil;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /** {@code false} = reiner Belohnungs-Sticker (z. B. Streak-Meilenstein), nicht im Marktplatz kaeuflich. */
    @Column(nullable = false)
    private boolean purchasable = true;

    public boolean isPurchasable() {
        return purchasable;
    }

    public void setPurchasable(boolean purchasable) {
        this.purchasable = purchasable;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageKey() {
        return imageKey;
    }

    public void setImageKey(String imageKey) {
        this.imageKey = imageKey;
    }

    public String getImageContentType() {
        return imageContentType;
    }

    public void setImageContentType(String imageContentType) {
        this.imageContentType = imageContentType;
    }

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Integer getStockTotal() {
        return stockTotal;
    }

    public void setStockTotal(Integer stockTotal) {
        this.stockTotal = stockTotal;
    }

    public int getStockSold() {
        return stockSold;
    }

    public void setStockSold(int stockSold) {
        this.stockSold = stockSold;
    }

    public Instant getAvailableFrom() {
        return availableFrom;
    }

    public void setAvailableFrom(Instant availableFrom) {
        this.availableFrom = availableFrom;
    }

    public Instant getAvailableUntil() {
        return availableUntil;
    }

    public void setAvailableUntil(Instant availableUntil) {
        this.availableUntil = availableUntil;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isSoldOut() {
        return stockTotal != null && stockSold >= stockTotal;
    }

    public boolean isAvailableAt(Instant now) {
        boolean started = availableFrom == null || !availableFrom.isAfter(now);
        boolean notEnded = availableUntil == null || availableUntil.isAfter(now);
        return active && started && notEnded;
    }
}
