package at.stoneforge.gnoeckly.admob;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * {@code gnoeckly.admob.ssv.*}. {@code verifierKeysUrl} ist eine Spring-Resource-Location:
 * produktiv {@code https://www.gstatic.com/admob/reward/verifier-keys.json}, lokal/Test
 * {@code classpath:...} mit selbst erzeugtem EC-Schluessel (siehe docs/admob-ssv-lokal.md).
 */
@ConfigurationProperties(prefix = "gnoeckly.admob.ssv")
public class AdMobProperties {

    private String verifierKeysUrl = "https://www.gstatic.com/admob/reward/verifier-keys.json";
    private Duration keyCacheTtl = Duration.ofHours(12);

    public String getVerifierKeysUrl() {
        return verifierKeysUrl;
    }

    public void setVerifierKeysUrl(String verifierKeysUrl) {
        this.verifierKeysUrl = verifierKeysUrl;
    }

    public Duration getKeyCacheTtl() {
        return keyCacheTtl;
    }

    public void setKeyCacheTtl(Duration keyCacheTtl) {
        this.keyCacheTtl = keyCacheTtl;
    }
}
