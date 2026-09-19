package at.stoneforge.gnoeckly.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Spielregeln fuer die App-Anzeige ("Einreichen kostet 5 Gnoecken"). Die SystemOptions selbst
 * sind Superadmin-only (superadmin-paths); hier nur die fuer User relevanten Betraege, lesend.
 */
@RestController
@RequestMapping("/api/v1/public/config")
public class PublicConfigController {

    public record PublicConfigResponse(long submitFee, long boostFee, long boostDurationHours, long coinsPerAd,
                                       long coinsPerApprovedJoke, long welcomeCoins) {
    }

    private final GnoecklySettings settings;

    public PublicConfigController(GnoecklySettings settings) {
        this.settings = settings;
    }

    @GetMapping
    public PublicConfigResponse config() {
        return new PublicConfigResponse(settings.submitFee(), settings.boostFee(), settings.boostDuration().toHours(),
                settings.coinsPerAd(), settings.coinsPerApprovedJoke(), settings.welcomeCoins());
    }
}
