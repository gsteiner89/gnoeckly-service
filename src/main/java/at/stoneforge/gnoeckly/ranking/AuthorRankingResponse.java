package at.stoneforge.gnoeckly.ranking;

import java.util.UUID;

public record AuthorRankingResponse(UUID userId, String nickname, long karma, long approvedJokes) {
}
