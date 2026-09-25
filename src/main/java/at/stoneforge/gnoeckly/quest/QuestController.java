package at.stoneforge.gnoeckly.quest;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/me/quests")
public class QuestController {

    private final QuestService questService;

    public QuestController(QuestService questService) {
        this.questService = questService;
    }

    @GetMapping
    public QuestResponse quests(@AuthenticationPrincipal UUID userId) {
        return questService.today(userId);
    }

    @PostMapping("/{key}/claim")
    public QuestResponse claim(@AuthenticationPrincipal UUID userId, @PathVariable String key) {
        return questService.claim(userId, key);
    }
}
