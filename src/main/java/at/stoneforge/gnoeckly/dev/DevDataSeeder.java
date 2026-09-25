package at.stoneforge.gnoeckly.dev;

import at.stoneforge.gnoeckly.category.JokeCategory;
import at.stoneforge.gnoeckly.category.JokeCategoryRepository;
import at.stoneforge.gnoeckly.favorite.JokeFavorite;
import at.stoneforge.gnoeckly.favorite.JokeFavoriteRepository;
import at.stoneforge.gnoeckly.joke.Joke;
import at.stoneforge.gnoeckly.joke.JokeRepository;
import at.stoneforge.gnoeckly.joke.JokeStatus;
import at.stoneforge.gnoeckly.profile.UserProfile;
import at.stoneforge.gnoeckly.profile.UserProfileRepository;
import at.stoneforge.gnoeckly.sticker.Sticker;
import at.stoneforge.gnoeckly.sticker.StickerRepository;
import at.stoneforge.gnoeckly.sticker.StickerService;
import at.stoneforge.gnoeckly.vote.VoteService;
import at.stoneforge.gnoeckly.wallet.WalletService;
import at.stoneforge.midgard.tenant.Tenant;
import at.stoneforge.midgard.tenant.TenantContext;
import at.stoneforge.midgard.tenant.TenantHibernateFilterActivator;
import at.stoneforge.midgard.tenant.TenantRepository;
import at.stoneforge.midgard.upgrade.MidgardBootstrapAdminProperties;
import at.stoneforge.midgard.user.User;
import at.stoneforge.midgard.user.UserRepository;
import at.stoneforge.midgard.user.UserService;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Testdaten nur im {@code dev}-Profil: Testuser (Passwort {@value #PASSWORD}), zusaetzliche Kategorien, freigegebene,
 * offene und abgelehnte Witze, Votes, Favoriten sowie verliehene Sticker. Idempotent ueber den ersten Testuser: existiert
 * er, passiert nichts. Neu erzeugen: Testuser und -daten in der DB loeschen bzw. DB neu aufsetzen.
 *
 * Laeuft nach {@code GnoecklySeedRunner} (Order 10) und geht ueber die echten Services (Votes, Sticker), damit Zaehler,
 * Hot-Score und Ledger stimmen. Tenant-Handling wie im Seed-Runner (ausserhalb eines Requests).
 */
@Component
@Profile("dev")
@Order(20)
public class DevDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    static final String PASSWORD = "test1234";
    private static final String[] NICKNAMES = {"lena", "tobi", "sarah", "max", "nina"};
    private static final long START_COINS = 300;

    private record Seed(String category, String title, String text) {
    }

    private static final List<Seed> JOKES = List.of(
            new Seed("flachwitze", null, "Was ist grün und klopft an die Tür? Ein Klopfsalat."),
            new Seed("flachwitze", "Der Klassiker", "Warum können Geister so schlecht lügen? Man kann durch sie hindurchsehen."),
            new Seed("flachwitze", null, "Treffen sich zwei Jäger. Beide tot."),
            new Seed("flachwitze", null, "Was macht ein Pirat am Computer? Er drückt die Enter-Taste."),
            new Seed("flachwitze", "Kurz und schlecht", "Kommt ein Pferd in die Bar. Fragt der Barkeeper: „Warum das lange Gesicht?“"),
            new Seed("wortspiele", null, "Ich habe einen Schneemann gebaut und ihn Ölhaven genannt. Er hat sich nicht bedankt."),
            new Seed("wortspiele", "Backwaren", "Ich wollte Bäcker werden, aber mir fehlte der nötige Teig."),
            new Seed("wortspiele", null, "Ein Fotograf ist ein Mensch mit Blende und Charakter."),
            new Seed("wortspiele", "Nachtschicht", "Wer nachts arbeitet, hat am Tag keine Zeit für Tagträume."),
            new Seed("buero", null, "Meeting um 8 Uhr? Das hätte auch eine E-Mail sein können. Und die hätte man nicht lesen müssen."),
            new Seed("buero", "Homeoffice", "Ich arbeite von zu Hause aus. Meine Kollegen sehen nur die obere Hälfte, und auch die nur mit Kamera."),
            new Seed("buero", null, "Der Drucker funktioniert immer genau dann nicht, wenn man ihn braucht. Ein echter Teamplayer."),
            new Seed("buero", "Deadline", "Deadlines sind wie Wecker: Man hört sie kommen und drückt trotzdem auf Snooze."),
            new Seed("tiere", null, "Was sagt ein Hai nach einer langen Diät? Ich habe wieder Flossengewicht."),
            new Seed("tiere", "Katzenlogik", "Katze, ist tot, lustig? Nein, aber die Katze hat trotzdem den Vogel im Blick."),
            new Seed("tiere", null, "Warum sind Fische so schlau? Sie leben in Schulen."),
            new Seed("tiere", "Oma", "Die Oma ist moma ist das loma?\nviel mehr text\n\nund etwas frei und nun gehts weiter, ist ja fast wie ein Lied."),
            new Seed("schwarzer-humor", null, "Mein Wecker klingelt jeden Morgen um 6 Uhr. Das ist der einzige Moment, in dem ich ihm wirklich zuhöre."),
            new Seed("schwarzer-humor", null, "Ich habe keine Angst vor dem Tod. Ich will nur nicht dabei sein, wenn es passiert."),
            new Seed("programmierer", null, "Es gibt 10 Arten von Menschen: Die, die Binärcode verstehen, und die, die es nicht tun."),
            new Seed("programmierer", "Debugging", "Es funktioniert auf meinem Rechner. Dann liefern wir eben meinen Rechner aus."),
            new Seed("programmierer", null, "Ein SQL-Statement geht in eine Bar, geht zu zwei Tischen und fragt: Darf ich mich zu euch joinen?"),
            new Seed("kinderwitze", null, "Was ist orange und läuft durch den Wald? Eine Wanderine."),
            new Seed("kinderwitze", "Schule", "Fragt der Lehrer: Wer kann mir einen Satz mit „trotzdem“ sagen? Fritzchen: Trotzdem hab ich Hunger."));

    private final TenantRepository tenantRepository;
    private final MidgardBootstrapAdminProperties adminProperties;
    private final UserRepository userRepository;
    private final UserService userService;
    private final UserProfileRepository profileRepository;
    private final WalletService walletService;
    private final JokeCategoryRepository categoryRepository;
    private final JokeRepository jokeRepository;
    private final VoteService voteService;
    private final JokeFavoriteRepository favoriteRepository;
    private final StickerRepository stickerRepository;
    private final StickerService stickerService;
    private final TenantHibernateFilterActivator filterActivator;
    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;

    public DevDataSeeder(TenantRepository tenantRepository, MidgardBootstrapAdminProperties adminProperties,
                         UserRepository userRepository, UserService userService, UserProfileRepository profileRepository,
                         WalletService walletService, JokeCategoryRepository categoryRepository,
                         JokeRepository jokeRepository, VoteService voteService, JokeFavoriteRepository favoriteRepository,
                         StickerRepository stickerRepository, StickerService stickerService,
                         TenantHibernateFilterActivator filterActivator, EntityManager entityManager,
                         PlatformTransactionManager transactionManager) {
        this.tenantRepository = tenantRepository;
        this.adminProperties = adminProperties;
        this.userRepository = userRepository;
        this.userService = userService;
        this.profileRepository = profileRepository;
        this.walletService = walletService;
        this.categoryRepository = categoryRepository;
        this.jokeRepository = jokeRepository;
        this.voteService = voteService;
        this.favoriteRepository = favoriteRepository;
        this.stickerRepository = stickerRepository;
        this.stickerService = stickerService;
        this.filterActivator = filterActivator;
        this.entityManager = entityManager;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        Tenant tenant = tenantRepository.findBySlug(adminProperties.getTenantSlug()).orElse(null);
        if (tenant == null) {
            log.warn("Dev-Testdaten uebersprungen: Default-Tenant fehlt.");
            return;
        }
        TenantContext.setCurrentTenantId(tenant.getId());
        filterActivator.activate(entityManager, tenant.getId());
        try {
            if (userRepository.findByEmail(email(NICKNAMES[0])).isPresent()) {
                log.info("Dev-Testdaten schon vorhanden - uebersprungen.");
                return;
            }
            transactionTemplate.executeWithoutResult(status -> seed(tenant.getId()));
        } finally {
            filterActivator.deactivate(entityManager);
            TenantContext.clear();
        }
    }

    private void seed(UUID tenantId) {
        List<UUID> users = new ArrayList<>();
        for (String nickname : NICKNAMES) {
            User user = userService.create(tenantId, email(nickname), PASSWORD, null, null, "de");
            UserProfile profile = new UserProfile();
            profile.setUserId(user.getId());
            profile.setNickname(nickname);
            profileRepository.save(profile);
            walletService.createForUser(user.getId(), START_COINS);
            users.add(user.getId());
        }

        seedCategories();
        Random random = new Random(42);
        List<UUID> approved = new ArrayList<>();
        Instant now = Instant.now();
        for (int i = 0; i < JOKES.size(); i++) {
            Seed seed = JOKES.get(i);
            Instant approvedAt = now.minus(Duration.ofHours(3L + i * 9L));
            Joke joke = newJoke(users.get(i % users.size()), seed, JokeStatus.APPROVED);
            joke.setApprovedAt(approvedAt);
            joke.setHotScore((approvedAt.getEpochSecond() - 1134028003L) / 45000.0);
            approved.add(jokeRepository.save(joke).getId());
        }
        // Offen und abgelehnt, damit Moderation und "Meine Witze" etwas zeigen.
        newPending(users.get(0), "programmierer", "Frisch eingereicht", "Ein Witz, der noch auf Freigabe wartet.");
        newPending(users.get(0), "buero", null, "Noch ein offener Witz für die Moderations-Queue.");
        newPending(users.get(1), "tiere", null, "Auch dieser Witz wartet auf die Moderation.");
        Joke rejected = newJoke(users.get(0), new Seed("flachwitze", null, "Ein abgelehnter Witz zum Testen."), JokeStatus.REJECTED);
        rejected.setRejectionReason("Zu wenig Pointe.");
        jokeRepository.save(rejected);

        for (UUID jokeId : approved) {
            for (UUID userId : users) {
                double roll = random.nextDouble();
                if (roll < 0.6) {
                    voteService.vote(jokeId, userId, 1);
                } else if (roll < 0.75) {
                    voteService.vote(jokeId, userId, -1);
                }
            }
        }
        for (int i = 0; i < approved.size(); i += 3) {
            favorite(users.get(0), approved.get(i));
        }
        seedStickers(users, approved);
        log.info("Dev-Testdaten angelegt: {} Testuser ({}@{}, Passwort '{}'), {} Witze.", users.size(),
                NICKNAMES[0], EMAIL_DOMAIN, PASSWORD, JOKES.size() + 4);
    }

    private void seedCategories() {
        createCategoryIfMissing("programmierer", "Programmierer", "code", 60);
        createCategoryIfMissing("kinderwitze", "Kinderwitze", "child_care", 70);
    }

    private void createCategoryIfMissing(String slug, String name, String icon, int sortOrder) {
        if (categoryRepository.findBySlugAndDeletedAtIsNull(slug).isPresent()) {
            return;
        }
        JokeCategory category = new JokeCategory();
        category.setSlug(slug);
        category.setName(name);
        category.setIcon(icon);
        category.setSortOrder(sortOrder);
        category.setActive(true);
        categoryRepository.save(category);
    }

    private Joke newJoke(UUID authorId, Seed seed, JokeStatus status) {
        Joke joke = new Joke();
        joke.setAuthorId(authorId);
        joke.setCategoryId(categoryRepository.findBySlugAndDeletedAtIsNull(seed.category()).orElseThrow().getId());
        joke.setTitle(seed.title());
        joke.setText(seed.text());
        joke.setStatus(status);
        return joke;
    }

    private void newPending(UUID authorId, String category, String title, String text) {
        jokeRepository.save(newJoke(authorId, new Seed(category, title, text), JokeStatus.PENDING));
    }

    private void favorite(UUID userId, UUID jokeId) {
        JokeFavorite favorite = new JokeFavorite();
        favorite.setUserId(userId);
        favorite.setJokeId(jokeId);
        favoriteRepository.save(favorite);
    }

    /** Jeder Testuser bekommt Sticker geschenkt und verleiht zwei davon an Witze anderer. */
    private void seedStickers(List<UUID> users, List<UUID> jokes) {
        Sticker tear = stickerRepository.findBySlugAndDeletedAtIsNull("lachtraene").orElse(null);
        Sticker gold = stickerRepository.findBySlugAndDeletedAtIsNull("goldene-pointe").orElse(null);
        if (tear == null || gold == null) {
            return;
        }
        for (int u = 0; u < users.size(); u++) {
            UUID giver = users.get(u);
            stickerService.grant(giver, "lachtraene");
            stickerService.grant(giver, "goldene-pointe");
            // Autor von Witz j ist users[j % n]; die Offsets (u+1, u+2) sind nie der Verleiher selbst.
            int n = users.size();
            stickerService.award(jokes.get(n + (u + 1) % n), tear.getId(), giver, "Sehr gut!");
            stickerService.award(jokes.get(2 * n + (u + 2) % n), gold.getId(), giver, null);
        }
    }

    private static final String EMAIL_DOMAIN = "gnoeckly.test";

    private static String email(String nickname) {
        return nickname + "@" + EMAIL_DOMAIN;
    }
}
