package at.stoneforge.gnoeckly.sticker;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface JokeStickerRepository extends JpaRepository<JokeSticker, UUID> {

    /** Feed-Anreicherung: alle verliehenen Sticker einer Seite in einer Query, Gruppierung im Service. */
    List<JokeSticker> findByJokeIdIn(Collection<UUID> jokeIds);

    /** Daily-Quest "Sticker verleihen". */
    long countByGiverIdAndCreatedAtGreaterThanEqual(UUID giverId, java.time.Instant since);

    Page<JokeSticker> findByJokeIdOrderByCreatedAtDesc(UUID jokeId, Pageable pageable);
}
