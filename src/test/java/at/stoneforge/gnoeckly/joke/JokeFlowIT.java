package at.stoneforge.gnoeckly.joke;

import at.stoneforge.gnoeckly.GnoecklyIntegrationTestBase;
import at.stoneforge.gnoeckly.GnoecklyTestData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Einreichen (Gebuehr), Moderation (Freigabe/Ablehnung), Sichtbarkeit im Feed, Zurueckziehen, Melden. */
class JokeFlowIT extends GnoecklyIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GnoecklyTestData testData;

    @Test
    void submitDebitsFeeAndStaysHiddenUntilApproved() throws Exception {
        GnoecklyTestData.TestUser author = testData.registerUser();
        assertThat(testData.balance(author)).isEqualTo(100);

        JsonNode created = testData.perform(post("/api/v1/jokes")
                .header("Authorization", author.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "title", "Bäcker", "text", "Was macht ein Bäcker im Urlaub? Brötchen.",
                        "categoryId", testData.anyCategoryId()))), 201);
        UUID jokeId = UUID.fromString(created.get("id").asText());
        assertThat(created.get("status").asText()).isEqualTo("PENDING");
        assertThat(created.get("authorNickname").asText()).isEqualTo(author.nickname());
        assertThat(testData.balance(author)).isEqualTo(95);

        mockMvc.perform(get("/api/v1/public/jokes/" + jokeId)).andExpect(status().isNotFound());
        JsonNode feed = testData.perform(get("/api/v1/public/jokes").param("sort", "NEW"), 200);
        assertThat(feed.get("content").findValuesAsText("id")).doesNotContain(jokeId.toString());

        JsonNode mine = testData.perform(get("/api/v1/me/jokes").header("Authorization", author.bearer()), 200);
        assertThat(mine.get("content").get(0).get("id").asText()).isEqualTo(jokeId.toString());
    }

    @Test
    void approveMakesJokePublicAndRewardsAuthor() throws Exception {
        GnoecklyTestData.TestUser author = testData.registerUser();
        UUID jokeId = testData.submitJoke(author, "Approved joke " + jokeId());
        long before = testData.balance(author);
        GnoecklyTestData.TestUser admin = testData.admin();

        JsonNode approved = testData.perform(post("/api/admin/jokes/" + jokeId + "/approve")
                .header("Authorization", admin.bearer()), 200);
        assertThat(approved.get("status").asText()).isEqualTo("APPROVED");
        assertThat(approved.get("approvedAt").isNull()).isFalse();

        assertThat(testData.balance(author)).isEqualTo(before + 20); // gnoeckly.coinsPerApprovedJoke
        testData.perform(get("/api/v1/public/jokes/" + jokeId), 200);

        // Zweite Freigabe ist kein gueltiger Uebergang.
        mockMvc.perform(post("/api/admin/jokes/" + jokeId + "/approve").header("Authorization", admin.bearer()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectStoresReasonVisibleOnlyToAuthor() throws Exception {
        GnoecklyTestData.TestUser author = testData.registerUser();
        UUID jokeId = testData.submitJoke(author, "Rejected joke " + jokeId());

        mockMvc.perform(post("/api/admin/jokes/" + jokeId + "/reject")
                        .header("Authorization", testData.admin().bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "Zu flach"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Zu flach"));

        JsonNode mine = testData.perform(get("/api/v1/me/jokes").header("Authorization", author.bearer()), 200);
        assertThat(mine.get("content").get(0).get("rejectionReason").asText()).isEqualTo("Zu flach");
        mockMvc.perform(get("/api/v1/public/jokes/" + jokeId)).andExpect(status().isNotFound());
    }

    @Test
    void submitWithoutEnoughCoinsIs409AndRollsBack() throws Exception {
        GnoecklyTestData.TestUser author = testData.registerUser();
        testData.setBalance(author, 10); // = 2 Einreichungen
        testData.perform(post("/api/v1/jokes").header("Authorization", author.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("text", "eins", "categoryId", testData.anyCategoryId()))), 201);
        testData.perform(post("/api/v1/jokes").header("Authorization", author.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("text", "zwei", "categoryId", testData.anyCategoryId()))), 201);

        mockMvc.perform(post("/api/v1/jokes").header("Authorization", author.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("text", "drei", "categoryId", testData.anyCategoryId()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_COINS"))
                .andExpect(jsonPath("$.required").value(5))
                .andExpect(jsonPath("$.balance").value(0));

        JsonNode mine = testData.perform(get("/api/v1/me/jokes").header("Authorization", author.bearer()), 200);
        assertThat(mine.get("totalElements").asLong()).isEqualTo(2);
    }

    @Test
    void superadminPaysNoFees() throws Exception {
        GnoecklyTestData.TestUser admin = testData.admin();
        long before = testData.balance(admin);

        JsonNode created = testData.perform(post("/api/v1/jokes")
                .header("Authorization", admin.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("text", "Admin-Witz " + jokeId(), "categoryId", testData.anyCategoryId()))), 201);
        assertThat(created.get("status").asText()).isEqualTo("PENDING");
        assertThat(testData.balance(admin)).isEqualTo(before);

        JsonNode ledger = testData.perform(get("/api/v1/me/wallet/transactions").header("Authorization", admin.bearer()), 200);
        assertThat(ledger.get("content").findValuesAsText("type")).doesNotContain("SUBMIT_FEE");
    }

    @Test
    void moderationRequiresSuperadmin() throws Exception {
        GnoecklyTestData.TestUser author = testData.registerUser();
        GnoecklyTestData.TestUser other = testData.registerUser();
        UUID jokeId = testData.submitJoke(author, "Nur Admin " + jokeId());

        mockMvc.perform(post("/api/admin/jokes/" + jokeId + "/approve").header("Authorization", other.bearer()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/jokes").header("Authorization", testData.admin().bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id").value(org.hamcrest.Matchers.hasItem(jokeId.toString())));
    }

    @Test
    void authorCanWithdrawOnlyPendingJokes() throws Exception {
        GnoecklyTestData.TestUser author = testData.registerUser();
        GnoecklyTestData.TestUser stranger = testData.registerUser();
        UUID pending = testData.submitJoke(author, "Pending " + jokeId());

        mockMvc.perform(delete("/api/v1/jokes/" + pending).header("Authorization", stranger.bearer()))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/jokes/" + pending).header("Authorization", author.bearer()))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/jokes/" + pending).header("Authorization", author.bearer()))
                .andExpect(status().isNotFound());

        UUID approved = testData.approvedJoke(author, "Approved " + jokeId());
        mockMvc.perform(delete("/api/v1/jokes/" + approved).header("Authorization", author.bearer()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reportsLandInAdminQueueAndCanBeResolved() throws Exception {
        GnoecklyTestData.TestUser author = testData.registerUser();
        GnoecklyTestData.TestUser reporter = testData.registerUser();
        UUID jokeId = testData.approvedJoke(author, "Reported " + jokeId());

        mockMvc.perform(post("/api/v1/jokes/" + jokeId + "/report").header("Authorization", reporter.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "Unpassend"))))
                .andExpect(status().isNoContent());
        // Idempotent
        mockMvc.perform(post("/api/v1/jokes/" + jokeId + "/report").header("Authorization", reporter.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "Nochmal"))))
                .andExpect(status().isNoContent());

        String adminBearer = testData.admin().bearer();
        JsonNode reports = testData.perform(get("/api/admin/reports").header("Authorization", adminBearer), 200);
        JsonNode report = null;
        for (JsonNode candidate : reports.get("content")) {
            if (candidate.get("jokeId").asText().equals(jokeId.toString())) {
                report = candidate;
            }
        }
        assertThat(report).isNotNull();
        assertThat(report.get("reporterNickname").asText()).isEqualTo(reporter.nickname());
        assertThat(report.get("reason").asText()).isEqualTo("Unpassend");

        mockMvc.perform(post("/api/admin/reports/" + report.get("id").asText() + "/resolve").header("Authorization", adminBearer))
                .andExpect(status().isNoContent());
        JsonNode after = testData.perform(get("/api/admin/reports").header("Authorization", adminBearer), 200);
        assertThat(after.get("content").findValuesAsText("jokeId")).doesNotContain(jokeId.toString());
    }

    private static String jokeId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
