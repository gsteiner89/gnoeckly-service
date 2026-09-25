package at.stoneforge.gnoeckly.profile;

import at.stoneforge.gnoeckly.GnoecklyIntegrationTestBase;
import at.stoneforge.gnoeckly.GnoecklyTestData;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Oeffentliche Witzliste im Profil: nur freigegebene Witze genau dieses Users. */
class ProfileJokesIT extends GnoecklyIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GnoecklyTestData testData;

    @Test
    void listsOnlyApprovedJokesOfThatUser() throws Exception {
        GnoecklyTestData.TestUser author = testData.registerUser();
        GnoecklyTestData.TestUser other = testData.registerUser();
        UUID approved = testData.approvedJoke(author, "Profil-Witz " + UUID.randomUUID());
        testData.submitJoke(author, "Noch offen " + UUID.randomUUID());
        testData.approvedJoke(other, "Fremder Witz " + UUID.randomUUID());

        JsonNode page = testData.perform(get("/api/v1/public/users/" + author.id() + "/jokes"), 200);
        assertThat(page.get("totalElements").asInt()).isEqualTo(1);
        assertThat(page.get("content").get(0).get("id").asText()).isEqualTo(approved.toString());
    }

    @Test
    void bestJokeFirst() throws Exception {
        GnoecklyTestData.TestUser author = testData.registerUser();
        GnoecklyTestData.TestUser voter = testData.registerUser();
        UUID older = testData.approvedJoke(author, "Beliebt " + UUID.randomUUID());
        UUID newer = testData.approvedJoke(author, "Neuer " + UUID.randomUUID());
        mockMvc.perform(put("/api/v1/jokes/" + older + "/vote").header("Authorization", voter.bearer())
                .contentType("application/json").content("{\"value\":1}")).andExpect(status().isOk());

        JsonNode page = testData.perform(get("/api/v1/public/users/" + author.id() + "/jokes"), 200);
        assertThat(page.get("content").get(0).get("id").asText()).isEqualTo(older.toString());
        assertThat(page.get("content").get(1).get("id").asText()).isEqualTo(newer.toString());
    }
}
