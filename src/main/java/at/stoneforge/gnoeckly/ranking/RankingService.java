package at.stoneforge.gnoeckly.ranking;

import at.stoneforge.gnoeckly.joke.FeedSort;
import at.stoneforge.gnoeckly.joke.JokeRepository;
import at.stoneforge.gnoeckly.joke.JokeResponse;
import at.stoneforge.gnoeckly.joke.JokeService;
import at.stoneforge.gnoeckly.joke.Period;
import at.stoneforge.gnoeckly.profile.ProfileService;
import at.stoneforge.midgard.web.PagedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.List;
import java.util.UUID;

/** Rankings werden zur Query-Zeit aggregiert (kein Karma-Cache, kein Job - TenantContext ist ThreadLocal). */
@Service
public class RankingService {

    private final JokeService jokeService;
    private final JokeRepository jokeRepository;
    private final ProfileService profileService;

    public RankingService(JokeService jokeService, JokeRepository jokeRepository, ProfileService profileService) {
        this.jokeService = jokeService;
        this.jokeRepository = jokeRepository;
        this.profileService = profileService;
    }

    @Transactional(readOnly = true)
    public PagedResponse<JokeResponse> topJokes(Period period, UUID viewerId, Pageable pageable) {
        return jokeService.feed(FeedSort.TOP, period, List.of(), viewerId, pageable);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AuthorRankingResponse> topAuthors(Period period, Pageable pageable) {
        Instant since = period.sinceOrNull(Instant.now());
        Page<AuthorRankingRow> page = since == null
                ? jokeRepository.rankAuthorsAllTime(pageable)
                : jokeRepository.rankAuthorsSince(since, pageable);
        Map<UUID, String> nicknames = profileService.nicknames(page.map(AuthorRankingRow::getAuthorId).toList());
        return PagedResponse.from(page, row -> new AuthorRankingResponse(row.getAuthorId(),
                nicknames.getOrDefault(row.getAuthorId(), "?"), row.getKarma(), row.getApprovedJokes()));
    }
}
