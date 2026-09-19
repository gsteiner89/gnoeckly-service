package at.stoneforge.gnoeckly;

import at.stoneforge.midgard.test.EmbeddedPostgresTestSupport;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Echtes PostgreSQL (eingebetteter Prozess aus midgard-cores test-fixtures) plus der komplette
 * Gnoeckly-Context inklusive Midgard. Das {@code test}-Profil setzt die Bootstrap-Properties, damit
 * beim Context-Start alle Upgrades laufen: Default-Tenant, Superadmin, Kategorien und
 * Beispiel-Sticker existieren in jedem Test. MockMvc laeuft mit der echten Filter-Chain
 * (Midgards TenantFilter, DefaultTenantFilter, Security).
 */
@SpringBootTest(classes = GnoecklyApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public abstract class GnoecklyIntegrationTestBase extends EmbeddedPostgresTestSupport {
}
