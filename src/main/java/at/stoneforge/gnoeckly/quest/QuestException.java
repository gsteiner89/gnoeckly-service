package at.stoneforge.gnoeckly.quest;

/** HTTP 409 mit {@code QUEST_ALREADY_CLAIMED} oder {@code QUEST_NOT_COMPLETED}. */
public class QuestException extends RuntimeException {

    public enum Code {
        QUEST_ALREADY_CLAIMED("Diese Aufgabe hast du heute schon eingelöst."),
        QUEST_NOT_COMPLETED("Diese Aufgabe ist noch nicht erledigt.");

        private final String message;

        Code(String message) {
            this.message = message;
        }
    }

    private final Code code;

    public QuestException(Code code) {
        super(code.message);
        this.code = code;
    }

    public Code getCode() {
        return code;
    }
}
