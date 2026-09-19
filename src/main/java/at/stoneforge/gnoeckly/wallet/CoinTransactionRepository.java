package at.stoneforge.gnoeckly.wallet;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CoinTransactionRepository extends JpaRepository<CoinTransaction, UUID> {

    boolean existsByExternalReference(String externalReference);

    Page<CoinTransaction> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
