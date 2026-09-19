package at.stoneforge.gnoeckly.admob;

import at.stoneforge.gnoeckly.GnoecklyIntegrationTestBase;
import at.stoneforge.gnoeckly.GnoecklyTestData;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Server-Side-Verification mit lokal erzeugtem P-256-Schluesselpaar: der Test signiert den
 * Query-String genau wie Google (alles vor {@code &signature=}), der KeyProvider wird per
 * {@code @Primary} auf den Test-Public-Key umgebogen. Explizites {@code @Import} ist noetig:
 * sobald {@code @SpringBootTest(classes = ...)} eine Nicht-Test-Klasse nennt (GnoecklyApplication
 * in der Basisklasse), sammelt Spring Boot verschachtelte {@code @TestConfiguration}s NICHT mehr
 * automatisch ein.
 */
@Import(AdMobSsvIT.TestKeys.class)
class AdMobSsvIT extends GnoecklyIntegrationTestBase {

    private static final long KEY_ID = 4242L;
    private static final KeyPair KEY_PAIR = generateKeyPair();

    @TestConfiguration
    static class TestKeys {
        @Bean
        @Primary
        AdMobVerifierKeyProvider testKeyProvider() {
            return new AdMobVerifierKeyProvider() {
                @Override
                public Optional<PublicKey> key(long keyId) {
                    return keyId == KEY_ID ? Optional.of(KEY_PAIR.getPublic()) : Optional.empty();
                }

                @Override
                public void refresh() {
                }
            };
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GnoecklyTestData testData;

    @Test
    void validCallbackCreditsCoinsExactlyOnce() throws Exception {
        GnoecklyTestData.TestUser user = testData.registerUser();
        long before = testData.balance(user);
        String transactionId = UUID.randomUUID().toString().replace("-", "");
        String query = signedQuery(user.id(), transactionId, 10, KEY_ID);

        mockMvc.perform(get("/api/v1/public/admob/ssv?" + query)).andExpect(status().isOk());
        assertThat(testData.balance(user)).isEqualTo(before + 10); // gnoeckly.coinsPerAd

        // Google wiederholt Callbacks - dieselbe transaction_id darf nicht doppelt buchen.
        mockMvc.perform(get("/api/v1/public/admob/ssv?" + query)).andExpect(status().isOk());
        assertThat(testData.balance(user)).isEqualTo(before + 10);

        JsonNode ledger = testData.perform(get("/api/v1/me/wallet/transactions").header("Authorization", user.bearer()), 200);
        assertThat(ledger.get("content").get(0).get("type").asText()).isEqualTo("AD_REWARD");
    }

    @Test
    void serverAmountWinsOverReportedRewardAmount() throws Exception {
        GnoecklyTestData.TestUser user = testData.registerUser();
        long before = testData.balance(user);
        String query = signedQuery(user.id(), UUID.randomUUID().toString(), 999, KEY_ID);

        mockMvc.perform(get("/api/v1/public/admob/ssv?" + query)).andExpect(status().isOk());
        assertThat(testData.balance(user)).isEqualTo(before + 10);
    }

    @Test
    void tamperedQueryIsRejectedWithoutCrediting() throws Exception {
        GnoecklyTestData.TestUser user = testData.registerUser();
        long before = testData.balance(user);
        String query = signedQuery(user.id(), UUID.randomUUID().toString(), 10, KEY_ID);
        String tampered = query.replace("reward_amount=10", "reward_amount=11");

        mockMvc.perform(get("/api/v1/public/admob/ssv?" + tampered)).andExpect(status().isBadRequest());
        assertThat(testData.balance(user)).isEqualTo(before);
    }

    @Test
    void unknownKeyIdIsRejected() throws Exception {
        GnoecklyTestData.TestUser user = testData.registerUser();
        String query = signedQuery(user.id(), UUID.randomUUID().toString(), 10, 1L);
        mockMvc.perform(get("/api/v1/public/admob/ssv?" + query)).andExpect(status().isBadRequest());
    }

    @Test
    void unknownUserIsAcknowledgedWithoutBooking() throws Exception {
        String query = signedQuery(UUID.randomUUID(), UUID.randomUUID().toString(), 10, KEY_ID);
        mockMvc.perform(get("/api/v1/public/admob/ssv?" + query)).andExpect(status().isOk());
    }

    private static String signedQuery(UUID userId, String transactionId, long rewardAmount, long keyId) throws Exception {
        String message = "ad_network=5450213213286189855&ad_unit=1234567890&reward_amount=" + rewardAmount
                + "&reward_item=coins&timestamp=" + System.currentTimeMillis()
                + "&transaction_id=" + transactionId + "&user_id=" + userId;
        Signature signer = Signature.getInstance("SHA256withECDSA");
        signer.initSign(KEY_PAIR.getPrivate());
        signer.update(message.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(signer.sign());
        return message + "&signature=" + signature + "&key_id=" + keyId;
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"));
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
