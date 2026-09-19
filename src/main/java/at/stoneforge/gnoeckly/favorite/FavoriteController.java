package at.stoneforge.gnoeckly.favorite;

import at.stoneforge.gnoeckly.joke.JokeResponse;
import at.stoneforge.midgard.web.PagedResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping("/api/v1/jokes/{jokeId}/favorite")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void add(@PathVariable UUID jokeId, @AuthenticationPrincipal UUID userId) {
        favoriteService.add(jokeId, userId);
    }

    @DeleteMapping("/api/v1/jokes/{jokeId}/favorite")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable UUID jokeId, @AuthenticationPrincipal UUID userId) {
        favoriteService.remove(jokeId, userId);
    }

    @GetMapping("/api/v1/me/favorites")
    public PagedResponse<JokeResponse> favorites(@AuthenticationPrincipal UUID userId,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return favoriteService.list(userId, PageRequest.of(page, Math.min(size, 100)));
    }
}
