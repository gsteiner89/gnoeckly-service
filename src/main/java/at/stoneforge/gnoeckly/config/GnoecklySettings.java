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

    public long streakFreezePrice() {
        return number(GnoecklySystemOptionDefinitions.STREAK_FREEZE_PRICE);
    }

    public int streakMaxFreezes() {
        return (int) number(GnoecklySystemOptionDefinitions.STREAK_MAX_FREEZES);
    }

    /** Belohnung fuer den Meilenstein bei {@code day} Tagen (7, 30 oder 100). */
    public long streakMilestoneCoins(int day) {
        return number(switch (day) {
            case 7 -> GnoecklySystemOptionDefinitions.STREAK_MILESTONE_7_COINS;
            case 30 -> GnoecklySystemOptionDefinitions.STREAK_MILESTONE_30_COINS;
            case 100 -> GnoecklySystemOptionDefinitions.STREAK_MILESTONE_100_COINS;
            default -> throw new IllegalArgumentException("Unbekannter Streak-Meilenstein " + day);
        });
    }

    public long questCoins(String questKey) {
        return number(switch (questKey) {
            case "VOTE" -> GnoecklySystemOptionDefinitions.QUEST_VOTE_COINS;
            case "SUBMIT" -> GnoecklySystemOptionDefinitions.QUEST_SUBMIT_COINS;
            case "AWARD" -> GnoecklySystemOptionDefinitions.QUEST_AWARD_COINS;
            default -> throw new IllegalArgumentException("Unbekannte Quest " + questKey);
        });
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
