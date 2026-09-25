package at.stoneforge.gnoeckly.joke;

import at.stoneforge.gnoeckly.category.JokeCategory;
import at.stoneforge.gnoeckly.category.JokeCategoryService;
import at.stoneforge.gnoeckly.config.GnoecklySettings;
import at.stoneforge.gnoeckly.profile.ProfileService;
import at.stoneforge.gnoeckly.push.PushMessage;
import at.stoneforge.gnoeckly.push.PushService;
import at.stoneforge.gnoeckly.streak.StreakService;
import at.stoneforge.gnoeckly.wallet.CoinTransactionType;
import at.stoneforge.gnoeckly.wallet.WalletService;
import at.stoneforge.midgard.web.PagedResponse;
import at.stoneforge.midgard.web.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

@Service
public class JokeService {

    /** Reddits Epoch-Offset aus der Hot-Formel; muss mit {@link JokeRepository#applyVoteDelta} uebereinstimmen. */
    static final long HOT_EPOCH_OFFSET = 1134028003L;
    static final double HOT_TIME_DIVISOR = 45000.0;

    private final JokeRepository jokeRepository;
    private final JokeReportRepository reportRepository;
    private final JokeCategoryService categoryService;
    private final WalletService walletService;
    private final GnoecklySettings settings;
    private final JokeResponseAssembler assembler;
    private final ProfileService profileService;
    private final StreakService streakService;
    private final PushService pushService;

    public JokeService(JokeRepository jokeRepository, JokeReportRepository reportRepository,
                       JokeCategoryService categoryService, WalletService walletService, GnoecklySettings settings,
                       JokeResponseAssembler assembler, ProfileService profileService,
                       StreakService streakService, PushService pushService) {
        this.jokeRepository = jokeRepository;
        this.reportRepository = reportRepository;
        this.categoryService = categoryService;
        this.walletService = walletService;
        this.settings = settings;
        this.assembler = assembler;
        this.profileService = profileService;
        this.streakService = streakService;
        this.pushService = pushService;
    }

    // --- oeffentlich ----------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PagedResponse<JokeResponse> feed(FeedSort sort, Period period, Collection<UUID> categoryIds, UUID viewerId, Pageable pageable) {
        return feed(sort, period, categoryIds, false, viewerId, pageable);
    }

    @Transactional(readOnly = true)
    public PagedResponse<JokeResponse> feed(FeedSort sort, Period period, Collection<UUID> categoryIds, boolean favoritesOnly,
                                            UUID viewerId, Pageable pageable) {
        Instant now = Instant.now();
        Page<Joke> page = jokeRepository.findAll(
                JokeSpecifications.approvedFeed(categoryIds, period.sinceOrNull(now), sort, now, favoritesOnly, viewerId), pageable);
        return toPaged(page, viewerId, false);
    }

    /** Alle freigegebenen Witze eines Users fuer sein oeffentliches Profil. */
    @Transactional(readOnly = true)
    public PagedResponse<JokeResponse> approvedByAuthor(UUID authorId, UUID viewerId, Pageable pageable) {
        return toPaged(jokeRepository.findByAuthorIdAndStatusAndDeletedAtIsNullOrderByScoreDescApprovedAtDescIdAsc(
                authorId, JokeStatus.APPROVED, pageable), viewerId, false);
    }

    @Transactional(readOnly = true)
    public JokeResponse getApproved(UUID id, UUID viewerId) {
        return assembler.assemble(requireApproved(id), viewerId, false);
    }

    // --- eingeloggt ------------------------------------------------------------------------------

    /**
     * Witz anlegen (PENDING) und Einreichgebuehr in derselben Transaktion abbuchen: reicht das
     * Guthaben nicht, rollt die {@link at.stoneforge.gnoeckly.wallet.InsufficientCoinsException}
     * (409) auch den Witz zurueck. Erst persistieren, damit die Buchung die Witz-ID referenziert.
     */
    @Transactional
    public JokeResponse submit(UUID authorId, SubmitJokeRequest request) {
        JokeCategory category = categoryService.requireActive(request.categoryId());
        Joke joke = new Joke();
        joke.setAuthorId(authorId);
        joke.setCategoryId(category.getId());
        joke.setTitle(blankToNull(request.title()));
        joke.setText(request.text().trim());
        joke.setStatus(JokeStatus.PENDING);
        joke = jokeRepository.save(joke);
        walletService.debit(authorId, settings.submitFee(), CoinTransactionType.SUBMIT_FEE, joke.getId(),
                "Einreichung");
        streakService.touch(authorId);
        return assembler.assemble(joke, authorId, true);
    }

    @Transactional(readOnly = true)
    public PagedResponse<JokeResponse> mine(UUID authorId, Pageable pageable) {
        return toPaged(jokeRepository.findByAuthorIdAndDeletedAtIsNullOrderByCreatedAtDesc(authorId, pageable), authorId, true);
    }

    @Transactional
    public JokeResponse boost(UUID jokeId, UUID userId) {
        Joke joke = requireApproved(jokeId);
        if (!joke.getAuthorId().equals(userId)) {
            throw new AccessDeniedException("Nur der Autor kann seinen Witz boosten");
        }
        walletService.debit(userId, settings.boostFee(), CoinTransactionType.BOOST, joke.getId(), "Boost");
        Instant now = Instant.now();
        Instant base = joke.isBoostedAt(now) ? joke.getBoostedUntil() : now;
        joke.setBoostedUntil(base.plus(settings.boostDuration()));
        return assembler.assemble(joke, userId, true);
    }

    /** Autor darf nur eigene, noch nicht moderierte Witze zurueckziehen. */
    @Transactional
    public void withdraw(UUID jokeId, UUID userId) {
        Joke joke = requireExisting(jokeId);
        if (!joke.getAuthorId().equals(userId)) {
            throw new AccessDeniedException("Nur der Autor kann seinen Witz zurückziehen");
        }
        if (joke.getStatus() != JokeStatus.PENDING) {
            throw new IllegalArgumentException("Nur unmoderierte Witze können zurückgezogen werden");
        }
        joke.setDeletedAt(Instant.now());
    }

    /** Idempotent: eine zweite Meldung desselben Users wird still ignoriert. */
    @Transactional
    public void report(UUID jokeId, UUID reporterId, String reason) {
        Joke joke = requireApproved(jokeId);
        if (reportRepository.existsByJokeIdAndReporterId(joke.getId(), reporterId)) {
            return;
        }
        JokeReport report = new JokeReport();
        report.setJokeId(joke.getId());
        report.setReporterId(reporterId);
        report.setReason(reason.trim());
        reportRepository.save(report);
    }

    // --- Admin ----------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PagedResponse<JokeResponse> byStatus(JokeStatus status, Pageable pageable) {
        return toPaged(jokeRepository.findByStatusAndDeletedAtIsNullOrderByCreatedAtAsc(status, pageable), null, true);
    }

    @Transactional
    public JokeResponse approve(UUID jokeId, UUID adminId) {
        Joke joke = requireExisting(jokeId);
        if (joke.getStatus() != JokeStatus.PENDING) {
            throw new IllegalArgumentException("Nur unmoderierte Witze können freigegeben werden");
        }
        Instant now = Instant.now();
        joke.setStatus(JokeStatus.APPROVED);
        joke.setApprovedAt(now);
        joke.setApprovedBy(adminId);
        joke.setRejectionReason(null);
        joke.setHotScore(initialHotScore(now));
        walletService.credit(joke.getAuthorId(), settings.coinsPerApprovedJoke(), CoinTransactionType.JOKE_APPROVED,
                joke.getId(), null, "Witz freigegeben");
        pushService.notify(joke.getAuthorId(), new PushMessage("Dein Witz ist freigegeben",
                "Er ist jetzt öffentlich, dazu gibt es " + settings.coinsPerApprovedJoke() + " Gnöcken.", "/me"));
        return assembler.assemble(joke, null, true);
    }

    @Transactional
    public JokeResponse reject(UUID jokeId, UUID adminId, String reason) {
        Joke joke = requireExisting(jokeId);
        if (joke.getStatus() != JokeStatus.PENDING) {
            throw new IllegalArgumentException("Nur unmoderierte Witze können abgelehnt werden");
        }
        joke.setStatus(JokeStatus.REJECTED);
        joke.setApprovedBy(adminId);
        joke.setRejectionReason(reason.trim());
        pushService.notify(joke.getAuthorId(), new PushMessage("Dein Witz wurde abgelehnt", reason.trim(), "/me"));
        return assembler.assemble(joke, null, true);
    }

    @Transactional
    public void adminDelete(UUID jokeId) {
        requireExisting(jokeId).setDeletedAt(Instant.now());
    }

    @Transactional(readOnly = true)
    public PagedResponse<JokeReportResponse> openReports(Pageable pageable) {
        Page<JokeReport> page = reportRepository.findByResolvedAtIsNullOrderByCreatedAtAsc(pageable);
        Map<UUID, String> nicknames = profileService.nicknames(page.map(JokeReport::getReporterId).toList());
        Map<UUID, Joke> jokes = jokeRepository.findAllById(page.map(JokeReport::getJokeId).toList()).stream()
                .collect(java.util.stream.Collectors.toMap(Joke::getId, joke -> joke));
        return PagedResponse.from(page, report -> new JokeReportResponse(report.getId(), report.getJokeId(),
                jokes.containsKey(report.getJokeId()) ? jokes.get(report.getJokeId()).getText() : null,
                report.getReporterId(), nicknames.getOrDefault(report.getReporterId(), "?"), report.getReason(),
                report.getCreatedAt(), report.getResolvedAt()));
    }

    @Transactional
    public void resolveReport(UUID reportId) {
        JokeReport report = reportRepository.findByIdAndDeletedAtIsNull(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Meldung " + reportId + " nicht gefunden"));
        report.setResolvedAt(Instant.now());
    }

    // --- intern ---------------------------------------------------------------------------------

    static double initialHotScore(Instant approvedAt) {
        return (approvedAt.getEpochSecond() - HOT_EPOCH_OFFSET) / HOT_TIME_DIVISOR;
    }

    Joke requireApproved(UUID id) {
        Joke joke = requireExisting(id);
        if (joke.getStatus() != JokeStatus.APPROVED) {
            throw new ResourceNotFoundException("Witz " + id + " nicht gefunden");
        }
        return joke;
    }

    Joke requireExisting(UUID id) {
        return jokeRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Witz " + id + " nicht gefunden"));
    }

    private PagedResponse<JokeResponse> toPaged(Page<Joke> page, UUID viewerId, boolean includeRejectionReason) {
        return new PagedResponse<>(assembler.assemble(page.getContent(), viewerId, includeRejectionReason),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
