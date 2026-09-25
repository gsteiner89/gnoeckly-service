package at.stoneforge.gnoeckly.streak;

import at.stoneforge.gnoeckly.GnoecklyIntegrationTestBase;
import at.stoneforge.gnoeckly.GnoecklyTestData;
import at.stoneforge.gnoeckly.MutableClock;
import at.stoneforge.gnoeckly.TestSupportConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Tages-Streak: Aufbau, Reset, Freeze, Meilenstein (genau einmal), Aktivitaetsdefinition. Die Zeit
 * steuert eine verstellbare {@link MutableClock}; explizites {@code @Import}, weil verschachtelte
 * {@code @TestConfiguration}s bei {@code @SpringBootTest(classes = ...)} nicht automatisch gefunden werden.
 */
@Import(TestSupportConfiguration.class)
class StreakIT extends GnoecklyIntegrationTestBase {

    private static final LocalDate DAY_ONE = LocalDate.of(2030, 1, 1);

    @Autowired
    private MutableClock clock;

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GnoecklyTestData testData;

    @BeforeEach
    void resetClock() {
        clock.setDate(DAY_ONE);
    }

    @Test
    void consecutiveDaysBuildStreakAndPayMilestoneExactlyOnce() throws Exception {
        GnoecklyTestData.TestUser author = testData.registerUser();
        GnoecklyTestData.TestUser voter = testData.registerUser();
        UUID jokeId = testData.approvedJoke(author, "Streak " + UUID.randomUUID());
        long startBalance = testData.balance(voter);

        for (int day = 0; day < 6; day++) {
            clock.setDate(DAY_ONE.plusDays(day));
            vote(voter, jokeId);
        }
        JsonNode beforeMilestone = streak(voter);
        assertThat(beforeMilestone.get("current").asInt()).isEqualTo(6);
        assertThat(beforeMilestone.get("state").asText()).isEqualTo("ACTIVE_TODAY");
        assertThat(beforeMilestone.get("nextMilestone").asInt()).isEqualTo(7);
        assertThat(testData.balance(voter)).isEqualTo(startBalance);

        clock.setDate(DAY_ONE.plusDays(6));
        vote(voter, jokeId);
        assertThat(streak(voter).get("current").asInt()).isEqualTo(7);
        assertThat(testData.balance(voter)).isEqualTo(startBalance + 20);
        assertThat(ownsSticker(voter, "streak-7")).isTrue();

        // Weitere Aktivitaet am selben Tag und am Folgetag zahlt den Meilenstein nicht nochmal aus.
        vote(voter, jokeId);
        clock.setDate(DAY_ONE.plusDays(7));
        vote(voter, jokeId);
        assertThat(streak(voter).get("current").asInt()).isEqualTo(8);
        assertThat(testData.balance(voter)).isEqualTo(startBalance + 20);
    }

    @Test
    void gapWithoutFreezeBreaksStreak() throws Exception {
        GnoecklyTestData.TestUser author = testData.registerUser();
        GnoecklyTestData.TestUser voter = testData.registerUser();
        UUID jokeId = testData.approvedJoke(author, "Gap " + UUID.randomUUID());

        vote(voter, jokeId);
        clock.setDate(DAY_ONE.plusDays(1));
        vote(voter, jokeId);
        assertThat(streak(voter).get("current").asInt()).isEqualTo(2);

        clock.setDate(DAY_ONE.plusDays(4));
        JsonNode broken = streak(voter);
        assertThat(broken.get("state").asText()).isEqualTo("BROKEN");
        assertThat(broken.get("current").asInt()).isZero();
        assertThat(broken.get("longest").asInt()).isEqualTo(2);

        vote(voter, jokeId);
        JsonNode restarted = streak(voter);
        assertThat(restarted.get("current").asInt()).isEqualTo(1);
        assertThat(restarted.get("longest").asInt()).isEqualTo(2);
    }

    @Test
    void freezeBridgesMissedDay() throws Exception {
        GnoecklyTestData.TestUser author = testData.registerUser();
        GnoecklyTestData.TestUser voter = testData.registerUser();
        UUID jokeId = testData.approvedJoke(author, "Freeze " + UUID.randomUUID());

        vote(voter, jokeId);
        long balance = testData.balance(voter);
        JsonNode bought = testData.perform(post("/api/v1/me/streak/freeze")
                .header("Authorization", voter.bearer()), 200);
        assertThat(bought.get("freezes").asInt()).isEqualTo(1);
        assertThat(testData.balance(voter)).isEqualTo(balance - bought.get("freezePrice").asLong());

        // Ein Tag ausgelassen: der Freeze rettet die Serie.
        clock.setDate(DAY_ONE.plusDays(2));
        assertThat(streak(voter).get("state").asText()).isEqualTo("AT_RISK");
        vote(voter, jokeId);
        JsonNode saved = streak(voter);
        assertThat(saved.get("current").asInt()).isEqualTo(2);
        assertThat(saved.get("freezes").asInt()).isZero();
    }

    @Test
    void freezeIsLimitedAndNeedsCoins() throws Exception {
        GnoecklyTestData.TestUser user = testData.registerUser();
        testData.setBalance(user, 1000);
        JsonNode first = testData.perform(post("/api/v1/me/streak/freeze").header("Authorization", user.bearer()), 200);
        int max = first.get("maxFreezes").asInt();
        for (int i = 1; i < max; i++) {
            testData.perform(post("/api/v1/me/streak/freeze").header("Authorization", user.bearer()), 200);
        }
        JsonNode limit = testData.perform(post("/api/v1/me/streak/freeze").header("Authorization", user.bearer()), 409);
        assertThat(limit.get("code").asText()).isEqualTo("STREAK_FREEZE_LIMIT");

        GnoecklyTestData.TestUser broke = testData.registerUser();
        testData.setBalance(broke, 0);
        JsonNode insufficient = testData.perform(post("/api/v1/me/streak/freeze")
                .header("Authorization", broke.bearer()), 409);
        assertThat(insufficient.get("code").asText()).isEqualTo("INSUFFICIENT_COINS");
    }

    @Test
    void removingVoteOrReadingFeedDoesNotCountAsActivity() throws Exception {
        GnoecklyTestData.TestUser author = testData.registerUser();
        GnoecklyTestData.TestUser voter = testData.registerUser();
        UUID jokeId = testData.approvedJoke(author, "Passive " + UUID.randomUUID());

        vote(voter, jokeId);
        clock.setDate(DAY_ONE.plusDays(1));
        testData.perform(delete("/api/v1/jokes/" + jokeId + "/vote").header("Authorization", voter.bearer()), 200);
        testData.perform(get("/api/v1/public/jokes").header("Authorization", voter.bearer()), 200);

        JsonNode streak = streak(voter);
        assertThat(streak.get("current").asInt()).isEqualTo(1);
        assertThat(streak.get("state").asText()).isEqualTo("AT_RISK");
        assertThat(LocalDate.parse(streak.get("lastActiveDate").asText())).isEqualTo(DAY_ONE);
    }

    @Test
    void meContainsStreakSummary() throws Exception {
        GnoecklyTestData.TestUser user = testData.registerUser();
        JsonNode me = testData.perform(get("/api/v1/me").header("Authorization", user.bearer()), 200);
        assertThat(me.get("streak").get("state").asText()).isEqualTo("NONE");
        assertThat(me.get("streak").get("current").asInt()).isZero();
    }

    @Test
    void rewardStickersAreNotPurchasable() throws Exception {
        JsonNode catalog = testData.perform(get("/api/v1/public/stickers"), 200);
        assertThat(catalog.findValuesAsText("slug")).contains("lachtraene").doesNotContain("streak-7", "streak-30",
                "streak-100");

        GnoecklyTestData.TestUser admin = testData.admin();
        JsonNode all = testData.perform(get("/api/admin/stickers").header("Authorization", admin.bearer()), 200);
        String streakStickerId = null;
        for (JsonNode sticker : all) {
            if ("streak-7".equals(sticker.get("slug").asText())) {
                assertThat(sticker.get("purchasable").asBoolean()).isFalse();
                streakStickerId = sticker.get("id").asText();
            }
        }
        assertThat(streakStickerId).isNotNull();

        GnoecklyTestData.TestUser buyer = testData.registerUser();
        JsonNode denied = testData.perform(post("/api/v1/stickers/" + streakStickerId + "/purchase")
                .header("Authorization", buyer.bearer()), 409);
        assertThat(denied.get("code").asText()).isEqualTo("STICKER_UNAVAILABLE");
    }

    // --- Helfer ---------------------------------------------------------------------------------

    private void vote(GnoecklyTestData.TestUser voter, UUID jokeId) throws Exception {
        testData.perform(put("/api/v1/jokes/" + jokeId + "/vote")
                .header("Authorization", voter.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("value", 1))), 200);
    }

    private JsonNode streak(GnoecklyTestData.TestUser user) throws Exception {
        return testData.perform(get("/api/v1/me/streak").header("Authorization", user.bearer()), 200);
    }

    private boolean ownsSticker(GnoecklyTestData.TestUser user, String slug) throws Exception {
        JsonNode owned = testData.perform(get("/api/v1/me/stickers").header("Authorization", user.bearer()), 200);
        return owned.findValuesAsText("slug").contains(slug);
    }
}
