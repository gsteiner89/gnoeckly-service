package at.stoneforge.gnoeckly.profile;

import at.stoneforge.gnoeckly.sticker.OwnedStickerResponse;

import java.util.List;
import java.util.UUID;

public record PublicProfileResponse(UUID userId, String nickname, String bio, long karma, long approvedJokes,
                                    List<OwnedStickerResponse> stickers) {
}
