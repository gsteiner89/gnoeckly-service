package at.stoneforge.gnoeckly.sticker;

import at.stoneforge.gnoeckly.joke.Joke;
import at.stoneforge.gnoeckly.joke.JokeRepository;
import at.stoneforge.gnoeckly.joke.JokeStatus;
import at.stoneforge.gnoeckly.profile.UserProfile;
import at.stoneforge.gnoeckly.profile.UserProfileRepository;
import at.stoneforge.gnoeckly.wallet.CoinTransactionType;
import at.stoneforge.gnoeckly.wallet.Wallet;
import at.stoneforge.gnoeckly.wallet.WalletService;
import at.stoneforge.midgard.storage.StorageMetadata;
import at.stoneforge.midgard.storage.StorageObject;
import at.stoneforge.midgard.storage.StorageService;
import at.stoneforge.midgard.web.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Sticker-Marktplatz: Katalog (Admin-kuratiert), Kauf gegen Gnoecken, Sammlung, Verleihen an
 * Witze. Kauf und Verleihen sperren die betroffenen Zeilen pessimistisch (Bestand, Sammlung), die
 * Wallet-Buchung laeuft in derselben Transaktion ueber {@link WalletService}.
 */
@Service
public class StickerService {

    public static final String STORAGE_NAMESPACE = "stickers";
    static final long MAX_IMAGE_BYTES = 512 * 1024;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/png", "image/webp", "image/svg+xml", "image/jpeg");

    private final StickerRepository stickerRepository;
    private final UserStickerRepository userStickerRepository;
    private final JokeStickerRepository jokeStickerRepository;
    private final JokeRepository jokeRepository;
    private final UserProfileRepository profileRepository;
    private final WalletService walletService;
    private final StorageService storageService;

    public StickerService(StickerRepository stickerRepository, UserStickerRepository userStickerRepository,
                          JokeStickerRepository jokeStickerRepository, JokeRepository jokeRepository,
                          UserProfileRepository profileRepository, WalletService walletService,
                          StorageService storageService) {
        this.stickerRepository = stickerRepository;
        this.userStickerRepository = userStickerRepository;
        this.jokeStickerRepository = jokeStickerRepository;
        this.jokeRepository = jokeRepository;
        this.profileRepository = profileRepository;
        this.walletService = walletService;
        this.storageService = storageService;
    }

    // --- Katalog -------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<StickerResponse> catalog() {
        Instant now = Instant.now();
        return stickerRepository.findByActiveTrueAndDeletedAtIsNullOrderBySortOrderAscNameAsc().stream()
                .filter(sticker -> sticker.isAvailableAt(now))
                .map(StickerResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StickerResponse> adminList() {
        return stickerRepository.findByDeletedAtIsNullOrderBySortOrderAscNameAsc().stream()
                .map(StickerResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public StickerResponse get(UUID id) {
        return StickerResponse.from(requireSticker(id));
    }

    // --- Kauf & Sammlung -----------------------------------------------------------------------

    @Transactional
    public PurchaseResponse purchase(UUID stickerId, UUID userId) {
        Sticker sticker = stickerRepository.findForUpdateByIdAndDeletedAtIsNull(stickerId)
                .orElseThrow(() -> new ResourceNotFoundException("Sticker " + stickerId + " nicht gefunden"));
        if (!sticker.isAvailableAt(Instant.now())) {
            throw new StickerUnavailableException(StickerUnavailableException.Code.STICKER_UNAVAILABLE);
        }
        if (sticker.isSoldOut()) {
            throw new StickerUnavailableException(StickerUnavailableException.Code.STICKER_SOLD_OUT);
        }
        Wallet wallet = walletService.debit(userId, sticker.getPrice(), CoinTransactionType.STICKER_PURCHASE,
                sticker.getId(), "Sticker: " + sticker.getName());
        sticker.setStockSold(sticker.getStockSold() + 1);

        UserSticker owned = userStickerRepository.findForUpdateByUserIdAndStickerId(userId, stickerId).orElseGet(() -> {
            UserSticker created = new UserSticker();
            created.setUserId(userId);
            created.setStickerId(stickerId);
            return created;
        });
        owned.setQuantity(owned.getQuantity() + 1);
        owned.setPurchasedTotal(owned.getPurchasedTotal() + 1);
        owned = userStickerRepository.save(owned);
        return new PurchaseResponse(toOwned(owned, sticker), wallet.getBalance());
    }

    @Transactional(readOnly = true)
    public List<OwnedStickerResponse> collection(UUID userId) {
        List<UserSticker> owned = userStickerRepository.findByUserIdOrderByCreatedAtDesc(userId);
        Map<UUID, Sticker> stickers = stickersById(owned.stream().map(UserSticker::getStickerId).toList());
        return owned.stream()
                .filter(entry -> stickers.containsKey(entry.getStickerId()))
                .map(entry -> toOwned(entry, stickers.get(entry.getStickerId())))
                .toList();
    }

    // --- Verleihen -----------------------------------------------------------------------------

    @Transactional
    public JokeStickerResponse award(UUID jokeId, UUID stickerId, UUID giverId, String message) {
        Joke joke = jokeRepository.findByIdAndDeletedAtIsNull(jokeId)
                .filter(candidate -> candidate.getStatus() == JokeStatus.APPROVED)
                .orElseThrow(() -> new ResourceNotFoundException("Witz " + jokeId + " nicht gefunden"));
        if (joke.getAuthorId().equals(giverId)) {
            throw new IllegalArgumentException("Eigene Witze können nicht ausgezeichnet werden");
        }
        Sticker sticker = requireSticker(stickerId);
        UserSticker owned = userStickerRepository.findForUpdateByUserIdAndStickerId(giverId, stickerId)
                .filter(entry -> entry.getQuantity() > 0)
                .orElseThrow(() -> new StickerUnavailableException(StickerUnavailableException.Code.STICKER_NOT_OWNED));
        owned.setQuantity(owned.getQuantity() - 1);

        JokeSticker award = new JokeSticker();
        award.setJokeId(jokeId);
        award.setStickerId(stickerId);
        award.setGiverId(giverId);
        award.setMessage(message);
        award = jokeStickerRepository.save(award);
        String giverNickname = profileRepository.findByUserIdAndDeletedAtIsNull(giverId)
                .map(UserProfile::getNickname).orElse("?");
        return toResponse(award, sticker, giverNickname);
    }

    @Transactional(readOnly = true)
    public Page<JokeStickerResponse> awardsOf(UUID jokeId, Pageable pageable) {
        Page<JokeSticker> page = jokeStickerRepository.findByJokeIdOrderByCreatedAtDesc(jokeId, pageable);
        Map<UUID, Sticker> stickers = stickersById(page.map(JokeSticker::getStickerId).toList());
        Map<UUID, String> nicknames = nicknames(page.map(JokeSticker::getGiverId).toList());
        return page.map(award -> toResponse(award, stickers.get(award.getStickerId()),
                nicknames.getOrDefault(award.getGiverId(), "?")));
    }

    /** Feed-Anreicherung fuer eine ganze Seite: eine Query fuer alle Awards, Gruppierung im Speicher. */
    @Transactional(readOnly = true)
    public Map<UUID, List<JokeStickerSummary>> summariesFor(Collection<UUID> jokeIds) {
        if (jokeIds.isEmpty()) {
            return Map.of();
        }
        List<JokeSticker> awards = jokeStickerRepository.findByJokeIdIn(jokeIds);
        Map<UUID, Sticker> stickers = stickersById(awards.stream().map(JokeSticker::getStickerId).distinct().toList());
        return awards.stream()
                .collect(Collectors.groupingBy(JokeSticker::getJokeId,
                        Collectors.collectingAndThen(
                                Collectors.groupingBy(JokeSticker::getStickerId, Collectors.counting()),
                                counts -> counts.entrySet().stream()
                                        .filter(entry -> stickers.containsKey(entry.getKey()))
                                        .map(entry -> summary(stickers.get(entry.getKey()), entry.getValue()))
                                        .sorted((a, b) -> Long.compare(b.count(), a.count()))
                                        .toList())));
    }

    // --- Admin ---------------------------------------------------------------------------------

    @Transactional
    public StickerResponse create(CreateStickerRequest request) {
        Sticker sticker = new Sticker();
        sticker.setSlug(request.slug());
        sticker.setName(request.name());
        sticker.setDescription(request.description());
        sticker.setPrice(request.price());
        sticker.setStockTotal(request.stockTotal());
        sticker.setAvailableFrom(request.availableFrom());
        sticker.setAvailableUntil(request.availableUntil());
        sticker.setSortOrder(request.sortOrder());
        sticker.setActive(true);
        return StickerResponse.from(stickerRepository.save(sticker));
    }

    @Transactional
    public StickerResponse update(UUID id, UpdateStickerRequest request) {
        Sticker sticker = requireSticker(id);
        if (request.stockTotal() != null && request.stockTotal() < sticker.getStockSold()) {
            throw new IllegalArgumentException("Bestand kann nicht unter die bereits verkaufte Menge gesenkt werden");
        }
        sticker.setName(request.name());
        sticker.setDescription(request.description());
        sticker.setPrice(request.price());
        sticker.setStockTotal(request.stockTotal());
        sticker.setAvailableFrom(request.availableFrom());
        sticker.setAvailableUntil(request.availableUntil());
        sticker.setSortOrder(request.sortOrder());
        sticker.setActive(request.active());
        return StickerResponse.from(sticker);
    }

    @Transactional
    public void softDelete(UUID id) {
        Sticker sticker = requireSticker(id);
        sticker.setActive(false);
        sticker.setDeletedAt(Instant.now());
    }

    @Transactional
    public StickerResponse uploadImage(UUID id, MultipartFile file) {
        Sticker sticker = requireSticker(id);
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Nur PNG, WebP, SVG oder JPEG erlaubt");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("Bild darf höchstens 512 KB groß sein");
        }
        String filename = file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()
                ? sticker.getSlug() : file.getOriginalFilename();
        StorageMetadata stored;
        try (InputStream content = file.getInputStream()) {
            stored = storageService.store(STORAGE_NAMESPACE, filename, contentType, content);
        } catch (IOException e) {
            throw new IllegalArgumentException("Bild konnte nicht gelesen werden", e);
        }
        if (sticker.getImageKey() != null) {
            storageService.delete(sticker.getImageKey());
        }
        sticker.setImageKey(stored.key());
        sticker.setImageContentType(contentType);
        return StickerResponse.from(sticker);
    }

    /** Liest das Bild vollstaendig (<= 512 KB), damit der StorageObject-Stream sicher geschlossen wird. */
    @Transactional(readOnly = true)
    public StickerImage image(UUID id) {
        Sticker sticker = requireSticker(id);
        if (sticker.getImageKey() == null) {
            throw new ResourceNotFoundException("Sticker " + id + " hat kein Bild");
        }
        try (StorageObject object = storageService.retrieve(sticker.getImageKey())) {
            byte[] bytes = object.content().readAllBytes();
            return new StickerImage(bytes, sticker.getImageContentType(), object.metadata().checksum());
        } catch (IOException e) {
            throw new IllegalStateException("Sticker-Bild konnte nicht gelesen werden", e);
        }
    }

    public record StickerImage(byte[] bytes, String contentType, String checksum) {
    }

    // --- intern --------------------------------------------------------------------------------

    private Sticker requireSticker(UUID id) {
        return stickerRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sticker " + id + " nicht gefunden"));
    }

    private Map<UUID, Sticker> stickersById(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return stickerRepository.findByIdIn(ids).stream()
                .collect(Collectors.toMap(Sticker::getId, Function.identity()));
    }

    private Map<UUID, String> nicknames(Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return profileRepository.findByUserIdInAndDeletedAtIsNull(userIds).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, UserProfile::getNickname, (a, b) -> a));
    }

    private static OwnedStickerResponse toOwned(UserSticker owned, Sticker sticker) {
        return new OwnedStickerResponse(sticker.getId(), sticker.getSlug(), sticker.getName(), imageUrl(sticker),
                owned.getQuantity(), owned.getPurchasedTotal());
    }

    private static JokeStickerResponse toResponse(JokeSticker award, Sticker sticker, String giverNickname) {
        return new JokeStickerResponse(award.getId(), award.getStickerId(),
                sticker == null ? null : sticker.getSlug(), sticker == null ? null : sticker.getName(),
                sticker == null ? null : imageUrl(sticker), award.getGiverId(), giverNickname, award.getMessage(),
                award.getCreatedAt());
    }

    private static JokeStickerSummary summary(Sticker sticker, long count) {
        return new JokeStickerSummary(sticker.getId(), sticker.getSlug(), sticker.getName(), imageUrl(sticker), count);
    }

    private static String imageUrl(Sticker sticker) {
        return sticker.getImageKey() == null ? null : "/api/v1/public/stickers/" + sticker.getId() + "/image";
    }
}
