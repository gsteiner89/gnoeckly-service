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

    /** Preis eines Streak-Freezes in Gnoecken und wie viele man gleichzeitig besitzen darf. */
    public static final String STREAK_FREEZE_PRICE = "gnoeckly.streak.freezePrice";
    public static final String STREAK_MAX_FREEZES = "gnoeckly.streak.maxFreezes";
    /** Gnoecken-Belohnung je Streak-Meilenstein (Tag 7 / 30 / 100). */
    public static final String STREAK_MILESTONE_7_COINS = "gnoeckly.streak.milestone7Coins";
    public static final String STREAK_MILESTONE_30_COINS = "gnoeckly.streak.milestone30Coins";
    public static final String STREAK_MILESTONE_100_COINS = "gnoeckly.streak.milestone100Coins";

    /** Gnoecken-Belohnung je Daily Quest (Bewerten / Einreichen / Sticker verleihen). */
    public static final String QUEST_VOTE_COINS = "gnoeckly.quest.voteCoins";
    public static final String QUEST_SUBMIT_COINS = "gnoeckly.quest.submitCoins";
    public static final String QUEST_AWARD_COINS = "gnoeckly.quest.awardCoins";

    @Override
    public List<SystemOptionDefinition> define() {
        return List.of(
                new SystemOptionDefinition(COINS_PER_AD, OptionType.NUMBER, "10", true),
                new SystemOptionDefinition(SUBMIT_FEE, OptionType.NUMBER, "5", true),
                new SystemOptionDefinition(BOOST_FEE, OptionType.NUMBER, "50", true),
                new SystemOptionDefinition(BOOST_DURATION_HOURS, OptionType.NUMBER, "24", true),
                new SystemOptionDefinition(COINS_PER_APPROVED_JOKE, OptionType.NUMBER, "20", true),
                new SystemOptionDefinition(WELCOME_COINS, OptionType.NUMBER, "100", true),
                new SystemOptionDefinition(STREAK_FREEZE_PRICE, OptionType.NUMBER, "50", true),
                new SystemOptionDefinition(STREAK_MAX_FREEZES, OptionType.NUMBER, "2", true),
                new SystemOptionDefinition(STREAK_MILESTONE_7_COINS, OptionType.NUMBER, "20", true),
                new SystemOptionDefinition(STREAK_MILESTONE_30_COINS, OptionType.NUMBER, "100", true),
                new SystemOptionDefinition(STREAK_MILESTONE_100_COINS, OptionType.NUMBER, "500", true),
                new SystemOptionDefinition(QUEST_VOTE_COINS, OptionType.NUMBER, "5", true),
                new SystemOptionDefinition(QUEST_SUBMIT_COINS, OptionType.NUMBER, "5", true),
                new SystemOptionDefinition(QUEST_AWARD_COINS, OptionType.NUMBER, "10", true));
    }
}
