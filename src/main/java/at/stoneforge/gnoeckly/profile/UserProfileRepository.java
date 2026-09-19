package at.stoneforge.gnoeckly.profile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Soft-Delete wird in Midgard nie automatisch ausgeblendet (kein @SQLRestriction auf der
 * Basisklasse) - jede Abfrage filtert {@code deletedAt} selbst. Kein manuelles {@code tenant_id}:
 * das uebernimmt der Hibernate-Filter (midgard CLAUDE.md Regel 6).
 */
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    Optional<UserProfile> findByUserIdAndDeletedAtIsNull(UUID userId);

    List<UserProfile> findByUserIdInAndDeletedAtIsNull(Collection<UUID> userIds);

    boolean existsByNicknameIgnoreCaseAndDeletedAtIsNull(String nickname);
}
