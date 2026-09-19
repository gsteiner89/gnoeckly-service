package at.stoneforge.gnoeckly.upgrade;

import at.stoneforge.gnoeckly.GnoecklyIntegrationTestBase;
import at.stoneforge.gnoeckly.GnoecklyTestData;
import at.stoneforge.midgard.tenant.TenantRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Regression fuer "Default-Tenant nicht vorhanden - Bootstrap-Upgrade gelaufen?": ohne
 * {@code midgard.bootstrap-admin.email/password} muss der Seed-Runner Tenant, Kategorien und
 * Sticker trotzdem anlegen, damit Registrierung und Feed funktionieren. Eigener Tenant-Slug, weil
 * alle Test-Contexts dieselbe eingebettete DB teilen und der Standard-Tenant dort bereits existiert.
 */
@TestPropertySource(properties = {
        "midgard.bootstrap-admin.email=",
        "midgard.bootstrap-admin.password=",
        "midgard.bootstrap-admin.tenant-slug=gnoeckly-noadmin",
        "midgard.bootstrap-admin.tenant-name=Gnoeckly ohne Admin"
})
class SeedWithoutAdminIT extends GnoecklyIntegrationTestBase {

    @Autowired
    private GnoecklyTestData testData;

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void tenantAndSeedExistWithoutBootstrapAdmin() throws Exception {
        assertThat(tenantRepository.findBySlug("gnoeckly-noadmin")).isPresent();

        JsonNode categories = testData.perform(get("/api/v1/public/categories"), 200);
        assertThat(categories.size()).isEqualTo(5);
        JsonNode stickers = testData.perform(get("/api/v1/public/stickers"), 200);
        assertThat(stickers.size()).isEqualTo(2);

        GnoecklyTestData.TestUser user = testData.registerUser();
        JsonNode me = testData.perform(get("/api/v1/me").header("Authorization", user.bearer()), 200);
        assertThat(me.get("nickname").asText()).isEqualTo(user.nickname());
    }
}
