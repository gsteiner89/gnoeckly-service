package at.stoneforge.gnoeckly.push;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PushTokenRepository extends JpaRepository<PushToken, UUID> {

    List<PushToken> findByUserIdAndDeletedAtIsNull(UUID userId);

    Optional<PushToken> findByToken(String token);

    /** Hartes Loeschen: ein Token ohne Geraet hat keinen Wert, Soft-Delete wuerde den Unique-Index blockieren. */
    void deleteByToken(String token);

    List<PushToken> findByTokenIn(Collection<String> tokens);
}
