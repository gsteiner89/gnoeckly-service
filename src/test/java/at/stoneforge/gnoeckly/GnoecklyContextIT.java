package at.stoneforge.gnoeckly;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Context startet, Bootstrap-Seed liegt vor, Security-Pfadlisten aus application.yml greifen. */
class GnoecklyContextIT extends GnoecklyIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GnoecklyTestData testData;

    @Test
    void publicCategoriesAreReadableWithoutTokenAndSeeded() throws Exception {
        JsonNode categories = testData.perform(get("/api/v1/public/categories"), 200);
        assertThat(categories.size()).isEqualTo(5);
        assertThat(categories.findValuesAsText("slug")).contains("flachwitze", "schwarzer-humor");
    }

    @Test
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void publicConfigExposesFeesWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/public/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submitFee").value(5))
                .andExpect(jsonPath("$.boostFee").value(50))
                .andExpect(jsonPath("$.coinsPerAd").value(10))
                .andExpect(jsonPath("$.boostDurationHours").value(24));
    }

    @Test
    void protectedEndpointsReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/jokes")).andExpect(status().isUnauthorized());
    }

    @Test
    void midgardManagementEndpointsAreSuperadminOnly() throws Exception {
        GnoecklyTestData.TestUser user = testData.registerUser();
        mockMvc.perform(get("/api/v1/users").header("Authorization", user.bearer()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/system-options").header("Authorization", user.bearer()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/jokes").header("Authorization", user.bearer()))
                .andExpect(status().isForbidden());

        GnoecklyTestData.TestUser admin = testData.admin();
        mockMvc.perform(get("/api/v1/system-options").header("Authorization", admin.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.key == 'gnoeckly.submitFee')].value").value("5"));
    }

    @Test
    void bootstrapAdminHasProfileAndWallet() throws Exception {
        GnoecklyTestData.TestUser admin = testData.admin();
        JsonNode me = testData.perform(get("/api/v1/me").header("Authorization", admin.bearer()), 200);
        assertThat(me.get("nickname").asText()).isEqualTo("admin");
        assertThat(me.get("superAdmin").asBoolean()).isTrue();
        assertThat(me.get("balance").asLong()).isGreaterThanOrEqualTo(0);
    }
}
