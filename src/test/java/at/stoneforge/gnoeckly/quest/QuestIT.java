package at.stoneforge.gnoeckly.quest;

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
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Daily Quests: Fortschritt aus Tagesdaten, Einloesen genau einmal pro Tag, Eigenvotes zaehlen nicht,
 * Tageswechsel. Midgards Auditing nutzt nicht unsere Clock ({@code created_at} = echte Zeit); die Uhr
 * steht deshalb auf dem echten heutigen Datum und wechselt nur fuer den Folgetag-Test.
 */
@Import(TestSupportConfiguration.class)
class QuestIT extends GnoecklyIntegrationTestBase {

    @Autowired
    private MutableClock clock;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GnoecklyTestData testData;

    @BeforeEach
    void today() {
        clock.setToday();
    }

    @Test
    void freshUserSeesThreeOpenQuests() throws Exception {
        GnoecklyTestData.TestUser user = testData.registerUser();
        JsonNode quests = quests(user);
        assertThat(quests.get("quests")).hasSize(3);
        for (JsonNode quest : quests.get("quests")) {
            assertThat(quest.get("progress").asInt()).isZero();
            assertThat(quest.get("claimed").asBoolean()).isFalse();
            assertThat(quest.get("claimable").asBoolean()).isFalse();
        }
        assertThat(quest(quests, "VOTE").get("target").asInt()).isEqualTo(5);
    }

    @Test
    void voteQuestCountsForeignJokesOnlyAndPaysOnce() throws Exception {
        GnoecklyTestData.TestUser author = testData.registerUser();
        GnoecklyTestData.TestUser voter = testData.registerUser();
        for (int i = 0; i < 4; i++) {
            vote(voter, testData.approvedJoke(author, "Quest " + UUID.randomUUID()));
        }
        // Ein eigener Witz zaehlt nicht mit.
        vote(voter, testData.approvedJoke(voter, "Own " + UUID.randomUUID()));
        assertThat(quest(quests(voter), "VOTE").get("progress").asInt()).isEqualTo(4);
        testData.perform(post("/api/v1/me/quests/VOTE/claim").header("Authorization", voter.bearer()), 409);

        vote(voter, testData.approvedJoke(author, "Quest " + UUID.randomUUID()));
        JsonNode done = quest(quests(voter), "VOTE");
        assertThat(done.get("claimable").asBoolean()).isTrue();

        long before = testData.balance(voter);
        JsonNode claimed = testData.perform(post("/api/v1/me/quests/VOTE/claim").header("Authorization", voter.bearer()), 200);
        assertThat(quest(claimed, "VOTE").get("claimed").asBoolean()).isTrue();
        assertThat(testData.balance(voter)).isEqualTo(before + done.get("reward").asLong());

        JsonNode again = testData.perform(post("/api/v1/me/quests/VOTE/claim").header("Authorization", voter.bearer()), 409);
        assertThat(again.get("code").asText()).isEqualTo("QUEST_ALREADY_CLAIMED");
        assertThat(testData.balance(voter)).isEqualTo(before + done.get("reward").asLong());
    }

    @Test
    void submitQuestCompletesAfterSubmission() throws Exception {
        GnoecklyTestData.TestUser user = testData.registerUser();
        JsonNode notYet = testData.perform(post("/api/v1/me/quests/SUBMIT/claim").header("Authorization", user.bearer()), 409);
        assertThat(notYet.get("code").asText()).isEqualTo("QUEST_NOT_COMPLETED");

        testData.submitJoke(user, "Quest submit " + UUID.randomUUID());
        assertThat(quest(quests(user), "SUBMIT").get("claimable").asBoolean()).isTrue();
        testData.perform(post("/api/v1/me/quests/SUBMIT/claim").header("Authorization", user.bearer()), 200);
    }

    @Test
    void awardQuestCompletesAfterGivingSticker() throws Exception {
        GnoecklyTestData.TestUser author = testData.registerUser();
        GnoecklyTestData.TestUser fan = testData.registerUser();
        UUID jokeId = testData.approvedJoke(author, "Award quest " + UUID.randomUUID());
        UUID stickerId = stickerId("lachtraene");
        testData.perform(post("/api/v1/stickers/" + stickerId + "/purchase").header("Authorization", fan.bearer()), 200);
        testData.perform(post("/api/v1/jokes/" + jokeId + "/stickers").header("Authorization", fan.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("stickerId", stickerId))), 201);

        assertThat(quest(quests(fan), "AWARD").get("claimable").asBoolean()).isTrue();
        testData.perform(post("/api/v1/me/quests/AWARD/claim").header("Authorization", fan.bearer()), 200);
    }

    @Test
    void nextDayResetsProgressAndClaimState() throws Exception {
        GnoecklyTestData.TestUser user = testData.registerUser();
        testData.submitJoke(user, "Day change " + UUID.randomUUID());
        testData.perform(post("/api/v1/me/quests/SUBMIT/claim").header("Authorization", user.bearer()), 200);

        clock.setDate(LocalDate.now(ZoneId.of("Europe/Vienna")).plusDays(1));
        JsonNode tomorrow = quest(quests(user), "SUBMIT");
        assertThat(tomorrow.get("claimed").asBoolean()).isFalse();
        // Die Einreichung liegt vor dem Beginn des Folgetags, der Fortschritt startet bei 0.
        assertThat(tomorrow.get("progress").asInt()).isZero();
    }

    @Test
    void unknownQuestIs404() throws Exception {
        GnoecklyTestData.TestUser user = testData.registerUser();
        testData.perform(post("/api/v1/me/quests/NOPE/claim").header("Authorization", user.bearer()), 404);
    }

    // --- Helfer ---------------------------------------------------------------------------------

    private JsonNode quests(GnoecklyTestData.TestUser user) throws Exception {
        return testData.perform(get("/api/v1/me/quests").header("Authorization", user.bearer()), 200);
    }

    private static JsonNode quest(JsonNode quests, String key) {
        for (JsonNode quest : quests.get("quests")) {
            if (quest.get("key").asText().equals(key)) {
                return quest;
            }
        }
        throw new AssertionError("Quest " + key + " fehlt");
    }

    private void vote(GnoecklyTestData.TestUser voter, UUID jokeId) throws Exception {
        testData.perform(put("/api/v1/jokes/" + jokeId + "/vote").header("Authorization", voter.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("value", 1))), 200);
    }

    private UUID stickerId(String slug) throws Exception {
        JsonNode catalog = testData.perform(get("/api/v1/public/stickers"), 200);
        for (JsonNode sticker : catalog) {
            if (sticker.get("slug").asText().equals(slug)) {
                return UUID.fromString(sticker.get("id").asText());
            }
        }
        throw new AssertionError("Sticker " + slug + " nicht im Katalog");
    }
}
