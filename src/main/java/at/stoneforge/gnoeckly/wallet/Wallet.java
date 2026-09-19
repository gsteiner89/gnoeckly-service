package at.stoneforge.gnoeckly.wallet;

import at.stoneforge.midgard.base.MidgardBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.envers.Audited;

import java.util.UUID;

/**
 * Gnoecken-Guthaben eines Users (UI-Name "Gnoecken", im Code neutral "Wallet"/"Coin"). Genau ein
 * Wallet pro User, {@code balance >= 0} per DB-Check. Jede Aenderung laeuft ueber
 * {@link WalletService} mit {@code PESSIMISTIC_WRITE}-Lock und erzeugt einen
 * {@link CoinTransaction}-Ledger-Eintrag.
 */
@Entity
@Table(name = "gnoeckly_wallet")
@Audited
public class Wallet extends MidgardBaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false)
    private long balance;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public long getBalance() {
        return balance;
    }

    public void setBalance(long balance) {
        this.balance = balance;
    }
}
