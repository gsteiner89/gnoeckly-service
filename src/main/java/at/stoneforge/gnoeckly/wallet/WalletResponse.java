package at.stoneforge.gnoeckly.wallet;

import java.time.Instant;

public record WalletResponse(long balance, Instant updatedAt) {

    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(wallet.getBalance(), wallet.getUpdatedAt());
    }
}
