package at.stoneforge.gnoeckly.sticker;

/**
 * HTTP 409 mit einem der Codes {@code STICKER_SOLD_OUT}, {@code STICKER_UNAVAILABLE} (inaktiv oder
 * ausserhalb des Verkaufszeitraums) oder {@code STICKER_NOT_OWNED} (Verleihen ohne Exemplar).
 */
public class StickerUnavailableException extends RuntimeException {

    public enum Code {
        STICKER_SOLD_OUT("Dieser Sticker ist ausverkauft."),
        STICKER_UNAVAILABLE("Dieser Sticker ist derzeit nicht erhältlich."),
        STICKER_NOT_OWNED("Du besitzt keinen Sticker dieser Art mehr.");

        private final String message;

        Code(String message) {
            this.message = message;
        }
    }

    private final Code code;

    public StickerUnavailableException(Code code) {
        super(code.message);
        this.code = code;
    }

    public Code getCode() {
        return code;
    }
}
