package at.stoneforge.gnoeckly.wallet;

import java.time.Instant;
import java.util.UUID;

public record CoinTransactionResponse(UUID id, CoinTransactionType type, long amount, long balanceAfter,
                                      UUID referenceId, String description, Instant createdAt) {

    public static CoinTransactionResponse from(CoinTransaction transaction) {
        return new CoinTransactionResponse(transaction.getId(), transaction.getType(), transaction.getAmount(),
                transaction.getBalanceAfter(), transaction.getReferenceId(), transaction.getDescription(),
                transaction.getCreatedAt());
    }
}
