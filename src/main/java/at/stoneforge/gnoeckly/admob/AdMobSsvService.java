package at.stoneforge.gnoeckly.admob;

import at.stoneforge.gnoeckly.config.GnoecklySettings;
import at.stoneforge.gnoeckly.wallet.CoinTransactionRepository;
import at.stoneforge.gnoeckly.wallet.CoinTransactionType;
import at.stoneforge.gnoeckly.wallet.WalletRepository;
import at.stoneforge.gnoeckly.wallet.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Gutschrift nach verifiziertem Rewarded Ad. Idempotent ueber {@code transaction_id} (Pruefung +
 * partieller Unique-Index in der DB fuer den Race-Fall). Der Betrag ist die Server-Wahrheit
 * ({@code gnoeckly.coinsPerAd}); Googles {@code reward_amount} wird nur bei Abweichung geloggt.
 */
@Service
public class AdMobSsvService {

    private static final Logger log = LoggerFactory.getLogger(AdMobSsvService.class);

    private final CoinTransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final GnoecklySettings settings;

    public AdMobSsvService(CoinTransactionRepository transactionRepository, WalletRepository walletRepository,
                           WalletService walletService, GnoecklySettings settings) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
        this.walletService = walletService;
        this.settings = settings;
    }

    /** @return true, wenn tatsaechlich gebucht wurde */
    @Transactional
    public boolean credit(UUID userId, String transactionId, String adUnit, long reportedRewardAmount) {
        if (transactionRepository.existsByExternalReference(transactionId)) {
            log.debug("AdMob SSV: transaction_id {} bereits gebucht", transactionId);
            return false;
        }
        if (walletRepository.findByUserId(userId).isEmpty()) {
            log.warn("AdMob SSV: kein Wallet fuer user_id {} (transaction_id {})", userId, transactionId);
            return false;
        }
        long amount = settings.coinsPerAd();
        if (reportedRewardAmount != amount) {
            log.info("AdMob SSV: reward_amount {} weicht von coinsPerAd {} ab - Server-Wert gebucht", reportedRewardAmount, amount);
        }
        walletService.credit(userId, amount, CoinTransactionType.AD_REWARD, null, transactionId,
                "Werbung angesehen" + (adUnit == null ? "" : " (" + adUnit + ")"));
        return true;
    }
}
