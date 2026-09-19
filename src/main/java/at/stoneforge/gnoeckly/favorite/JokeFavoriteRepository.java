package at.stoneforge.gnoeckly.favorite;

import at.stoneforge.gnoeckly.joke.Joke;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JokeFavoriteRepository extends JpaRepository<JokeFavorite, UUID> {

    Optional<JokeFavorite> findByJokeIdAndUserId(UUID jokeId, UUID userId);

    /** Fuer die {@code myFavorite}-Anreicherung einer Seite in einer Query (kein N+1). */
    List<JokeFavorite> findByUserIdAndJokeIdIn(UUID userId, Collection<UUID> jokeIds);

    /** Favoriten des Users, neueste zuerst; geloeschte und nicht (mehr) freigegebene Witze fallen raus. */
    @Query(value = "select j from Joke j, JokeFavorite f where f.jokeId = j.id and f.userId = :userId "
            + "and j.deletedAt is null and j.status = at.stoneforge.gnoeckly.joke.JokeStatus.APPROVED "
            + "order by f.createdAt desc",
            countQuery = "select count(j) from Joke j, JokeFavorite f where f.jokeId = j.id and f.userId = :userId "
                    + "and j.deletedAt is null and j.status = at.stoneforge.gnoeckly.joke.JokeStatus.APPROVED")
    Page<Joke> findFavoriteJokes(@Param("userId") UUID userId, Pageable pageable);
}
