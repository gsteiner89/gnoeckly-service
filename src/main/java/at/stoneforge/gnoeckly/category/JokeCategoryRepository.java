package at.stoneforge.gnoeckly.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JokeCategoryRepository extends JpaRepository<JokeCategory, UUID> {

    List<JokeCategory> findByDeletedAtIsNullOrderBySortOrderAscNameAsc();

    List<JokeCategory> findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAscNameAsc();

    Optional<JokeCategory> findByIdAndDeletedAtIsNull(UUID id);

    Optional<JokeCategory> findBySlugAndDeletedAtIsNull(String slug);

    List<JokeCategory> findByIdIn(Collection<UUID> ids);
}
