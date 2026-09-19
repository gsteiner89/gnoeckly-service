package at.stoneforge.gnoeckly.vote;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JokeVoteRepository extends JpaRepository<JokeVote, UUID> {

    Optional<JokeVote> findByJokeIdAndUserId(UUID jokeId, UUID userId);

    /** Fuer die {@code myVote}-Anreicherung einer Feed-Seite in einer Query (kein N+1). */
    List<JokeVote> findByUserIdAndJokeIdIn(UUID userId, Collection<UUID> jokeIds);
}
