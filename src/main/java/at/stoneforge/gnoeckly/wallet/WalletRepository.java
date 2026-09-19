package at.stoneforge.gnoeckly.wallet;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    Optional<Wallet> findByUserId(UUID userId);

    /**
     * Fuer jede Buchung: sperrt die Wallet-Zeile bis zum Commit, damit zwei gleichzeitige
     * Abbuchungen (z.B. Submit + Kauf) den Kontostand nicht ueberschreiben. Contention pro User
     * ist gering, deshalb pessimistisch statt Retry auf {@code @Version}.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Wallet> findForUpdateByUserId(UUID userId);
}
