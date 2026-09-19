package at.stoneforge.gnoeckly.vote;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jokes/{jokeId}/vote")
public class VoteController {

    private final VoteService voteService;

    public VoteController(VoteService voteService) {
        this.voteService = voteService;
    }

    @PutMapping
    public VoteResponse vote(@PathVariable UUID jokeId, @AuthenticationPrincipal UUID userId,
                             @Valid @RequestBody VoteRequest request) {
        return voteService.vote(jokeId, userId, request.value());
    }

    @DeleteMapping
    public VoteResponse remove(@PathVariable UUID jokeId, @AuthenticationPrincipal UUID userId) {
        return voteService.removeVote(jokeId, userId);
    }
}
