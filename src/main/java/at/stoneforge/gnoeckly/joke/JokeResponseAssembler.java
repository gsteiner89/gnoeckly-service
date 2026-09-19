package at.stoneforge.gnoeckly.joke;

import at.stoneforge.gnoeckly.category.JokeCategory;
import at.stoneforge.gnoeckly.category.JokeCategoryService;
import at.stoneforge.gnoeckly.favorite.JokeFavorite;
import at.stoneforge.gnoeckly.favorite.JokeFavoriteRepository;
import at.stoneforge.gnoeckly.profile.ProfileService;
import at.stoneforge.gnoeckly.sticker.JokeStickerSummary;
import at.stoneforge.gnoeckly.sticker.StickerService;
import at.stoneforge.gnoeckly.vote.JokeVote;
import at.stoneforge.gnoeckly.vote.JokeVoteRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Baut {@link JokeResponse}s fuer eine ganze Seite mit je einer Query pro Zusatzinfo (Nicknames,
 * Kategorien, eigene Votes, Sticker) statt N+1.
 */
@Component
public class JokeResponseAssembler {

    private final ProfileService profileService;
    private final JokeCategoryService categoryService;
    private final JokeVoteRepository voteRepository;
    private final StickerService stickerService;
    private final JokeFavoriteRepository favoriteRepository;

    public JokeResponseAssembler(ProfileService profileService, JokeCategoryService categoryService,
                                 JokeVoteRepository voteRepository, StickerService stickerService,
                                 JokeFavoriteRepository favoriteRepository) {
        this.favoriteRepository = favoriteRepository;
        this.profileService = profileService;
        this.categoryService = categoryService;
        this.voteRepository = voteRepository;
        this.stickerService = stickerService;
    }

    public List<JokeResponse> assemble(List<Joke> jokes, UUID viewerId, boolean includeRejectionReason) {
        if (jokes.isEmpty()) {
            return List.of();
        }
        Instant now = Instant.now();
        List<UUID> jokeIds = jokes.stream().map(Joke::getId).toList();
        Map<UUID, String> nicknames = profileService.nicknames(jokes.stream().map(Joke::getAuthorId).distinct().toList());
        Map<UUID, JokeCategory> categories = categoryService.byIds(jokes.stream().map(Joke::getCategoryId).distinct().toList());
        Map<UUID, Integer> myVotes = new HashMap<>();
        if (viewerId != null) {
            myVotes = voteRepository.findByUserIdAndJokeIdIn(viewerId, jokeIds).stream()
                    .collect(Collectors.toMap(JokeVote::getJokeId, vote -> (int) vote.getValue()));
        }
        Set<UUID> favorites = viewerId == null ? Set.of()
                : favoriteRepository.findByUserIdAndJokeIdIn(viewerId, jokeIds).stream()
                        .map(JokeFavorite::getJokeId).collect(Collectors.toSet());
        Map<UUID, List<JokeStickerSummary>> stickers = stickerService.summariesFor(jokeIds);

        Map<UUID, Integer> votes = myVotes;
        return jokes.stream().map(joke -> {
            JokeCategory category = categories.get(joke.getCategoryId());
            return new JokeResponse(joke.getId(), joke.getTitle(), joke.getText(), joke.getCategoryId(),
                    category == null ? null : category.getName(), joke.getAuthorId(),
                    nicknames.getOrDefault(joke.getAuthorId(), "?"), joke.getStatus(), joke.getUpvotes(),
                    joke.getDownvotes(), joke.getScore(), joke.isBoostedAt(now), joke.getBoostedUntil(),
                    joke.getApprovedAt(), joke.getCreatedAt(), votes.get(joke.getId()),
                    includeRejectionReason ? joke.getRejectionReason() : null,
                    stickers.getOrDefault(joke.getId(), List.of()), favorites.contains(joke.getId()));
        }).toList();
    }

    public JokeResponse assemble(Joke joke, UUID viewerId, boolean includeRejectionReason) {
        return assemble(List.of(joke), viewerId, includeRejectionReason).get(0);
    }
}
