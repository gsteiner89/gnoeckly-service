package at.stoneforge.gnoeckly.sticker;

import at.stoneforge.gnoeckly.GnoecklyIntegrationTestBase;
import at.stoneforge.gnoeckly.GnoecklyTestData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Katalog, Kauf, Sammlung, Verleihen, limitierter Bestand, Bild-Upload und oeffentlicher Bildabruf. */
class StickerMarketplaceIT extends GnoecklyIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GnoecklyTestData testData;

    @Test
    void catalogIsPublicAndSeeded() throws Exception {
        JsonNode catalog = testData.perform(get("/api/v1/public/stickers"), 200);
        assertThat(catalog.findValuesAsText("slug")).contains("lachtraene", "goldene-pointe");
    }

    @Test
    void purchaseDebitsPriceAndGrowsCollection() throws Exception {
        GnoecklyTestData.TestUser buyer = testData.registerUser(); // Startguthaben 100
        UUID lachtraene = stickerId("lachtraene"); // Preis 30

        JsonNode first = testData.perform(post("/api/v1/stickers/" + lachtraene + "/purchase").header("Authorization", buyer.bearer()), 200);
        assertThat(first.get("sticker").get("quantity").asInt()).isEqualTo(1);
        assertThat(first.get("balance").asLong()).isEqualTo(70);

        JsonNode second = testData.perform(post("/api/v1/stickers/" + lachtraene + "/purchase").header("Authorization", buyer.bearer()), 200);
        assertThat(second.get("sticker").get("quantity").asInt()).isEqualTo(2);
        assertThat(second.get("sticker").get("purchasedTotal").asInt()).isEqualTo(2);

        JsonNode collection = testData.perform(get("/api/v1/me/stickers").header("Authorization", buyer.bearer()), 200);
        assertThat(collection.size()).isEqualTo(1);
        assertThat(collection.get(0).get("slug").asText()).isEqualTo("lachtraene");

        JsonNode ledger = testData.perform(get("/api/v1/me/wallet/transactions").header("Authorization", buyer.bearer()), 200);
        assertThat(ledger.get("content").get(0).get("type").asText()).isEqualTo("STICKER_PURCHASE");
        assertThat(ledger.get("content").get(0).get("amount").asLong()).isEqualTo(-30);
    }

    @Test
    void purchaseWithoutCoinsIs409() throws Exception {
        GnoecklyTestData.TestUser poor = testData.registerUser();
        testData.setBalance(poor, 10); // Goldene Pointe kostet 100
        mockMvc.perform(post("/api/v1/stickers/" + stickerId("goldene-pointe") + "/purchase").header("Authorization", poor.bearer()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_COINS"));
    }

    @Test
    void awardingConsumesStickerAndShowsUpOnJoke() throws Exception {
        GnoecklyTestData.TestUser author = testData.registerUser();
        GnoecklyTestData.TestUser fan = testData.registerUser();
        UUID jokeId = testData.approvedJoke(author, "Award me " + UUID.randomUUID());
        UUID lachtraene = stickerId("lachtraene");
        testData.perform(post("/api/v1/stickers/" + lachtraene + "/purchase").header("Authorization", fan.bearer()), 200);

        JsonNode award = testData.perform(post("/api/v1/jokes/" + jokeId + "/stickers")
                .header("Authorization", fan.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("stickerId", lachtraene, "message", "Herrlich!"))), 201);
        assertThat(award.get("giverNickname").asText()).isEqualTo(fan.nickname());
        assertThat(award.get("slug").asText()).isEqualTo("lachtraene");

        JsonNode collection = testData.perform(get("/api/v1/me/stickers").header("Authorization", fan.bearer()), 200);
        assertThat(collection.get(0).get("quantity").asInt()).isZero();
        assertThat(collection.get(0).get("purchasedTotal").asInt()).isEqualTo(1);

        JsonNode joke = testData.perform(get("/api/v1/public/jokes/" + jokeId), 200);
        assertThat(joke.get("stickers").get(0).get("slug").asText()).isEqualTo("lachtraene");
        assertThat(joke.get("stickers").get(0).get("count").asLong()).isEqualTo(1);

        JsonNode awards = testData.perform(get("/api/v1/public/jokes/" + jokeId + "/stickers"), 200);
        assertThat(awards.get("totalElements").asLong()).isEqualTo(1);
        assertThat(awards.get("content").get(0).get("message").asText()).isEqualTo("Herrlich!");

        // Ohne Exemplar (aufgebraucht) -> 409
        mockMvc.perform(post("/api/v1/jokes/" + jokeId + "/stickers").header("Authorization", fan.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("stickerId", lachtraene))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STICKER_NOT_OWNED"));

        // Eigener Witz -> 400
        testData.perform(post("/api/v1/stickers/" + lachtraene + "/purchase").header("Authorization", author.bearer()), 200);
        mockMvc.perform(post("/api/v1/jokes/" + jokeId + "/stickers").header("Authorization", author.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("stickerId", lachtraene))))
                .andExpect(status().isBadRequest());

        // Oeffentliches Profil zeigt die Sammlung des Autors
        JsonNode profile = testData.perform(get("/api/v1/public/users/" + author.id() + "/profile"), 200);
        assertThat(profile.get("stickers").get(0).get("quantity").asInt()).isEqualTo(1);
    }

    @Test
    void limitedStockSellsOut() throws Exception {
        GnoecklyTestData.TestUser admin = testData.admin();
        String slug = "limited-" + UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> create = new HashMap<>();
        create.put("slug", slug);
        create.put("name", "Limitiert");
        create.put("price", 0);
        create.put("stockTotal", 1);
        create.put("sortOrder", 99);
        JsonNode created = testData.perform(post("/api/admin/stickers").header("Authorization", admin.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(create)), 201);
        UUID stickerId = UUID.fromString(created.get("id").asText());

        GnoecklyTestData.TestUser first = testData.registerUser();
        GnoecklyTestData.TestUser second = testData.registerUser();
        testData.perform(post("/api/v1/stickers/" + stickerId + "/purchase").header("Authorization", first.bearer()), 200);
        mockMvc.perform(post("/api/v1/stickers/" + stickerId + "/purchase").header("Authorization", second.bearer()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STICKER_SOLD_OUT"));

        JsonNode catalog = testData.perform(get("/api/v1/public/stickers"), 200);
        for (JsonNode sticker : catalog) {
            if (sticker.get("id").asText().equals(stickerId.toString())) {
                assertThat(sticker.get("soldOut").asBoolean()).isTrue();
            }
        }

        // Normale User duerfen keine Sticker anlegen
        mockMvc.perform(post("/api/admin/stickers").header("Authorization", first.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminUploadsImageAndPublicCanFetchItWithoutToken() throws Exception {
        GnoecklyTestData.TestUser admin = testData.admin();
        UUID stickerId = stickerId("goldene-pointe");
        byte[] png = onePixelPng();

        MockMultipartFile file = new MockMultipartFile("file", "pointe.png", "image/png", png);
        JsonNode updated = testData.perform(multipart("/api/admin/stickers/" + stickerId + "/image").file(file)
                .header("Authorization", admin.bearer()), 200);
        assertThat(updated.get("imageUrl").asText()).isEqualTo("/api/v1/public/stickers/" + stickerId + "/image");

        mockMvc.perform(get("/api/v1/public/stickers/" + stickerId + "/image"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().exists("ETag"))
                .andExpect(result -> assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(png));

        // Normale User: 403; falscher Typ: 400
        GnoecklyTestData.TestUser user = testData.registerUser();
        mockMvc.perform(multipart("/api/admin/stickers/" + stickerId + "/image").file(file).header("Authorization", user.bearer()))
                .andExpect(status().isForbidden());
        MockMultipartFile text = new MockMultipartFile("file", "x.txt", "text/plain", "nope".getBytes());
        mockMvc.perform(multipart("/api/admin/stickers/" + stickerId + "/image").file(text).header("Authorization", admin.bearer()))
                .andExpect(status().isBadRequest());
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

    private static byte[] onePixelPng() throws Exception {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0xFFFF8800);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
