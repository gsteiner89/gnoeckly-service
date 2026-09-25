package at.stoneforge.gnoeckly.push;

import at.stoneforge.midgard.tenant.TenantContext;
import at.stoneforge.midgard.tenant.TenantHibernateFilterActivator;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collection;
import java.util.UUID;

/**
 * Loescht vom Provider als ungueltig gemeldete Tokens. Laeuft ggf. im Push-Thread ohne
 * {@code TenantContext} (Regel 8): setzt Tenant und Hibernate-Filter selbst (Muster
 * {@code GnoecklySeedRunner}) und raeumt nur auf, was er selbst gesetzt hat. Eigene Transaktion
 * ({@code REQUIRES_NEW}), weil der Aufruf im Inline-Modus in {@code afterCommit} der Fachtransaktion liegt.
 */
@Component
public class PushTokenCleaner {

    private final PushTokenRepository repository;
    private final TenantHibernateFilterActivator filterActivator;
    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;

    public PushTokenCleaner(PushTokenRepository repository, TenantHibernateFilterActivator filterActivator,
                            EntityManager entityManager, PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.filterActivator = filterActivator;
        this.entityManager = entityManager;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void remove(UUID tenantId, Collection<String> tokens) {
        boolean ownsContext = TenantContext.getCurrentTenantId().isEmpty();
        if (ownsContext) {
            TenantContext.setCurrentTenantId(tenantId);
        }
        try {
            transactionTemplate.executeWithoutResult(status -> {
                filterActivator.activate(entityManager, tenantId);
                repository.deleteAll(repository.findByTokenIn(tokens));
            });
        } finally {
            if (ownsContext) {
                TenantContext.clear();
            }
        }
    }
}
