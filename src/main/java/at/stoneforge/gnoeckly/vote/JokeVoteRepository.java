package at.stoneforge.gnoeckly.vote;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JokeVoteRepository extends JpaRepository<JokeVote, UUID> {

    Optional<JokeVote> findByJokeIdAndUserId(UUID jokeId, UUID userId);

    /** Fuer die {@code myVote}-Anreicherung einer Feed-Seite in einer Query (kein N+1). */
    List<JokeVote> findByUserIdAndJokeIdIn(UUID userId, Collection<UUID> jokeIds);

    /** Daily-Quest "Bewerten": neu gesetzte Votes seit {@code since} auf Witze anderer Autoren. */
    @Query("""
            select count(v) from JokeVote v, Joke j
            where j.id = v.jokeId and v.userId = :userId and v.createdAt >= :since
              and v.deletedAt is null and j.deletedAt is null and j.authorId <> :userId
            """)
    long countVotesOnForeignJokesSince(@Param("userId") UUID userId, @Param("since") Instant since);
}
