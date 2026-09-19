package at.stoneforge.gnoeckly.admob;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Laedt Googles Verifier-Keys ({@code {"keys":[{"keyId":..,"pem":"..","base64":".."}]}}) ueber eine
 * Spring-Resource (http(s), classpath, file) und cached sie fuer {@code keyCacheTtl}. Google rotiert
 * die Schluessel gelegentlich: bei unbekannter {@code key_id} wird einmal (mit Mindestabstand)
 * nachgeladen, bevor der Callback abgelehnt wird.
 */
@Component
public class GstaticVerifierKeyProvider implements AdMobVerifierKeyProvider {

    private static final Logger log = LoggerFactory.getLogger(GstaticVerifierKeyProvider.class);
    private static final Duration MIN_REFRESH_INTERVAL = Duration.ofSeconds(60);

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private final AdMobProperties properties;

    private volatile Map<Long, PublicKey> keys = Map.of();
    private volatile Instant loadedAt = Instant.EPOCH;

    public GstaticVerifierKeyProvider(ResourceLoader resourceLoader, ObjectMapper objectMapper, AdMobProperties properties) {
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public Optional<PublicKey> key(long keyId) {
        Instant now = Instant.now();
        if (loadedAt.plus(properties.getKeyCacheTtl()).isBefore(now)) {
            refresh();
        }
        PublicKey key = keys.get(keyId);
        if (key == null && loadedAt.plus(MIN_REFRESH_INTERVAL).isBefore(now)) {
            refresh();
            key = keys.get(keyId);
        }
        return Optional.ofNullable(key);
    }

    @Override
    public synchronized void refresh() {
        Resource resource = resourceLoader.getResource(properties.getVerifierKeysUrl());
        try (InputStream in = resource.getInputStream()) {
            JsonNode root = objectMapper.readTree(in);
            Map<Long, PublicKey> loaded = new HashMap<>();
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            for (JsonNode node : root.path("keys")) {
                long keyId = node.path("keyId").asLong();
                String base64 = node.path("base64").asText(null);
                if (base64 == null || base64.isBlank()) {
                    continue;
                }
                loaded.put(keyId, keyFactory.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(base64))));
            }
            keys = Map.copyOf(loaded);
            log.info("AdMob-Verifier-Keys geladen: {} Schluessel aus {}", loaded.size(), properties.getVerifierKeysUrl());
        } catch (IOException | java.security.GeneralSecurityException | IllegalArgumentException e) {
            log.warn("AdMob-Verifier-Keys konnten nicht geladen werden ({}): {}", properties.getVerifierKeysUrl(), e.toString());
        } finally {
            loadedAt = Instant.now();
        }
    }
}
