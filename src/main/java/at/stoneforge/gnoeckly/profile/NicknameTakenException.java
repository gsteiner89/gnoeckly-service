package at.stoneforge.gnoeckly.profile;

/** HTTP 409 {@code NICKNAME_TAKEN}. */
public class NicknameTakenException extends RuntimeException {

    public NicknameTakenException(String nickname) {
        super("Der Nickname \"" + nickname + "\" ist bereits vergeben.");
    }
}
