package at.stoneforge.gnoeckly.joke;

import at.stoneforge.gnoeckly.sticker.JokeStickerResponse;
import at.stoneforge.gnoeckly.sticker.StickerService;
import at.stoneforge.midgard.web.PagedResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Oeffentlicher Feed. Der Pfad ist permitAll, Midgards JwtAuthenticationFilter laeuft trotzdem -
 * mit gueltigem Token ist {@code viewerId} gesetzt und {@code myVote} wird angereichert, sonst null.
 */
@RestController
@RequestMapping("/api/v1/public/jokes")
public class PublicJokeController {

    private final JokeService jokeService;
    private final StickerService stickerService;

    public PublicJokeController(JokeService jokeService, StickerService stickerService) {
        this.jokeService = jokeService;
        this.stickerService = stickerService;
    }

    @GetMapping
    public PagedResponse<JokeResponse> feed(@RequestParam(defaultValue = "HOT") FeedSort sort,
                                            @RequestParam(defaultValue = "ALL") Period period,
                                            @RequestParam(required = false) List<UUID> categoryIds,
                                            @RequestParam(defaultValue = "false") boolean favoritesOnly,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size,
                                            @AuthenticationPrincipal UUID viewerId) {
        return jokeService.feed(sort, period, categoryIds, favoritesOnly, viewerId, PageRequest.of(page, Math.min(size, 100)));
    }

    @GetMapping("/{id}")
    public JokeResponse get(@PathVariable UUID id, @AuthenticationPrincipal UUID viewerId) {
        return jokeService.getApproved(id, viewerId);
    }

    @GetMapping("/{id}/stickers")
    public PagedResponse<JokeStickerResponse> stickers(@PathVariable UUID id,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        jokeService.requireApproved(id);
        return PagedResponse.from(stickerService.awardsOf(id, PageRequest.of(page, Math.min(size, 100))));
    }
}
