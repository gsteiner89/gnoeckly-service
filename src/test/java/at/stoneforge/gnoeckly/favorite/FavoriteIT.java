package at.stoneforge.gnoeckly.favorite;

import at.stoneforge.gnoeckly.GnoecklyIntegrationTestBase;
import at.stoneforge.gnoeckly.GnoecklyTestData;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Favorisieren/Entfernen idempotent, myFavorite-Anreicherung, Favoritenliste, Mehrfach-Kategorie-Feed. */
class FavoriteIT extends GnoecklyIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GnoecklyTestData testData;

    @Test
    void favoriteLifecycle() throws Exception {
        GnoecklyTestData.TestUser author = testData.registerUser();
        GnoecklyTestData.TestUser fan = testData.registerUser();
        UUID jokeId = testData.approvedJoke(author, "Merk mich " + UUID.randomUUID());

        assertThat(joke(jokeId, fan).get("myFavorite").asBoolean()).isFalse();

        favorite(jokeId, fan);
        favorite(jokeId, fan); // idempotent
        assertThat(joke(jokeId, fan).get("myFavorite").asBoolean()).isTrue();
        assertThat(joke(jokeId, author).get("myFavorite").asBoolean()).isFalse();
        assertThat(testData.perform(get("/api/v1/public/jokes/" + jokeId), 200).get("myFavorite").asBoolean()).isFalse();

        JsonNode list = testData.perform(get("/api/v1/me/favorites").header("Authorization", fan.bearer()), 200);
        assertThat(list.get("totalElements").asInt()).isEqualTo(1);
        assertThat(list.get("content").get(0).get("id").asText()).isEqualTo(jokeId.toString());
        assertThat(list.get("content").get(0).get("myFavorite").asBoolean()).isTrue();

        mockMvc.perform(delete("/api/v1/jokes/" + jokeId + "/favorite").header("Authorization", fan.bearer()))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/jokes/" + jokeId + "/favorite").header("Authorization", fan.bearer()))
                .andExpect(status().isNoContent()); // idempotent
        assertThat(joke(jokeId, fan).get("myFavorite").asBoolean()).isFalse();
        assertThat(testData.perform(get("/api/v1/me/favorites").header("Authorization", fan.bearer()), 200)
                .get("totalElements").asInt()).isZero();
    }

    @Test
    void favoritingNeedsLoginAndApprovedJoke() throws Exception {
        GnoecklyTestData.TestUser author = testData.registerUser();
        UUID pending = testData.submitJoke(author, "Noch offen " + UUID.randomUUID());
        mockMvc.perform(post("/api/v1/jokes/" + pending + "/favorite").header("Authorization", author.bearer()))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/jokes/" + pending + "/favorite")).andExpect(status().isUnauthorized());
    }

    @Test
    void feedFiltersByMultipleCategories() throws Exception {
        GnoecklyTestData.TestUser author = testData.registerUser();
        UUID jokeId = testData.approvedJoke(author, "Kategorie " + UUID.randomUUID());
        UUID category = testData.anyCategoryId();
        UUID other = UUID.randomUUID();

        JsonNode hit = testData.perform(get("/api/v1/public/jokes?categoryIds=" + other + "," + category + "&size=100"), 200);
        assertThat(ids(hit)).contains(jokeId.toString());
        JsonNode miss = testData.perform(get("/api/v1/public/jokes?categoryIds=" + other), 200);
        assertThat(miss.get("totalElements").asInt()).isZero();
    }

    @Test
    void feedFavoritesOnly() throws Exception {
        GnoecklyTestData.TestUser author = testData.registerUser();
        GnoecklyTestData.TestUser fan = testData.registerUser();
        UUID kept = testData.approvedJoke(author, "Favorit " + UUID.randomUUID());
        UUID other = testData.approvedJoke(author, "Kein Favorit " + UUID.randomUUID());
        favorite(kept, fan);

        JsonNode mine = testData.perform(get("/api/v1/public/jokes?favoritesOnly=true&size=100").header("Authorization", fan.bearer()), 200);
        assertThat(ids(mine)).containsExactly(kept.toString());
        assertThat(ids(testData.perform(get("/api/v1/public/jokes?favoritesOnly=true").header("Authorization", author.bearer()), 200))).isEmpty();
        assertThat(ids(testData.perform(get("/api/v1/public/jokes?favoritesOnly=true"), 200))).isEmpty();
        assertThat(ids(testData.perform(get("/api/v1/public/jokes?size=100").header("Authorization", fan.bearer()), 200))).contains(kept.toString(), other.toString());
    }

    private void favorite(UUID jokeId, GnoecklyTestData.TestUser user) throws Exception {
        mockMvc.perform(post("/api/v1/jokes/" + jokeId + "/favorite").header("Authorization", user.bearer()))
                .andExpect(status().isNoContent());
    }

    private JsonNode joke(UUID jokeId, GnoecklyTestData.TestUser viewer) throws Exception {
        return testData.perform(get("/api/v1/public/jokes/" + jokeId).header("Authorization", viewer.bearer()), 200);
    }

    private static java.util.List<String> ids(JsonNode page) {
        java.util.List<String> ids = new java.util.ArrayList<>();
        page.get("content").forEach(n -> ids.add(n.get("id").asText()));
        return ids;
    }
}
