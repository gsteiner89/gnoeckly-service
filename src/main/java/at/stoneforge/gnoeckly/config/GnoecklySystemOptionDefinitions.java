package at.stoneforge.gnoeckly.config;

import at.stoneforge.midgard.systemoption.OptionType;
import at.stoneforge.midgard.systemoption.SystemOptionDefinition;
import at.stoneforge.midgard.systemoption.SystemOptionDefinitionProvider;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Gnoecklys Spielregeln als Midgard-SystemOptions: ueber {@code /api/v1/system-options} (nur
 * Superadmin, siehe {@code midgard.security.superadmin-paths}) zur Laufzeit aenderbar, ohne
 * Deployment. Alle Werte sind Gnoecken-Betraege bzw. Stunden; gelesen ueber {@link GnoecklySettings}.
 */
@Component
public class GnoecklySystemOptionDefinitions implements SystemOptionDefinitionProvider {

    public static final String COINS_PER_AD = "gnoeckly.coinsPerAd";
    public static final String SUBMIT_FEE = "gnoeckly.submitFee";
    public static final String BOOST_FEE = "gnoeckly.boostFee";
    public static final String BOOST_DURATION_HOURS = "gnoeckly.boostDurationHours";
    public static final String COINS_PER_APPROVED_JOKE = "gnoeckly.coinsPerApprovedJoke";
    /** Startguthaben bei der Registrierung (Nutzer-Vorgabe: 100). Bei Account-Spam ueber SystemOptions senken. */
    public static final String WELCOME_COINS = "gnoeckly.welcomeCoins";

    @Override
    public List<SystemOptionDefinition> define() {
        return List.of(
                new SystemOptionDefinition(COINS_PER_AD, OptionType.NUMBER, "10", true),
                new SystemOptionDefinition(SUBMIT_FEE, OptionType.NUMBER, "5", true),
                new SystemOptionDefinition(BOOST_FEE, OptionType.NUMBER, "50", true),
                new SystemOptionDefinition(BOOST_DURATION_HOURS, OptionType.NUMBER, "24", true),
                new SystemOptionDefinition(COINS_PER_APPROVED_JOKE, OptionType.NUMBER, "20", true),
                new SystemOptionDefinition(WELCOME_COINS, OptionType.NUMBER, "100", true));
    }
}
