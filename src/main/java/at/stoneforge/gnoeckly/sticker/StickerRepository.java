package at.stoneforge.gnoeckly.sticker;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StickerRepository extends JpaRepository<Sticker, UUID> {

    List<Sticker> findByDeletedAtIsNullOrderBySortOrderAscNameAsc();

    List<Sticker> findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAscNameAsc();

    Optional<Sticker> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Sticker> findBySlugAndDeletedAtIsNull(String slug);

    List<Sticker> findByIdIn(Collection<UUID> ids);

    /** Kauf: Bestand ({@code stockSold}) nur unter Zeilensperre erhoehen, sonst Ueberverkauf. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Sticker> findForUpdateByIdAndDeletedAtIsNull(UUID id);
}
