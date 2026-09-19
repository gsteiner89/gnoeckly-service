package at.stoneforge.gnoeckly.joke;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JokeReportRepository extends JpaRepository<JokeReport, UUID> {

    boolean existsByJokeIdAndReporterId(UUID jokeId, UUID reporterId);

    Page<JokeReport> findByResolvedAtIsNullOrderByCreatedAtAsc(Pageable pageable);

    Optional<JokeReport> findByIdAndDeletedAtIsNull(UUID id);
}
