package at.stoneforge.gnoeckly.favorite;

import at.stoneforge.gnoeckly.joke.Joke;
import at.stoneforge.gnoeckly.joke.JokeRepository;
import at.stoneforge.gnoeckly.joke.JokeResponse;
import at.stoneforge.gnoeckly.joke.JokeResponseAssembler;
import at.stoneforge.gnoeckly.joke.JokeStatus;
import at.stoneforge.midgard.web.PagedResponse;
import at.stoneforge.midgard.web.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Favorisieren ist idempotent: doppeltes Setzen bzw. Entfernen ist kein Fehler. */
@Service
public class FavoriteService {

    private final JokeRepository jokeRepository;
    private final JokeFavoriteRepository favoriteRepository;
    private final JokeResponseAssembler assembler;

    public FavoriteService(JokeRepository jokeRepository, JokeFavoriteRepository favoriteRepository,
                           JokeResponseAssembler assembler) {
        this.jokeRepository = jokeRepository;
        this.favoriteRepository = favoriteRepository;
        this.assembler = assembler;
    }

    @Transactional
    public void add(UUID jokeId, UUID userId) {
        Joke joke = jokeRepository.findByIdAndDeletedAtIsNull(jokeId)
                .filter(j -> j.getStatus() == JokeStatus.APPROVED)
                .orElseThrow(() -> new ResourceNotFoundException("Witz " + jokeId + " nicht gefunden"));
        if (favoriteRepository.findByJokeIdAndUserId(joke.getId(), userId).isEmpty()) {
            JokeFavorite favorite = new JokeFavorite();
            favorite.setJokeId(joke.getId());
            favorite.setUserId(userId);
            favoriteRepository.saveAndFlush(favorite);
        }
    }

    @Transactional
    public void remove(UUID jokeId, UUID userId) {
        favoriteRepository.findByJokeIdAndUserId(jokeId, userId).ifPresent(favoriteRepository::delete);
    }

    @Transactional(readOnly = true)
    public PagedResponse<JokeResponse> list(UUID userId, Pageable pageable) {
        Page<Joke> page = favoriteRepository.findFavoriteJokes(userId, pageable);
        return new PagedResponse<>(assembler.assemble(page.getContent(), userId, false),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
