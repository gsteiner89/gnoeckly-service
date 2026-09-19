package at.stoneforge.gnoeckly.vote;

import at.stoneforge.gnoeckly.GnoecklyIntegrationTestBase;
import at.stoneforge.gnoeckly.GnoecklyTestData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Up/Down/Wechsel/Entfernen exakt, myVote-Anreicherung, Hot-Sortierung mit Boost, Autoren-Ranking. */
class VoteAndRankingIT extends GnoecklyIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GnoecklyTestData testData;

    @Test
    void voteLifecycleKeepsCountersExact() throws Exception {
        GnoecklyTestData.TestUser author = testData.registerUser();
        GnoecklyTestData.TestUser voter = testData.registerUser();
        UUID jokeId = testData.approvedJoke(author, "Vote me");

        JsonNode up = vote(voter, jokeId, 1, 200);
        assertThat(up.get("upvotes").asInt()).isEqualTo(1);
        assertThat(up.get("downvotes").asInt()).isZero();
        assertThat(up.get("score").asInt()).isEqualTo(1);
        assertThat(up.get("myVote").asInt()).isEqualTo(1);

        // Gleicher Vote nochmal: idempotent
        JsonNode again = vote(voter, jokeId, 1, 200);
        assertThat(again.get("upvotes").asInt()).isEqualTo(1);

        JsonNode down = vote(voter, jokeId, -1, 200);
        assertThat(down.get("upvotes").asInt()).isZero();
        assertThat(down.get("downvotes").asInt()).isEqualTo(1);
        assertThat(down.get("score").asInt()).isEqualTo(-1);

        JsonNode removed = testData.perform(delete("/api/v1/jokes/" + jokeId + "/vote").header("Authorization", voter.bearer()), 200);
        assertThat(removed.get("upvotes").asInt()).isZero();
        assertThat(removed.get("downvotes").asInt()).isZero();
        assertThat(removed.get("score").asInt()).isZero();
        assertThat(removed.get("myVote").isNull()).isTrue();

        // 0 ist kein gueltiger Vote
        vote(voter, jokeId, 0, 400);
    }

    @Test
    void feedEnrichesMyVoteOnlyWithToken() throws Exception {
        GnoecklyTestData.TestUser author = testData.registerUser();
        GnoecklyTestData.TestUser voter = testData.registerUser();
        UUID jokeId = testData.approvedJoke(author, "Enrich " + UUID.randomUUID());
        vote(voter, jokeId, 1, 200);

        JsonNode anonymous = testData.perform(get("/api/v1/public/jokes/" + jokeId), 200);
        assertThat(anonymous.get("myVote").isNull()).isTrue();
        assertThat(anonymous.get("score").asInt()).isEqualTo(1);

        JsonNode withToken = testData.perform(get("/api/v1/public/jokes/" + jokeId).header("Authorization", voter.bearer()), 200);
        assertThat(withToken.get("myVote").asInt()).isEqualTo(1);
    }

    @Test
    void votingOnPendingJokeIs404() throws Exception {
        GnoecklyTestData.TestUser author = testData.registerUser();
        GnoecklyTestData.TestUser voter = testData.registerUser();
        UUID pending = testData.submitJoke(author, "Pending vote");
        vote(voter, pending, 1, 404);
    }

    @Test
    void hotFeedOrdersByScoreAndBoostFirst() throws Exception {
        GnoecklyTestData.TestUser author = testData.registerUser();
        List<GnoecklyTestData.TestUser> voters = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            voters.add(testData.registerUser());
        }
        UUID popular = testData.approvedJoke(author, "Popular " + UUID.randomUUID());
        UUID plain = testData.approvedJoke(author, "Plain " + UUID.randomUUID());
        UUID boosted = testData.approvedJoke(author, "Boosted " + UUID.randomUUID());
        for (GnoecklyTestData.TestUser voter : voters) {
            vote(voter, popular, 1, 200);
        }
        vote(voters.get(0), plain, -1, 200);

        List<String> order = ids(testData.perform(get("/api/v1/public/jokes").param("sort", "HOT").param("size", "100"), 200));
        assertThat(order.indexOf(popular.toString())).isLessThan(order.indexOf(plain.toString()));

        // Boost kostet 50 und zieht den Witz in HOT nach vorne, in TOP nicht.
        testData.credit(author, 50);
        JsonNode boostedResponse = testData.perform(post("/api/v1/jokes/" + boosted + "/boost").header("Authorization", author.bearer()), 200);
        assertThat(boostedResponse.get("boosted").asBoolean()).isTrue();

        List<String> hot = ids(testData.perform(get("/api/v1/public/jokes").param("sort", "HOT").param("size", "100"), 200));
        assertThat(hot.get(0)).isEqualTo(boosted.toString());
        List<String> top = ids(testData.perform(get("/api/v1/public/jokes").param("sort", "TOP").param("size", "100"), 200));
        assertThat(top.indexOf(popular.toString())).isLessThan(top.indexOf(boosted.toString()));

        // Fremde duerfen nicht boosten
        mockMvc.perform(post("/api/v1/jokes/" + boosted + "/boost").header("Authorization", voters.get(0).bearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void authorRankingSumsKarmaOfApprovedJokes() throws Exception {
        GnoecklyTestData.TestUser star = testData.registerUser();
        GnoecklyTestData.TestUser fan1 = testData.registerUser();
        GnoecklyTestData.TestUser fan2 = testData.registerUser();
        UUID one = testData.approvedJoke(star, "Star 1 " + UUID.randomUUID());
        UUID two = testData.approvedJoke(star, "Star 2 " + UUID.randomUUID());
        vote(fan1, one, 1, 200);
        vote(fan2, one, 1, 200);
        vote(fan1, two, 1, 200);

        JsonNode ranking = testData.perform(get("/api/v1/public/rankings/authors").param("period", "DAY").param("size", "100"), 200);
        JsonNode entry = null;
        for (JsonNode candidate : ranking.get("content")) {
            if (candidate.get("userId").asText().equals(star.id().toString())) {
                entry = candidate;
            }
        }
        assertThat(entry).isNotNull();
        assertThat(entry.get("nickname").asText()).isEqualTo(star.nickname());
        assertThat(entry.get("karma").asLong()).isEqualTo(3);
        assertThat(entry.get("approvedJokes").asLong()).isEqualTo(2);

        JsonNode me = testData.perform(get("/api/v1/me").header("Authorization", star.bearer()), 200);
        assertThat(me.get("karma").asLong()).isEqualTo(3);

        mockMvc.perform(get("/api/v1/public/rankings/jokes").param("period", "WEEK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    private JsonNode vote(GnoecklyTestData.TestUser voter, UUID jokeId, int value, int expectedStatus) throws Exception {
        return testData.perform(put("/api/v1/jokes/" + jokeId + "/vote")
                .header("Authorization", voter.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("value", value))), expectedStatus);
    }

    private static List<String> ids(JsonNode page) {
        return page.get("content").findValuesAsText("id");
    }
}
