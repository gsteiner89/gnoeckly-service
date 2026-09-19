package at.stoneforge.gnoeckly.tenant;

import at.stoneforge.midgard.tenant.Tenant;
import at.stoneforge.midgard.tenant.TenantRepository;
import at.stoneforge.midgard.upgrade.MidgardBootstrapAdminProperties;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Gnoeckly ist eine Single-Tenant-App: es gibt genau den Bootstrap-Tenant aus
 * {@code midgard.bootstrap-admin.tenant-slug}. Tokenlose Requests (Public-Feed, Registrierung,
 * AdMob-SSV-Callback, Sticker-Bilder) brauchen trotzdem einen Tenant im
 * {@link at.stoneforge.midgard.tenant.TenantContext} - Midgards {@code TenantFilter} setzt ihn nur
 * aus einem JWT. Dieser Resolver liefert die ID des Default-Tenants, gecacht nach dem ersten
 * erfolgreichen Lookup. Ein leeres Ergebnis wird bewusst NICHT gecacht: beim allerersten Start
 * legt {@code MidgardBootstrapAdminUpgrade} den Tenant erst nach dem Hochfahren an.
 */
@Component
public class DefaultTenantResolver {

    private final TenantRepository tenantRepository;
    private final MidgardBootstrapAdminProperties bootstrapProperties;
    private final AtomicReference<UUID> cached = new AtomicReference<>();

    public DefaultTenantResolver(TenantRepository tenantRepository, MidgardBootstrapAdminProperties bootstrapProperties) {
        this.tenantRepository = tenantRepository;
        this.bootstrapProperties = bootstrapProperties;
    }

    public Optional<UUID> defaultTenantId() {
        UUID id = cached.get();
        if (id != null) {
            return Optional.of(id);
        }
        Optional<UUID> resolved = tenantRepository.findBySlug(bootstrapProperties.getTenantSlug()).map(Tenant::getId);
        resolved.ifPresent(cached::set);
        return resolved;
    }
}
