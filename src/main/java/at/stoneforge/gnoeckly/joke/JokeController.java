package at.stoneforge.gnoeckly.joke;

import at.stoneforge.gnoeckly.sticker.AwardStickerRequest;
import at.stoneforge.gnoeckly.sticker.JokeStickerResponse;
import at.stoneforge.gnoeckly.sticker.StickerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Eingeloggte User: einreichen, boosten, zurueckziehen, melden, Sticker verleihen. */
@RestController
@RequestMapping("/api/v1/jokes")
public class JokeController {

    private final JokeService jokeService;
    private final StickerService stickerService;

    public JokeController(JokeService jokeService, StickerService stickerService) {
        this.jokeService = jokeService;
        this.stickerService = stickerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JokeResponse submit(@AuthenticationPrincipal UUID userId, @Valid @RequestBody SubmitJokeRequest request) {
        return jokeService.submit(userId, request);
    }

    @PostMapping("/{id}/boost")
    public JokeResponse boost(@PathVariable UUID id, @AuthenticationPrincipal UUID userId) {
        return jokeService.boost(id, userId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(@PathVariable UUID id, @AuthenticationPrincipal UUID userId) {
        jokeService.withdraw(id, userId);
    }

    @PostMapping("/{id}/report")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void report(@PathVariable UUID id, @AuthenticationPrincipal UUID userId,
                       @Valid @RequestBody ReportJokeRequest request) {
        jokeService.report(id, userId, request.reason());
    }

    @PostMapping("/{id}/stickers")
    @ResponseStatus(HttpStatus.CREATED)
    public JokeStickerResponse award(@PathVariable UUID id, @AuthenticationPrincipal UUID userId,
                                     @Valid @RequestBody AwardStickerRequest request) {
        return stickerService.award(id, request.stickerId(), userId, request.message());
    }
}
