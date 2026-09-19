package at.stoneforge.gnoeckly.tenant;

import at.stoneforge.midgard.tenant.TenantContext;
import at.stoneforge.midgard.tenant.TenantHibernateFilterActivator;
import jakarta.persistence.EntityManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * Setzt fuer tokenlose Requests den Default-Tenant (siehe {@link DefaultTenantResolver}) und
 * aktiviert den Hibernate-{@code tenantFilter} - exakt das, was Midgards {@code TenantFilter} fuer
 * Requests MIT JWT tut. Ohne diesen Filter wuerde ein Public-Request alle Tenants sehen (Reads)
 * bzw. an {@code tenant_id NOT NULL} scheitern (Writes wie Registrierung oder SSV-Gutschrift), und
 * {@code StorageService.retrieve} wuerfe {@code IllegalStateException} beim Sticker-Bild.
 *
 * Laeuft NACH Midgards TenantFilter (Order {@code DEFAULT_FILTER_ORDER - 5} vs. {@code -10}, siehe
 * {@link at.stoneforge.gnoeckly.config.GnoecklyWebConfiguration}) und nur, wenn dort noch kein
 * Tenant gesetzt wurde - ein gueltiges JWT gewinnt also immer. Wie bei Midgards TenantFilter ist
 * die EntityManager-Session zu diesem Zeitpunkt bereits gebunden (Midgards eigener
 * {@code OpenEntityManagerInViewFilter} liegt bei {@code -20}).
 *
 * Als {@code @Component} plus expliziter {@code FilterRegistrationBean}: Spring Boot registriert
 * einen Filter-Bean, der bereits von einer FilterRegistrationBean referenziert wird, nicht ein
 * zweites Mal (gleiches Muster wie Midgards TenantFilter).
 */
@Component
public class DefaultTenantFilter extends OncePerRequestFilter {

    private final DefaultTenantResolver defaultTenantResolver;
    private final TenantHibernateFilterActivator filterActivator;
    private final EntityManager entityManager;

    public DefaultTenantFilter(DefaultTenantResolver defaultTenantResolver,
                               TenantHibernateFilterActivator filterActivator,
                               EntityManager entityManager) {
        this.defaultTenantResolver = defaultTenantResolver;
        this.filterActivator = filterActivator;
        this.entityManager = entityManager;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (TenantContext.getCurrentTenantId().isPresent()) {
            filterChain.doFilter(request, response);
            return;
        }
        Optional<UUID> defaultTenant = defaultTenantResolver.defaultTenantId();
        if (defaultTenant.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }
        UUID tenantId = defaultTenant.get();
        TenantContext.setCurrentTenantId(tenantId);
        filterActivator.activate(entityManager, tenantId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
