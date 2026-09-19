package at.stoneforge.gnoeckly.profile;

import at.stoneforge.gnoeckly.GnoecklyIntegrationTestBase;
import at.stoneforge.gnoeckly.GnoecklyTestData;
import at.stoneforge.gnoeckly.tenant.DefaultTenantResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RegistrationIT extends GnoecklyIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GnoecklyTestData testData;

    @Autowired
    private UserProfileRepository profileRepository;

    @Autowired
    private DefaultTenantResolver defaultTenantResolver;

    @Test
    void registersUserWithProfileWalletAndTokens() throws Exception {
        GnoecklyTestData.TestUser user = testData.registerUser("neuling_1", "neuling1@example.test");

        JsonNode me = testData.perform(get("/api/v1/me").header("Authorization", user.bearer()), 200);
        assertThat(me.get("nickname").asText()).isEqualTo("neuling_1");
        assertThat(me.get("email").asText()).isEqualTo("neuling1@example.test");
        assertThat(me.get("superAdmin").asBoolean()).isFalse();
        assertThat(me.get("balance").asLong()).isEqualTo(100); // gnoeckly.welcomeCoins Default
        assertThat(me.get("karma").asLong()).isZero();

        JsonNode ledger = testData.perform(get("/api/v1/me/wallet/transactions").header("Authorization", user.bearer()), 200);
        assertThat(ledger.get("totalElements").asLong()).isEqualTo(1);
        assertThat(ledger.get("content").get(0).get("type").asText()).isEqualTo("WELCOME");
    }

    @Test
    void tokenlessRegistrationWritesRowsIntoTheDefaultTenant() throws Exception {
        GnoecklyTestData.TestUser user = testData.registerUser();
        UUID defaultTenant = defaultTenantResolver.defaultTenantId().orElseThrow();

        UserProfile profile = testData.inTenant(() -> profileRepository.findByUserIdAndDeletedAtIsNull(user.id()).orElseThrow());
        assertThat(profile.getTenantId()).isEqualTo(defaultTenant);
    }

    @Test
    void duplicateEmailAndNicknameAreRejectedWithStableCodes() throws Exception {
        testData.registerUser("doppelt", "doppelt@example.test");

        mockMvc.perform(post("/api/v1/public/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "Doppelt@example.test", "password", "Passw0rd!x", "nickname", "anders"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));

        mockMvc.perform(post("/api/v1/public/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "neu@example.test", "password", "Passw0rd!x", "nickname", "DOPPELT"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NICKNAME_TAKEN"));
    }

    @Test
    void invalidNicknameFailsBeanValidation() throws Exception {
        mockMvc.perform(post("/api/v1/public/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "x@example.test", "password", "Passw0rd!x", "nickname", "a b"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("nickname"));
    }

    @Test
    void profileUpdateChangesNicknameAndIsVisiblePublicly() throws Exception {
        GnoecklyTestData.TestUser user = testData.registerUser();

        mockMvc.perform(put("/api/v1/me/profile")
                        .header("Authorization", user.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nickname", "witzbold_" + user.nickname().substring(0, 4), "bio", "Ich lache gern."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("Ich lache gern."));

        JsonNode profile = testData.perform(get("/api/v1/public/users/" + user.id() + "/profile"), 200);
        assertThat(profile.get("nickname").asText()).startsWith("witzbold_");
        assertThat(profile.get("stickers").isArray()).isTrue();
    }
}
