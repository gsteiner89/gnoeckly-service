package at.stoneforge.gnoeckly.profile;

import at.stoneforge.gnoeckly.joke.JokeResponse;
import at.stoneforge.gnoeckly.joke.JokeService;
import at.stoneforge.midgard.web.PagedResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/users")
public class PublicProfileController {

    private final ProfileService profileService;
    private final JokeService jokeService;

    public PublicProfileController(ProfileService profileService, JokeService jokeService) {
        this.profileService = profileService;
        this.jokeService = jokeService;
    }

    @GetMapping("/{userId}/profile")
    public PublicProfileResponse profile(@PathVariable UUID userId) {
        return profileService.publicProfile(userId);
    }

    /** Freigegebene Witze des Users, bester Score zuerst. Mit gueltigem Token wird myVote/myFavorite angereichert. */
    @GetMapping("/{userId}/jokes")
    public PagedResponse<JokeResponse> jokes(@PathVariable UUID userId,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size,
                                             @AuthenticationPrincipal UUID viewerId) {
        return jokeService.approvedByAuthor(userId, viewerId, PageRequest.of(page, Math.min(size, 100)));
    }
}
