package at.stoneforge.gnoeckly.dev;

import at.stoneforge.gnoeckly.GnoecklyIntegrationTestBase;
import at.stoneforge.gnoeckly.GnoecklyTestData;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Das dev-Profil legt Testdaten an; ein zweiter Lauf des Seeders ist ein No-op. */
@ActiveProfiles({"test", "dev"})
class DevDataSeederIT extends GnoecklyIntegrationTestBase {

    @Autowired
    private GnoecklyTestData testData;

    @Autowired
    private DevDataSeeder seeder;

    @Test
    void seedsTestDataIdempotently() throws Exception {
        JsonNode feed = testData.perform(get("/api/v1/public/jokes?size=100"), 200);
        int approved = feed.get("totalElements").asInt();
        assertThat(approved).isGreaterThanOrEqualTo(24);
        assertThat(feed.get("content").findValues("stickers")).anyMatch(node -> !node.isEmpty());

        seeder.run(null);
        assertThat(testData.perform(get("/api/v1/public/jokes?size=100"), 200).get("totalElements").asInt()).isEqualTo(approved);
    }
}
