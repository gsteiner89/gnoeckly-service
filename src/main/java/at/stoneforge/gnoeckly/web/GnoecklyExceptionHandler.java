package at.stoneforge.gnoeckly.web;

import at.stoneforge.gnoeckly.profile.EmailTakenException;
import at.stoneforge.gnoeckly.profile.NicknameTakenException;
import at.stoneforge.gnoeckly.quest.QuestException;
import at.stoneforge.gnoeckly.sticker.StickerUnavailableException;
import at.stoneforge.gnoeckly.streak.StreakFreezeLimitException;
import at.stoneforge.gnoeckly.wallet.InsufficientCoinsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Gnoeckly-eigene Fehlerfaelle im RFC-7807-Format mit stabilem {@code code} (midgard CLAUDE.md
 * Regel 12). Bewusst NICHT von {@code ResponseEntityExceptionHandler} abgeleitet - das taete bereits
 * Midgards {@code GlobalExceptionHandler}, und zwei Ableitungen im selben Context kollidieren bei
 * den eingebauten MVC-Exceptions. 404/400/409-Standardfaelle laufen weiterhin ueber Midgard.
 */
@RestControllerAdvice
public class GnoecklyExceptionHandler {

    @ExceptionHandler(InsufficientCoinsException.class)
    public ProblemDetail handleInsufficientCoins(InsufficientCoinsException exception) {
        ProblemDetail problem = problemDetail(HttpStatus.CONFLICT, "INSUFFICIENT_COINS", exception.getMessage());
        problem.setProperty("required", exception.getRequired());
        problem.setProperty("balance", exception.getBalance());
        return problem;
    }

    @ExceptionHandler(NicknameTakenException.class)
    public ProblemDetail handleNicknameTaken(NicknameTakenException exception) {
        return problemDetail(HttpStatus.CONFLICT, "NICKNAME_TAKEN", exception.getMessage());
    }

    @ExceptionHandler(EmailTakenException.class)
    public ProblemDetail handleEmailTaken(EmailTakenException exception) {
        return problemDetail(HttpStatus.CONFLICT, "EMAIL_TAKEN", exception.getMessage());
    }

    @ExceptionHandler(StickerUnavailableException.class)
    public ProblemDetail handleStickerUnavailable(StickerUnavailableException exception) {
        return problemDetail(HttpStatus.CONFLICT, exception.getCode().name(), exception.getMessage());
    }

    @ExceptionHandler(StreakFreezeLimitException.class)
    public ProblemDetail handleStreakFreezeLimit(StreakFreezeLimitException exception) {
        return problemDetail(HttpStatus.CONFLICT, "STREAK_FREEZE_LIMIT", exception.getMessage());
    }

    @ExceptionHandler(QuestException.class)
    public ProblemDetail handleQuest(QuestException exception) {
        return problemDetail(HttpStatus.CONFLICT, exception.getCode().name(), exception.getMessage());
    }

    private static ProblemDetail problemDetail(HttpStatus status, String code, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setProperty("code", code);
        return problemDetail;
    }
}
