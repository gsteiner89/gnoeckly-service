package at.stoneforge.gnoeckly.streak;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserStreakRepository extends JpaRepository<UserStreak, UUID> {

    Optional<UserStreak> findByUserId(UUID userId);

    /** Erinnerungs-Job: Serien, deren letzter aktiver Tag {@code date} ist. */
    List<UserStreak> findByLastActiveDateAndCurrentStreakGreaterThanEqualAndDeletedAtIsNull(LocalDate date, int minStreak);

    /** Fortschreiben, Meilenstein-Auszahlung und Freeze-Kauf nur unter Zeilensperre (Idempotenz). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UserStreak> findForUpdateByUserId(UUID userId);
}
