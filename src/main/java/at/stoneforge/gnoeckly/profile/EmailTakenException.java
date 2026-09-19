package at.stoneforge.gnoeckly.profile;

/** HTTP 409 {@code EMAIL_TAKEN}. Absichtlich ohne die E-Mail im Text (keine Enumeration nach aussen). */
public class EmailTakenException extends RuntimeException {

    public EmailTakenException() {
        super("Mit dieser E-Mail-Adresse existiert bereits ein Konto.");
    }
}
