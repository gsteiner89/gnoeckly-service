package at.stoneforge.gnoeckly.profile;

import at.stoneforge.gnoeckly.joke.JokeResponse;
import at.stoneforge.gnoeckly.joke.JokeService;
import at.stoneforge.gnoeckly.sticker.OwnedStickerResponse;
import at.stoneforge.gnoeckly.sticker.StickerService;
import at.stoneforge.midgard.web.PagedResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Alles rund um den eingeloggten User; Principal ist die User-UUID aus Midgards JwtAuthenticationFilter. */
@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    private final ProfileService profileService;
    private final JokeService jokeService;
    private final StickerService stickerService;

    public MeController(ProfileService profileService, JokeService jokeService, StickerService stickerService) {
        this.profileService = profileService;
        this.jokeService = jokeService;
        this.stickerService = stickerService;
    }

    @GetMapping
    public MeResponse me(@AuthenticationPrincipal UUID userId) {
        return profileService.me(userId);
    }

    @PutMapping("/profile")
    public MeResponse updateProfile(@AuthenticationPrincipal UUID userId, @Valid @RequestBody UpdateProfileRequest request) {
        return profileService.updateProfile(userId, request);
    }

    /** Eigene Witze in allen Status, inklusive Ablehnungsgrund. */
    @GetMapping("/jokes")
    public PagedResponse<JokeResponse> myJokes(@AuthenticationPrincipal UUID userId,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return jokeService.mine(userId, PageRequest.of(page, Math.min(size, 100)));
    }

    @GetMapping("/stickers")
    public List<OwnedStickerResponse> myStickers(@AuthenticationPrincipal UUID userId) {
        return stickerService.collection(userId);
    }
}
