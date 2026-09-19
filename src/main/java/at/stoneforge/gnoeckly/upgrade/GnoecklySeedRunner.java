package at.stoneforge.gnoeckly.upgrade;

import at.stoneforge.gnoeckly.category.JokeCategory;
import at.stoneforge.gnoeckly.category.JokeCategoryRepository;
import at.stoneforge.gnoeckly.profile.UserProfile;
import at.stoneforge.gnoeckly.profile.UserProfileRepository;
import at.stoneforge.gnoeckly.sticker.Sticker;
import at.stoneforge.gnoeckly.sticker.StickerRepository;
import at.stoneforge.gnoeckly.wallet.WalletRepository;
import at.stoneforge.gnoeckly.wallet.WalletService;
import at.stoneforge.midgard.group.Group;
import at.stoneforge.midgard.group.GroupRepository;
import at.stoneforge.midgard.menu.MenuCategory;
import at.stoneforge.midgard.menu.MenuCategoryRepository;
import at.stoneforge.midgard.menu.MenuItem;
import at.stoneforge.midgard.menu.MenuItemRepository;
import at.stoneforge.midgard.module.Module;
import at.stoneforge.midgard.module.ModuleRepository;
import at.stoneforge.midgard.module.TenantModule;
import at.stoneforge.midgard.module.TenantModuleRepository;
import at.stoneforge.midgard.tenant.Tenant;
import at.stoneforge.midgard.tenant.TenantContext;
import at.stoneforge.midgard.tenant.TenantHibernateFilterActivator;
import at.stoneforge.midgard.tenant.TenantRepository;
import at.stoneforge.midgard.upgrade.MidgardBootstrapAdminProperties;
import at.stoneforge.midgard.user.User;
import at.stoneforge.midgard.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;

/**
 * Idempotenter Seed bei JEDEM Start (kein versioniertes {@code MidgardUpgrade}): Default-Tenant,
 * Modul {@code gnoeckly}, Witzkategorien, Beispiel-Sticker, sowie Superadmin + Profil + Wallet,
 * sobald {@code midgard.bootstrap-admin.email/password} gesetzt sind, und das Admin-Menue, sobald
 * die Gruppe "Administrators" existiert.
 *
 * Warum kein {@code MidgardUpgrade}: {@code MidgardUpgradeRunner} traegt ein Upgrade auch dann als
 * angewendet in {@code midgard_upgrade_history} ein, wenn {@code apply()} mangels Bootstrap-Env-Vars
 * nichts getan hat. Ein einziger Start ohne {@code MIDGARD_BOOTSTRAP_ADMIN_*} haette Tenant und Seed
 * damit dauerhaft uebersprungen ("Default-Tenant nicht vorhanden" bei der Registrierung). Die App
 * braucht den Tenant aber immer, unabhaengig vom Admin. Envers-Audit ist trotzdem gegeben (alle
 * Writes laufen ueber JPA), und jeder Baustein prueft seine Existenz selbst.
 *
 * Reihenfolge zu Midgards Bootstrap-Upgrades ist egal: beide finden den Tenant per Slug bzw. den
 * Admin per E-Mail und legen nur an, was fehlt. Tenant-Handling wie in Midgards Upgrades: ausserhalb
 * eines HTTP-Requests {@link TenantContext} setzen UND den Hibernate-Filter aktivieren.
 */
@Component
@EnableConfigurationProperties(MidgardBootstrapAdminProperties.class)
public class GnoecklySeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(GnoecklySeedRunner.class);

    public static final String MODULE_KEY = "gnoeckly";
    public static final String MENU_CATEGORY_KEY = "gnoeckly";
    private static final String ADMIN_GROUP_NAME = "Administrators";
    private static final String ADMIN_NICKNAME = "admin";

    private final MidgardBootstrapAdminProperties properties;
    private final Environment environment;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModuleRepository moduleRepository;
    private final TenantModuleRepository tenantModuleRepository;
    private final GroupRepository groupRepository;
    private final MenuCategoryRepository menuCategoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final JokeCategoryRepository jokeCategoryRepository;
    private final StickerRepository stickerRepository;
    private final UserProfileRepository userProfileRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final TenantHibernateFilterActivator filterActivator;
    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;

    public GnoecklySeedRunner(MidgardBootstrapAdminProperties properties,
                              Environment environment,
                              TenantRepository tenantRepository,
                              UserRepository userRepository,
                              PasswordEncoder passwordEncoder,
                              ModuleRepository moduleRepository,
                              TenantModuleRepository tenantModuleRepository,
                              GroupRepository groupRepository,
                              MenuCategoryRepository menuCategoryRepository,
                              MenuItemRepository menuItemRepository,
                              JokeCategoryRepository jokeCategoryRepository,
                              StickerRepository stickerRepository,
                              UserProfileRepository userProfileRepository,
                              WalletRepository walletRepository,
                              WalletService walletService,
                              TenantHibernateFilterActivator filterActivator,
                              EntityManager entityManager,
                              PlatformTransactionManager transactionManager) {
        this.properties = properties;
        this.environment = environment;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.moduleRepository = moduleRepository;
        this.tenantModuleRepository = tenantModuleRepository;
        this.groupRepository = groupRepository;
        this.menuCategoryRepository = menuCategoryRepository;
        this.menuItemRepository = menuItemRepository;
        this.jokeCategoryRepository = jokeCategoryRepository;
        this.stickerRepository = stickerRepository;
        this.userProfileRepository = userProfileRepository;
        this.walletRepository = walletRepository;
        this.walletService = walletService;
        this.filterActivator = filterActivator;
        this.entityManager = entityManager;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!environment.getProperty("spring.flyway.enabled", Boolean.class, Boolean.TRUE)) {
            log.info("spring.flyway.enabled=false - GnoecklySeedRunner wird uebersprungen.");
            return;
        }
        transactionTemplate.executeWithoutResult(status -> seed());
    }

    private void seed() {
        Tenant tenant = tenantRepository.findBySlug(properties.getTenantSlug()).orElseGet(() -> {
            Tenant created = new Tenant();
            created.setName(properties.getTenantName());
            created.setSlug(properties.getTenantSlug());
            log.info("Default-Tenant '{}' angelegt.", properties.getTenantSlug());
            return tenantRepository.save(created);
        });
        Module module = moduleRepository.findByKey(MODULE_KEY).orElseGet(() -> {
            Module created = new Module();
            created.setKey(MODULE_KEY);
            created.setLabelKey("gnoeckly.modules.gnoeckly");
            created.setDescription("Gnöckly Witze-App");
            return moduleRepository.save(created);
        });
        UUID tenantId = tenant.getId();
        tenantModuleRepository.findByTenantIdAndModuleId(tenantId, module.getId()).orElseGet(() -> {
            TenantModule tenantModule = new TenantModule();
            tenantModule.setTenantId(tenantId);
            tenantModule.setModuleId(module.getId());
            tenantModule.setEnabled(true);
            tenantModule.setActivatedAt(Instant.now());
            return tenantModuleRepository.save(tenantModule);
        });

        TenantContext.setCurrentTenantId(tenantId);
        filterActivator.activate(entityManager, tenantId);
        try {
            ensureSuperadmin(tenantId);
            seedCategories();
            seedStickers();
            seedAdminProfileAndWallet();
            seedAdminMenu(module);
        } finally {
            filterActivator.deactivate(entityManager);
            TenantContext.clear();
        }
        log.info("Gnoeckly-Seed fuer Tenant '{}' geprueft.", tenant.getSlug());
    }

    /**
     * Legt den Superadmin an, falls die Env-Vars gesetzt sind und der User fehlt - auch dann, wenn
     * Midgards {@code MidgardBootstrapAdminUpgrade} bei einem frueheren Start ohne Env-Vars bereits
     * als "angewendet" verbucht wurde.
     */
    private void ensureSuperadmin(UUID tenantId) {
        if (isBlank(properties.getEmail()) || isBlank(properties.getPassword())) {
            return;
        }
        if (userRepository.findByEmail(properties.getEmail()).isPresent()) {
            return;
        }
        User admin = new User();
        admin.setTenantId(tenantId);
        admin.setEmail(properties.getEmail());
        admin.setPasswordHash(passwordEncoder.encode(properties.getPassword()));
        admin.setSuperAdmin(true);
        userRepository.save(admin);
        log.info("Superadmin {} angelegt.", properties.getEmail());
    }

    private void seedAdminMenu(Module module) {
        Group administrators = groupRepository.findByName(ADMIN_GROUP_NAME).orElse(null);
        if (administrators == null) {
            return;
        }
        MenuCategory category = menuCategoryRepository.findByKey(MENU_CATEGORY_KEY).orElseGet(() -> {
            MenuCategory created = new MenuCategory();
            created.setKey(MENU_CATEGORY_KEY);
            created.setLabelKey("gnoeckly.menu.category");
            created.setIcon("fa-solid fa-face-laugh-squint");
            created.setSortOrder(5);
            return menuCategoryRepository.save(created);
        });
        findOrCreateMenuItem("gnoeckly-moderation", "gnoeckly.menu.moderation", "/moderation",
                "fa-solid fa-gavel", category.getId(), module.getId(), 10, administrators);
        findOrCreateMenuItem("gnoeckly-categories", "gnoeckly.menu.categories", "/joke-categories",
                "fa-solid fa-tags", category.getId(), module.getId(), 20, administrators);
        findOrCreateMenuItem("gnoeckly-stickers", "gnoeckly.menu.stickers", "/stickers-admin",
                "fa-solid fa-note-sticky", category.getId(), module.getId(), 30, administrators);
    }

    private void findOrCreateMenuItem(String key, String labelKey, String route, String icon, UUID categoryId,
                                      UUID moduleId, int sortOrder, Group visibleToGroup) {
        if (menuItemRepository.findByKey(key).isPresent()) {
            return;
        }
        MenuItem item = new MenuItem();
        item.setKey(key);
        item.setLabelKey(labelKey);
        item.setRoute(route);
        item.setIcon(icon);
        item.setCategoryId(categoryId);
        item.setModuleId(moduleId);
        item.setSortOrder(sortOrder);
        item.setEnabled(true);
        item.getVisibleToGroups().add(visibleToGroup);
        menuItemRepository.save(item);
    }

    private void seedCategories() {
        createCategoryIfMissing("flachwitze", "Flachwitze", "sentiment_very_satisfied", 10);
        createCategoryIfMissing("wortspiele", "Wortspiele", "abc", 20);
        createCategoryIfMissing("buero", "Büro & Arbeit", "work", 30);
        createCategoryIfMissing("tiere", "Tiere", "pets", 40);
        createCategoryIfMissing("schwarzer-humor", "Schwarzer Humor", "dark_mode", 50);
    }

    private void createCategoryIfMissing(String slug, String name, String icon, int sortOrder) {
        if (jokeCategoryRepository.findBySlugAndDeletedAtIsNull(slug).isPresent()) {
            return;
        }
        JokeCategory category = new JokeCategory();
        category.setSlug(slug);
        category.setName(name);
        category.setIcon(icon);
        category.setSortOrder(sortOrder);
        category.setActive(true);
        jokeCategoryRepository.save(category);
    }

    private void seedStickers() {
        createStickerIfMissing("lachtraene", "Lachträne", "Für Witze, bei denen die Tränen kullern.", 30, 10);
        createStickerIfMissing("goldene-pointe", "Goldene Pointe", "Die Auszeichnung für eine perfekte Pointe.", 100, 20);
    }

    private void createStickerIfMissing(String slug, String name, String description, long price, int sortOrder) {
        if (stickerRepository.findBySlugAndDeletedAtIsNull(slug).isPresent()) {
            return;
        }
        Sticker sticker = new Sticker();
        sticker.setSlug(slug);
        sticker.setName(name);
        sticker.setDescription(description);
        sticker.setPrice(price);
        sticker.setActive(true);
        sticker.setSortOrder(sortOrder);
        stickerRepository.save(sticker);
    }

    private void seedAdminProfileAndWallet() {
        if (isBlank(properties.getEmail())) {
            return;
        }
        User admin = userRepository.findByEmail(properties.getEmail()).orElse(null);
        if (admin == null) {
            return;
        }
        if (userProfileRepository.findByUserIdAndDeletedAtIsNull(admin.getId()).isEmpty()) {
            UserProfile profile = new UserProfile();
            profile.setUserId(admin.getId());
            profile.setNickname(ADMIN_NICKNAME);
            userProfileRepository.save(profile);
        }
        if (walletRepository.findByUserId(admin.getId()).isEmpty()) {
            walletService.createForUser(admin.getId(), 0);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
