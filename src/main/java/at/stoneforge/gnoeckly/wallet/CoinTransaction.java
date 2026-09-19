package at.stoneforge.gnoeckly.wallet;

import at.stoneforge.midgard.base.MidgardBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.envers.Audited;

import java.util.UUID;

/**
 * Ledger-Eintrag: {@code amount} ist vorzeichenbehaftet (Gutschrift positiv, Abbuchung negativ),
 * {@code balanceAfter} der Kontostand danach. {@code externalReference} traegt bei AD_REWARD die
 * AdMob-{@code transaction_id}; ein partieller Unique-Index darauf macht den SSV-Callback
 * idempotent (zweiter Callback mit derselben ID bucht nichts).
 */
@Entity
@Table(name = "gnoeckly_coin_transaction")
@Audited
public class CoinTransaction extends MidgardBaseEntity {

    @Column(name = "wallet_id", nullable = false, updatable = false)
    private UUID walletId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CoinTransactionType type;

    @Column(nullable = false)
    private long amount;

    @Column(name = "balance_after", nullable = false)
    private long balanceAfter;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "external_reference")
    private String externalReference;

    @Column(length = 255)
    private String description;

    public UUID getWalletId() {
        return walletId;
    }

    public void setWalletId(UUID walletId) {
        this.walletId = walletId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public CoinTransactionType getType() {
        return type;
    }

    public void setType(CoinTransactionType type) {
        this.type = type;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public long getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(long balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(UUID referenceId) {
        this.referenceId = referenceId;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
