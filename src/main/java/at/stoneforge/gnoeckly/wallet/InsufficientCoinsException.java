package at.stoneforge.gnoeckly.wallet;

/**
 * Zu wenig Gnoecken fuer eine Abbuchung. Wird zu HTTP 409 {@code INSUFFICIENT_COINS}: der Request
 * ist korrekt, nur der Zustand (Kontostand) passt nicht - nach einem Rewarded Ad ist er wiederholbar.
 */
public class InsufficientCoinsException extends RuntimeException {

    private final long required;
    private final long balance;

    public InsufficientCoinsException(long required, long balance) {
        super("Nicht genug Gnöcken: benötigt " + required + ", vorhanden " + balance);
        this.required = required;
        this.balance = balance;
    }

    public long getRequired() {
        return required;
    }

    public long getBalance() {
        return balance;
    }
}
