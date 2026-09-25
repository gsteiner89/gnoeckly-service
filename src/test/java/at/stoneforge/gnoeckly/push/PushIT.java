package at.stoneforge.gnoeckly.push;

import at.stoneforge.gnoeckly.GnoecklyIntegrationTestBase;
import at.stoneforge.gnoeckly.GnoecklyTestData;
import at.stoneforge.gnoeckly.MutableClock;
import at.stoneforge.gnoeckly.RecordingPushSender;
import at.stoneforge.gnoeckly.TestSupportConfiguration;
import at.stoneforge.gnoeckly.streak.StreakReminderJob;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Push: Token-Verwaltung, Ereignis-Nachrichten (Freigabe, Ablehnung, Sticker, Score-Schwelle),
 * Streak-Erinnerung und Robustheit (Versandfehler, ungueltige Tokens). Versendet wird inline
 * ({@code gnoeckly.push.async=false}) an den {@link RecordingPushSender}.
 */
@Import(TestSupportConfiguration.class)
class PushIT extends GnoecklyIntegrationTestBase {

    @Autowired
    private RecordingPushSender sender;

    @Autowired
    private MutableClock clock;

    @Autowired
    private PushService pushService;

    @Autowired
    private StreakReminderJob reminderJob;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GnoecklyTestData testData;

    @BeforeEach
    @AfterEach
    void reset() {
        sender.reset();
        clock.setToday();
    }

    @Test
    void tokenBelongsToOneUserAndOnlyOwnerCanUnregister() throws Exception {
        GnoecklyTestData.TestUser first = testData.registerUser();
        GnoecklyTestData.TestUser second = testData.registerUser();
        String token = token();

        register(first, token);
        notifyUser(first, "eins");
        assertThat(sender.messagesTo(token)).extracting(PushMessage::title).containsExactly("eins");

        // Anderer User meldet dasselbe Geraet an: der Token wird umgehaengt.
        register(second, token);
        notifyUser(first, "an den alten Besitzer");
        assertThat(sender.messagesTo(token)).hasSize(1);
        notifyUser(second, "zwei");
        assertThat(sender.messagesTo(token)).hasSize(2);

        // Fremder Abmeldeversuch ist wirkungslos, der Besitzer kann abmelden (idempotent).
        testData.perform(delete("/api/v1/me/push-tokens").param("token", token).header("Authorization", first.bearer()), 204);
        notifyUser(second, "drei");
        assertThat(sender.messagesTo(token)).hasSize(3);
        testData.perform(delete("/api/v1/me/push-tokens").param("token", token).header("Authorization", second.bearer()), 204);
        testData.perform(delete("/api/v1/me/push-tokens").param("token", token).header("Authorization", second.bearer()), 204);
        notifyUser(second, "vier");
        assertThat(sender.messagesTo(token)).hasSize(3);
    }

    @Test
    void registrationNeedsLoginAndValidBody() throws Exception {
        testData.perform(post("/api/v1/me/push-tokens").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("token", "x", "platform", "ANDROID"))), 401);
        GnoecklyTestData.TestUser user = testData.registerUser();
        testData.perform(post("/api/v1/me/push-tokens").header("Authorization", user.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("token", "", "platform", "ANDROID"))), 400);
    }

    @Test
    void approvalAndRejectionNotifyAuthor() throws Exception {
        GnoecklyTestData.TestUser author = testData.registerUser();
        String token = token();
        register(author, token);
        GnoecklyTestData.TestUser admin = testData.admin();

        UUID approved = testData.submitJoke(author, "Approve " + UUID.randomUUID());
        testData.perform(post("/api/admin/jokes/" + approved + "/approve").header("Authorization", admin.bearer()), 200);
        UUID rejected = testData.submitJoke(author, "Reject " + UUID.randomUUID());
        testData.perform(post("/api/admin/jokes/" + rejected + "/reject").header("Authorization", admin.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("reason", "Zu flach"))), 200);

        List<PushMessage> messages = sender.messagesTo(token);
        assertThat(messages).extracting(PushMessage::title)
                .containsExactly("Dein Witz ist freigegeben", "Dein Witz wurde abgelehnt");
        assertThat(messages.get(1).body()).isEqualTo("Zu flach");
        assertThat(messages).extracting(PushMessage::route).containsOnly("/me");
    }

    @Test
    void stickerAwardNotifiesJokeAuthor() throws Exception {
        GnoecklyTestData.TestUser author = testData.registerUser();
        GnoecklyTestData.TestUser fan = testData.registerUser();
        String token = token();
        register(author, token);
        UUID jokeId = testData.approvedJoke(author, "Sticker push " + UUID.randomUUID());
        sender.reset();
        UUID stickerId = stickerId("lachtraene");
        testData.perform(post("/api/v1/stickers/" + stickerId + "/purchase").header("Authorization", fan.bearer()), 200);
        testData.perform(post("/api/v1/jokes/" + jokeId + "/stickers").header("Authorization", fan.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("stickerId", stickerId))), 201);

        List<PushMessage> messages = sender.messagesTo(token);
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).title()).isEqualTo("Neuer Sticker für deinen Witz");
        assertThat(messages.get(0).body()).contains(fan.nickname()).contains("Lachträne");
        assertThat(messages.get(0).route()).isEqualTo("/joke/" + jokeId);
    }

    @Test
    void reachingScoreThresholdNotifiesAuthorExactlyOnce() throws Exception {
        GnoecklyTestData.TestUser author = testData.registerUser();
        String token = token();
        register(author, token);
        UUID jokeId = testData.approvedJoke(author, "Threshold " + UUID.randomUUID());
        sender.reset();

        for (int i = 0; i < 9; i++) {
            vote(testData.registerUser(), jokeId);
        }
        assertThat(sender.messagesTo(token)).isEmpty();
        vote(testData.registerUser(), jokeId);
        assertThat(sender.messagesTo(token)).extracting(PushMessage::title).containsExactly("Dein Witz kommt an");
        vote(testData.registerUser(), jokeId);
        assertThat(sender.messagesTo(token)).hasSize(1);
    }

    @Test
    void reminderJobOnlyHitsAtRiskStreaks() throws Exception {
        GnoecklyTestData.TestUser author = testData.registerUser();
        UUID jokeId = testData.approvedJoke(author, "Reminder " + UUID.randomUUID());
        GnoecklyTestData.TestUser atRisk = testData.registerUser();
        GnoecklyTestData.TestUser shortStreak = testData.registerUser();
        GnoecklyTestData.TestUser doneToday = testData.registerUser();
        String riskToken = token();
        String shortToken = token();
        String doneToken = token();
        register(atRisk, riskToken);
        register(shortStreak, shortToken);
        register(doneToday, doneToken);

        LocalDate day1 = LocalDate.of(2031, 6, 1);
        clock.setDate(day1);
        vote(atRisk, jokeId);
        vote(doneToday, jokeId);
        clock.setDate(day1.plusDays(1));
        vote(atRisk, jokeId);
        vote(shortStreak, jokeId);
        vote(doneToday, jokeId);
        clock.setDate(day1.plusDays(2));
        vote(doneToday, jokeId);
        sender.reset();

        reminderJob.run();

        assertThat(sender.messagesTo(riskToken)).extracting(PushMessage::title).containsExactly("Deine Serie ist in Gefahr");
        assertThat(sender.messagesTo(riskToken).get(0).route()).isEqualTo("/challenges");
        assertThat(sender.messagesTo(shortToken)).isEmpty();
        assertThat(sender.messagesTo(doneToken)).isEmpty();
    }

    @Test
    void sendFailureDoesNotBreakBusinessAction() throws Exception {
        GnoecklyTestData.TestUser author = testData.registerUser();
        register(author, token());
        sender.setFailing(true);

        UUID jokeId = testData.submitJoke(author, "Failing push " + UUID.randomUUID());
        testData.perform(post("/api/admin/jokes/" + jokeId + "/approve")
                .header("Authorization", testData.admin().bearer()), 200);
        JsonNode mine = testData.perform(get("/api/v1/me/jokes").header("Authorization", author.bearer()), 200);
        assertThat(mine.get("content").get(0).get("status").asText()).isEqualTo("APPROVED");
    }

    @Test
    void invalidTokensAreRemovedAfterSend() throws Exception {
        GnoecklyTestData.TestUser user = testData.registerUser();
        String token = token();
        register(user, token);
        sender.setInvalidTokens(List.of(token));

        notifyUser(user, "erste");
        assertThat(sender.messagesTo(token)).hasSize(1);
        notifyUser(user, "zweite");
        assertThat(sender.messagesTo(token)).hasSize(1);
    }

    // --- Helfer ---------------------------------------------------------------------------------

    private static String token() {
        return "test-token-" + UUID.randomUUID();
    }

    private void register(GnoecklyTestData.TestUser user, String token) throws Exception {
        testData.perform(post("/api/v1/me/push-tokens").header("Authorization", user.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("token", token, "platform", "ANDROID"))), 204);
    }

    private void notifyUser(GnoecklyTestData.TestUser user, String title) {
        testData.inTenant(() -> {
            pushService.notify(user.id(), new PushMessage(title, "Text", "/feed"));
            return null;
        });
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
