package at.stoneforge.gnoeckly.sticker;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserStickerRepository extends JpaRepository<UserSticker, UUID> {

    List<UserSticker> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<UserSticker> findByUserIdIn(Collection<UUID> userIds);

    Optional<UserSticker> findByUserIdAndStickerId(UUID userId, UUID stickerId);

    /** Kauf/Verleihen: {@code quantity} nur unter Zeilensperre veraendern. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UserSticker> findForUpdateByUserIdAndStickerId(UUID userId, UUID stickerId);
}
