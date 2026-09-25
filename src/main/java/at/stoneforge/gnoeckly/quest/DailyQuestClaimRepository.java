package at.stoneforge.gnoeckly.quest;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DailyQuestClaimRepository extends JpaRepository<DailyQuestClaim, UUID> {

    List<DailyQuestClaim> findByUserIdAndQuestDate(UUID userId, LocalDate questDate);

    boolean existsByUserIdAndQuestKeyAndQuestDate(UUID userId, String questKey, LocalDate questDate);
}
