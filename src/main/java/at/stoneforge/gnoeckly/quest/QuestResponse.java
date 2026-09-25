package at.stoneforge.gnoeckly.quest;

import java.time.LocalDate;
import java.util.List;

/** Die drei Daily Quests des heutigen Tages (Zeitzone laut {@code Clock}). */
public record QuestResponse(LocalDate date, List<Quest> quests) {

    public record Quest(String key, int target, int progress, long reward, boolean claimed, boolean claimable) {
    }
}
