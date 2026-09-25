package at.stoneforge.gnoeckly.joke;

import at.stoneforge.gnoeckly.ranking.AuthorRankingRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Feed-Abfragen laufen ueber {@link JpaSpecificationExecutor} (siehe {@link JokeSpecifications}),
 * weil Kategorie/Zeitraum optional sind und die Hot-Sortierung einen CASE-Ausdruck (Boost zuerst)
 * braucht - mit JPQL waeren das zwoelf Methodenvarianten oder null-Parameter-Tricks, die Postgres
 * bei UUID/Timestamp nicht typisieren kann.
 */
public interface JokeRepository extends JpaRepository<Joke, UUID>, JpaSpecificationExecutor<Joke> {

    Optional<Joke> findByIdAndDeletedAtIsNull(UUID id);

    /** Daily-Quest "Einreichen": alle nicht zurueckgezogenen Einreichungen seit {@code since}, egal welcher Status. */
    long countByAuthorIdAndCreatedAtGreaterThanEqualAndDeletedAtIsNull(UUID authorId, Instant since);

    Page<Joke> findByAuthorIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID authorId, Pageable pageable);

    /** Oeffentliche Witze eines Autors (Profil): nur freigegeben, bester Score zuerst, bei Gleichstand neueste Freigabe. */
    Page<Joke> findByAuthorIdAndStatusAndDeletedAtIsNullOrderByScoreDescApprovedAtDescIdAsc(UUID authorId, JokeStatus status, Pageable pageable);

    /** Moderations-Queue: aelteste Einreichung zuerst. */
    Page<Joke> findByStatusAndDeletedAtIsNullOrderByCreatedAtAsc(JokeStatus status, Pageable pageable);

    /**
     * Atomares Nachziehen der Vote-Zaehler inklusive Reddit-Hot-Score
     * ({@code sign(s) * log10(max(|s|, 1)) + (epoch(approved_at) - 1134028003) / 45000}) in EINEM
     * Statement: kein Lost-Update bei gleichzeitigen Votes, kein {@code @Version}-Konflikt.
     * Postgres' {@code log(x)} ist der Zehnerlogarithmus; 1134028003 ist Reddits Epoch-Offset
     * (reine Konstante, haelt die Zahlen klein). Bulk-Updates umgehen den Hibernate-Tenant-Filter -
     * sicher, weil {@code where id = :jokeId} und die ID zuvor tenant-gefiltert geladen wurde.
     * {@code clearAutomatically}: die Entity im Persistence-Context ist danach veraltet.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update gnoeckly_joke
               set upvotes    = upvotes + :dUp,
                   downvotes  = downvotes + :dDown,
                   score      = score + (:dUp - :dDown),
                   hot_score  = sign(score + (:dUp - :dDown))
                                * log(greatest(abs(score + (:dUp - :dDown)), 1))
                                + (extract(epoch from coalesce(approved_at, created_at)) - 1134028003) / 45000.0,
                   updated_at = now()
             where id = :jokeId""", nativeQuery = true)
    int applyVoteDelta(@Param("jokeId") UUID jokeId, @Param("dUp") int dUp, @Param("dDown") int dDown);

    @Query(value = """
            select j.authorId as authorId, sum(j.score) as karma, count(j) as approvedJokes
              from Joke j
             where j.status = at.stoneforge.gnoeckly.joke.JokeStatus.APPROVED
               and j.deletedAt is null
             group by j.authorId
             order by sum(j.score) desc, count(j) desc""",
            countQuery = """
            select count(distinct j.authorId) from Joke j
             where j.status = at.stoneforge.gnoeckly.joke.JokeStatus.APPROVED and j.deletedAt is null""")
    Page<AuthorRankingRow> rankAuthorsAllTime(Pageable pageable);

    @Query(value = """
            select j.authorId as authorId, sum(j.score) as karma, count(j) as approvedJokes
              from Joke j
             where j.status = at.stoneforge.gnoeckly.joke.JokeStatus.APPROVED
               and j.deletedAt is null
               and j.approvedAt >= :since
             group by j.authorId
             order by sum(j.score) desc, count(j) desc""",
            countQuery = """
            select count(distinct j.authorId) from Joke j
             where j.status = at.stoneforge.gnoeckly.joke.JokeStatus.APPROVED and j.deletedAt is null
               and j.approvedAt >= :since""")
    Page<AuthorRankingRow> rankAuthorsSince(@Param("since") Instant since, Pageable pageable);

    /** Karma eines einzelnen Autors (Profil): Summe der Scores seiner freigegebenen Witze. */
    @Query("""
            select coalesce(sum(j.score), 0) from Joke j
             where j.authorId = :authorId and j.status = at.stoneforge.gnoeckly.joke.JokeStatus.APPROVED
               and j.deletedAt is null""")
    long karmaOf(@Param("authorId") UUID authorId);

    long countByAuthorIdAndStatusAndDeletedAtIsNull(UUID authorId, JokeStatus status);
}
