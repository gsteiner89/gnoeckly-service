package at.stoneforge.gnoeckly.vote;

import at.stoneforge.gnoeckly.joke.Joke;
import at.stoneforge.gnoeckly.joke.JokeRepository;
import at.stoneforge.gnoeckly.joke.JokeStatus;
import at.stoneforge.midgard.web.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Reddit-artige Votes. Die Zaehler am Witz werden ausschliesslich ueber
 * {@link JokeRepository#applyVoteDelta} atomar veraendert; diese Klasse berechnet nur das Delta
 * aus altem und neuem Vote (neu, Wechsel, gleich, entfernt).
 */
@Service
public class VoteService {

    private final JokeRepository jokeRepository;
    private final JokeVoteRepository voteRepository;

    public VoteService(JokeRepository jokeRepository, JokeVoteRepository voteRepository) {
        this.jokeRepository = jokeRepository;
        this.voteRepository = voteRepository;
    }

    @Transactional
    public VoteResponse vote(UUID jokeId, UUID userId, int value) {
        if (value != 1 && value != -1) {
            throw new IllegalArgumentException("Vote muss +1 oder -1 sein");
        }
        Joke joke = requireApproved(jokeId);
        Optional<JokeVote> existing = voteRepository.findByJokeIdAndUserId(jokeId, userId);
        int dUp = 0;
        int dDown = 0;
        if (existing.isEmpty()) {
            JokeVote vote = new JokeVote();
            vote.setJokeId(jokeId);
            vote.setUserId(userId);
            vote.setValue((short) value);
            voteRepository.saveAndFlush(vote);
            if (value == 1) {
                dUp = 1;
            } else {
                dDown = 1;
            }
        } else if (existing.get().getValue() != value) {
            existing.get().setValue((short) value);
            voteRepository.saveAndFlush(existing.get());
            if (value == 1) {
                dUp = 1;
                dDown = -1;
            } else {
                dUp = -1;
                dDown = 1;
            }
        }
        if (dUp != 0 || dDown != 0) {
            jokeRepository.applyVoteDelta(joke.getId(), dUp, dDown);
        }
        return current(jokeId, value);
    }

    @Transactional
    public VoteResponse removeVote(UUID jokeId, UUID userId) {
        Joke joke = requireApproved(jokeId);
        Optional<JokeVote> existing = voteRepository.findByJokeIdAndUserId(jokeId, userId);
        if (existing.isPresent()) {
            short oldValue = existing.get().getValue();
            voteRepository.delete(existing.get());
            voteRepository.flush();
            jokeRepository.applyVoteDelta(joke.getId(), oldValue == 1 ? -1 : 0, oldValue == -1 ? -1 : 0);
        }
        return current(jokeId, null);
    }

    private VoteResponse current(UUID jokeId, Integer myVote) {
        Joke reloaded = jokeRepository.findByIdAndDeletedAtIsNull(jokeId)
                .orElseThrow(() -> new ResourceNotFoundException("Witz " + jokeId + " nicht gefunden"));
        return new VoteResponse(jokeId, reloaded.getUpvotes(), reloaded.getDownvotes(), reloaded.getScore(), myVote);
    }

    private Joke requireApproved(UUID jokeId) {
        return jokeRepository.findByIdAndDeletedAtIsNull(jokeId)
                .filter(joke -> joke.getStatus() == JokeStatus.APPROVED)
                .orElseThrow(() -> new ResourceNotFoundException("Witz " + jokeId + " nicht gefunden"));
    }
}
