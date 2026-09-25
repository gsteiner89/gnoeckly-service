package at.stoneforge.gnoeckly.streak;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/me/streak")
public class StreakController {

    private final StreakService streakService;

    public StreakController(StreakService streakService) {
        this.streakService = streakService;
    }

    @GetMapping
    public StreakResponse streak(@AuthenticationPrincipal UUID userId) {
        return streakService.view(userId);
    }

    @PostMapping("/freeze")
    public StreakResponse buyFreeze(@AuthenticationPrincipal UUID userId) {
        return streakService.buyFreeze(userId);
    }
}
