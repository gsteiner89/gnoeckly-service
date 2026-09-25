package at.stoneforge.gnoeckly.streak;

import at.stoneforge.gnoeckly.config.GnoecklySettings;
import at.stoneforge.gnoeckly.sticker.StickerService;
import at.stoneforge.gnoeckly.wallet.CoinTransactionType;
import at.stoneforge.gnoeckly.wallet.WalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Tages-Streak. Aktiv zaehlt, wer votet oder einen Witz einreicht ({@link #touch} aus den jeweiligen
 * Services, in deren Transaktion). Ob eine Serie gerissen ist, wird nie per Job, sondern zur
 * Abrufzeit aus {@code lastActiveDate} und dem Freeze-Vorrat abgeleitet (Regel 8). Der Tageswechsel
 * folgt der injizierten {@link Clock}, damit Tests die Zeit steuern koennen.
 */
@Service
public class StreakService {

    /** Meilenstein-Tage; je Tag gibt es Gnoecken (SystemOption) und den Sticker {@code streak-<tag>}. */
    static final int[] MILESTONES = {7, 30, 100};

    private final UserStreakRepository repository;
    private final WalletService walletService;
    private final StickerService stickerService;
    private final GnoecklySettings settings;
    private final Clock clock;

    public StreakService(UserStreakRepository repository, WalletService walletService, StickerService stickerService,
                         GnoecklySettings settings, Clock clock) {
        this.repository = repository;
        this.walletService = walletService;
        this.stickerService = stickerService;
        this.settings = settings;
        this.clock = clock;
    }

    /** Verbucht Aktivitaet des Users heute; mehrfach am selben Tag ohne Wirkung. */
    @Transactional
    public void touch(UUID userId) {
        LocalDate today = LocalDate.now(clock);
        UserStreak streak = lockedStreak(userId);
        LocalDate last = streak.getLastActiveDate();
        if (last != null && !last.isBefore(today)) {
            return;
        }
        if (last == null) {
            streak.setCurrentStreak(1);
        } else {
            long missed = ChronoUnit.DAYS.between(last, today) - 1;
            if (missed == 0) {
                streak.setCurrentStreak(streak.getCurrentStreak() + 1);
            } else if (missed <= streak.getFreezes()) {
                streak.setFreezes(streak.getFreezes() - (int) missed);
                streak.setCurrentStreak(streak.getCurrentStreak() + 1);
            } else {
                streak.setCurrentStreak(1);
                streak.setLastMilestoneClaimed(0);
            }
        }
        streak.setLastActiveDate(today);
        streak.setLongestStreak(Math.max(streak.getLongestStreak(), streak.getCurrentStreak()));
        payOutMilestones(streak);
    }

    /** Anzeigezustand; schreibt nichts (auch aus {@code readOnly}-Transaktionen aufrufbar). */
    @Transactional(readOnly = true)
    public StreakResponse view(UUID userId) {
        LocalDate today = LocalDate.now(clock);
        UserStreak streak = repository.findByUserId(userId).orElse(null);
        int current = 0;
        int longest = 0;
        int freezes = 0;
        int claimed = 0;
        LocalDate last = null;
        StreakState state = StreakState.NONE;
        if (streak != null) {
            longest = streak.getLongestStreak();
            freezes = streak.getFreezes();
        }
        if (streak != null && streak.getLastActiveDate() != null) {
            last = streak.getLastActiveDate();
            claimed = streak.getLastMilestoneClaimed();
            long missed = ChronoUnit.DAYS.between(last, today) - 1;
            if (!last.isBefore(today)) {
                state = StreakState.ACTIVE_TODAY;
                current = streak.getCurrentStreak();
            } else if (missed <= freezes) {
                state = StreakState.AT_RISK;
                current = streak.getCurrentStreak();
            } else {
                state = StreakState.BROKEN;
                claimed = 0;
            }
        }
        Integer nextMilestone = null;
        Long nextCoins = null;
        for (int milestone : MILESTONES) {
            if (milestone > Math.max(current, claimed)) {
                nextMilestone = milestone;
                nextCoins = settings.streakMilestoneCoins(milestone);
                break;
            }
        }
        return new StreakResponse(current, longest, freezes, settings.streakMaxFreezes(), settings.streakFreezePrice(),
                last, state, nextMilestone, nextCoins);
    }

    /** Kauft einen Freeze gegen Gnoecken; 409 {@code STREAK_FREEZE_LIMIT} beim Vorratslimit. */
    @Transactional
    public StreakResponse buyFreeze(UUID userId) {
        UserStreak streak = lockedStreak(userId);
        int max = settings.streakMaxFreezes();
        if (streak.getFreezes() >= max) {
            throw new StreakFreezeLimitException(max);
        }
        walletService.debit(userId, settings.streakFreezePrice(), CoinTransactionType.STREAK_FREEZE, streak.getId(),
                "Streak-Freeze");
        streak.setFreezes(streak.getFreezes() + 1);
        return view(userId);
    }

    private void payOutMilestones(UserStreak streak) {
        for (int milestone : MILESTONES) {
            if (streak.getCurrentStreak() >= milestone && milestone > streak.getLastMilestoneClaimed()) {
                walletService.credit(streak.getUserId(), settings.streakMilestoneCoins(milestone),
                        CoinTransactionType.STREAK_REWARD, streak.getId(), null, "Streak: " + milestone + " Tage");
                stickerService.grant(streak.getUserId(), "streak-" + milestone);
                streak.setLastMilestoneClaimed(milestone);
            }
        }
    }

    private UserStreak lockedStreak(UUID userId) {
        return repository.findForUpdateByUserId(userId).orElseGet(() -> {
            UserStreak created = new UserStreak();
            created.setUserId(userId);
            return repository.saveAndFlush(created);
        });
    }
}
