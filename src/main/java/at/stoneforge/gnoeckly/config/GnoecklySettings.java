package at.stoneforge.gnoeckly.config;

import at.stoneforge.midgard.systemoption.SystemOptionResponse;
import at.stoneforge.midgard.systemoption.SystemOptionService;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Typisierter Lesezugriff auf die in {@link GnoecklySystemOptionDefinitions} definierten Optionen.
 * {@link SystemOptionService#list()} ist Midgards einzige Lese-API (Definition + Tenant-Override
 * gemergt) und braucht einen aktiven {@code TenantContext} - im Request immer gegeben (JWT oder
 * DefaultTenantFilter), in Upgrades nur innerhalb des expliziten Tenant-Blocks.
 */
@Component
public class GnoecklySettings {

    private final SystemOptionService systemOptionService;

    public GnoecklySettings(SystemOptionService systemOptionService) {
        this.systemOptionService = systemOptionService;
    }

    public long coinsPerAd() {
        return number(GnoecklySystemOptionDefinitions.COINS_PER_AD);
    }

    public long submitFee() {
        return number(GnoecklySystemOptionDefinitions.SUBMIT_FEE);
    }

    public long boostFee() {
        return number(GnoecklySystemOptionDefinitions.BOOST_FEE);
    }

    public Duration boostDuration() {
        return Duration.ofHours(number(GnoecklySystemOptionDefinitions.BOOST_DURATION_HOURS));
    }

    public long coinsPerApprovedJoke() {
        return number(GnoecklySystemOptionDefinitions.COINS_PER_APPROVED_JOKE);
    }

    public long welcomeCoins() {
        return number(GnoecklySystemOptionDefinitions.WELCOME_COINS);
    }

    private long number(String key) {
        return systemOptionService.list().stream()
                .filter(option -> option.key().equals(key))
                .findFirst()
                .map(SystemOptionResponse::value)
                .map(String::trim)
                .map(Long::parseLong)
                .orElseThrow(() -> new IllegalStateException("SystemOption " + key + " ist nicht definiert"));
    }
}
