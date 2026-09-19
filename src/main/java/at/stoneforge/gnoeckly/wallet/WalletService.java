package at.stoneforge.gnoeckly.wallet;

import at.stoneforge.midgard.user.User;
import at.stoneforge.midgard.user.UserRepository;
import at.stoneforge.midgard.web.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Einzige Stelle, die {@link Wallet#setBalance} aufruft. Jede Buchung: Zeilensperre
 * ({@code findForUpdateByUserId}), Kontostand aendern, Ledger-Eintrag mit {@code balanceAfter}.
 * Laeuft immer innerhalb der Transaktion des Aufrufers (Submit, Kauf, Approve, SSV), damit
 * Fachobjekt und Buchung atomar sind.
 */
@Service
public class WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);

    private final WalletRepository walletRepository;
    private final CoinTransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public WalletService(WalletRepository walletRepository, CoinTransactionRepository transactionRepository,
                         UserRepository userRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Wallet createForUser(UUID userId, long welcomeCoins) {
        Wallet wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setBalance(0);
        wallet = walletRepository.save(wallet);
        if (welcomeCoins > 0) {
            wallet.setBalance(welcomeCoins);
            record(wallet, CoinTransactionType.WELCOME, welcomeCoins, null, null, "Startguthaben");
        }
        return wallet;
    }

    /**
     * Superadmins zahlen nichts (Nutzer-Vorgabe "der Admin benoetigt keine Gnoecken"): Einreichen,
     * Boost und Sticker-Kauf laufen fuer sie ohne Abbuchung und ohne Ledger-Eintrag.
     *
     * @throws InsufficientCoinsException wenn der Kontostand nicht reicht (HTTP 409 INSUFFICIENT_COINS)
     */
    @Transactional
    public Wallet debit(UUID userId, long amount, CoinTransactionType type, UUID referenceId, String description) {
        if (amount < 0) {
            throw new IllegalArgumentException("Abbuchungsbetrag darf nicht negativ sein");
        }
        Wallet wallet = lockedWallet(userId);
        if (amount == 0) {
            return wallet;
        }
        if (isFeeExempt(userId)) {
            log.debug("Superadmin {}: Abbuchung {} ({}) entfaellt", userId, amount, type);
            return wallet;
        }
        if (wallet.getBalance() < amount) {
            throw new InsufficientCoinsException(amount, wallet.getBalance());
        }
        wallet.setBalance(wallet.getBalance() - amount);
        record(wallet, type, -amount, referenceId, null, description);
        return wallet;
    }

    @Transactional
    public Wallet credit(UUID userId, long amount, CoinTransactionType type, UUID referenceId,
                         String externalReference, String description) {
        if (amount < 0) {
            throw new IllegalArgumentException("Gutschriftsbetrag darf nicht negativ sein");
        }
        Wallet wallet = lockedWallet(userId);
        if (amount == 0) {
            return wallet;
        }
        wallet.setBalance(wallet.getBalance() + amount);
        record(wallet, type, amount, referenceId, externalReference, description);
        return wallet;
    }

    @Transactional(readOnly = true)
    public Wallet get(UUID userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet für User " + userId + " nicht gefunden"));
    }

    @Transactional(readOnly = true)
    public Page<CoinTransaction> ledger(UUID userId, Pageable pageable) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public boolean isFeeExempt(UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId).map(User::isSuperAdmin).orElse(false);
    }

    private Wallet lockedWallet(UUID userId) {
        return walletRepository.findForUpdateByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet für User " + userId + " nicht gefunden"));
    }

    private void record(Wallet wallet, CoinTransactionType type, long signedAmount, UUID referenceId,
                        String externalReference, String description) {
        CoinTransaction transaction = new CoinTransaction();
        transaction.setWalletId(wallet.getId());
        transaction.setUserId(wallet.getUserId());
        transaction.setType(type);
        transaction.setAmount(signedAmount);
        transaction.setBalanceAfter(wallet.getBalance());
        transaction.setReferenceId(referenceId);
        transaction.setExternalReference(externalReference);
        transaction.setDescription(description);
        transactionRepository.save(transaction);
    }
}
