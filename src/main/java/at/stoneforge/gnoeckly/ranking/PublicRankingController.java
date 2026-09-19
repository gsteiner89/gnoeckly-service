package at.stoneforge.gnoeckly.ranking;

import at.stoneforge.gnoeckly.joke.JokeResponse;
import at.stoneforge.gnoeckly.joke.Period;
import at.stoneforge.midgard.web.PagedResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/rankings")
public class PublicRankingController {

    private final RankingService rankingService;

    public PublicRankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    @GetMapping("/jokes")
    public PagedResponse<JokeResponse> jokes(@RequestParam(defaultValue = "WEEK") Period period,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size,
                                             @AuthenticationPrincipal UUID viewerId) {
        return rankingService.topJokes(period, viewerId, PageRequest.of(page, Math.min(size, 100)));
    }

    @GetMapping("/authors")
    public PagedResponse<AuthorRankingResponse> authors(@RequestParam(defaultValue = "WEEK") Period period,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        return rankingService.topAuthors(period, PageRequest.of(page, Math.min(size, 100)));
    }
}
