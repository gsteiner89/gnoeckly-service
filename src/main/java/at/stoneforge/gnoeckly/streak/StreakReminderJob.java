package at.stoneforge.gnoeckly.streak;

import at.stoneforge.gnoeckly.push.PushMessage;
import at.stoneforge.gnoeckly.push.PushService;
import at.stoneforge.gnoeckly.tenant.DefaultTenantResolver;
import at.stoneforge.midgard.tenant.TenantContext;
import at.stoneforge.midgard.tenant.TenantHibernateFilterActivator;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Taegliche Erinnerung an gefaehrdete Serien (Standard 18:00 Vienna). Bewusste Ausnahme zu Regel 8:
 * der Job setzt Default-Tenant und Hibernate-Filter selbst (Muster {@code GnoecklySeedRunner}).
 * Gemeldet wird, wer gestern zuletzt aktiv war und eine Serie ab {@link #MIN_STREAK} hat. Abschaltbar
 * mit {@code gnoeckly.push.reminder-cron: "-"}. Laeuft einmal je Backend-Instanz - bei mehreren
 * Instanzen braeuchte es eine Sperre (z. B. ShedLock), sonst kaemen Erinnerungen doppelt.
 */
@Component
public class StreakReminderJob {

    static final int MIN_STREAK = 2;

    private static final Logger log = LoggerFactory.getLogger(StreakReminderJob.class);

    private final UserStreakRepository streakRepository;
    private final PushService pushService;
    private final DefaultTenantResolver tenantResolver;
    private final TenantHibernateFilterActivator filterActivator;
    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public StreakReminderJob(UserStreakRepository streakRepository, PushService pushService,
                             DefaultTenantResolver tenantResolver, TenantHibernateFilterActivator filterActivator,
                             EntityManager entityManager, PlatformTransactionManager transactionManager, Clock clock) {
        this.streakRepository = streakRepository;
        this.pushService = pushService;
        this.tenantResolver = tenantResolver;
        this.filterActivator = filterActivator;
        this.entityManager = entityManager;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    @Scheduled(cron = "${gnoeckly.push.reminder-cron:0 0 18 * * *}", zone = "${gnoeckly.timezone:Europe/Vienna}")
    public void run() {
        UUID tenantId = tenantResolver.defaultTenantId().orElse(null);
        if (tenantId == null) {
            log.warn("Streak-Erinnerung uebersprungen: kein Default-Tenant.");
            return;
        }
        LocalDate yesterday = LocalDate.now(clock).minusDays(1);
        TenantContext.setCurrentTenantId(tenantId);
        try {
            transactionTemplate.executeWithoutResult(status -> {
                filterActivator.activate(entityManager, tenantId);
                List<UserStreak> atRisk = streakRepository
                        .findByLastActiveDateAndCurrentStreakGreaterThanEqualAndDeletedAtIsNull(yesterday, MIN_STREAK);
                for (UserStreak streak : atRisk) {
                    pushService.notify(streak.getUserId(), new PushMessage("Deine Serie ist in Gefahr",
                            "Vote heute einen Witz, sonst reißt deine " + streak.getCurrentStreak() + "-Tage-Serie.",
                            "/challenges"));
                }
                log.info("Streak-Erinnerung: {} gefaehrdete Serie(n).", atRisk.size());
            });
        } finally {
            TenantContext.clear();
        }
    }
}
