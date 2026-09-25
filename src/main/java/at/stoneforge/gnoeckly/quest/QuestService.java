package at.stoneforge.gnoeckly.quest;

import at.stoneforge.gnoeckly.config.GnoecklySettings;
import at.stoneforge.gnoeckly.joke.JokeRepository;
import at.stoneforge.gnoeckly.sticker.JokeStickerRepository;
import at.stoneforge.gnoeckly.vote.JokeVoteRepository;
import at.stoneforge.gnoeckly.wallet.CoinTransactionType;
import at.stoneforge.gnoeckly.wallet.WalletService;
import at.stoneforge.midgard.web.ResourceNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Drei feste Daily Quests. Der Fortschritt wird nie gespeichert, sondern zur Abrufzeit aus den Daten
 * des heutigen Tages berechnet (Tagesgrenze laut {@link Clock}, kein Job). Eingeloest wird manuell;
 * der Unique-Index auf {@link DailyQuestClaim} verhindert Doppelbuchung auch bei parallelen Requests.
 */
@Service
public class QuestService {

    /** Quest-Definition: Schluessel (auch SystemOption-Zuordnung in {@link GnoecklySettings}) und Tagesziel. */
    private record Definition(String key, int target) {
    }

    private static final List<Definition> DEFINITIONS = List.of(
            new Definition("VOTE", 5),
            new Definition("SUBMIT", 1),
            new Definition("AWARD", 1));

    private final DailyQuestClaimRepository claimRepository;
    private final JokeVoteRepository voteRepository;
    private final JokeRepository jokeRepository;
    private final JokeStickerRepository jokeStickerRepository;
    private final WalletService walletService;
    private final GnoecklySettings settings;
    private final Clock clock;

    public QuestService(DailyQuestClaimRepository claimRepository, JokeVoteRepository voteRepository,
                        JokeRepository jokeRepository, JokeStickerRepository jokeStickerRepository,
                        WalletService walletService, GnoecklySettings settings, Clock clock) {
        this.claimRepository = claimRepository;
        this.voteRepository = voteRepository;
        this.jokeRepository = jokeRepository;
        this.jokeStickerRepository = jokeStickerRepository;
        this.walletService = walletService;
        this.settings = settings;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public QuestResponse today(UUID userId) {
        LocalDate today = LocalDate.now(clock);
        Instant since = startOfDay(today);
        Set<String> claimed = claimRepository.findByUserIdAndQuestDate(userId, today).stream()
                .map(DailyQuestClaim::getQuestKey).collect(Collectors.toSet());
        List<QuestResponse.Quest> quests = DEFINITIONS.stream()
                .map(definition -> toQuest(definition, userId, since, claimed.contains(definition.key())))
                .toList();
        return new QuestResponse(today, quests);
    }

    /** Loest eine erledigte Quest ein und bucht die Belohnung in derselben Transaktion. */
    @Transactional
    public QuestResponse claim(UUID userId, String questKey) {
        Definition definition = DEFINITIONS.stream()
                .filter(candidate -> candidate.key().equalsIgnoreCase(questKey))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Aufgabe " + questKey + " nicht gefunden"));
        LocalDate today = LocalDate.now(clock);
        if (claimRepository.existsByUserIdAndQuestKeyAndQuestDate(userId, definition.key(), today)) {
            throw new QuestException(QuestException.Code.QUEST_ALREADY_CLAIMED);
        }
        if (progress(definition, userId, startOfDay(today)) < definition.target()) {
            throw new QuestException(QuestException.Code.QUEST_NOT_COMPLETED);
        }
        long reward = settings.questCoins(definition.key());
        DailyQuestClaim claim = new DailyQuestClaim();
        claim.setUserId(userId);
        claim.setQuestKey(definition.key());
        claim.setQuestDate(today);
        claim.setReward(reward);
        try {
            claimRepository.saveAndFlush(claim);
        } catch (DataIntegrityViolationException e) {
            throw new QuestException(QuestException.Code.QUEST_ALREADY_CLAIMED);
        }
        walletService.credit(userId, reward, CoinTransactionType.DAILY_QUEST, claim.getId(), null,
                "Aufgabe: " + definition.key());
        return today(userId);
    }

    private QuestResponse.Quest toQuest(Definition definition, UUID userId, Instant since, boolean claimed) {
        int progress = Math.min(progress(definition, userId, since), definition.target());
        boolean done = progress >= definition.target();
        return new QuestResponse.Quest(definition.key(), definition.target(), progress,
                settings.questCoins(definition.key()), claimed, done && !claimed);
    }

    private int progress(Definition definition, UUID userId, Instant since) {
        long value = switch (definition.key()) {
            case "VOTE" -> voteRepository.countVotesOnForeignJokesSince(userId, since);
            case "SUBMIT" -> jokeRepository.countByAuthorIdAndCreatedAtGreaterThanEqualAndDeletedAtIsNull(userId, since);
            case "AWARD" -> jokeStickerRepository.countByGiverIdAndCreatedAtGreaterThanEqual(userId, since);
            default -> throw new IllegalStateException("Unbekannte Quest " + definition.key());
        };
        return (int) Math.min(value, Integer.MAX_VALUE);
    }

    private Instant startOfDay(LocalDate day) {
        return day.atStartOfDay(clock.getZone()).toInstant();
    }
}
