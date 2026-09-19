package at.stoneforge.gnoeckly.profile;

import at.stoneforge.gnoeckly.joke.JokeRepository;
import at.stoneforge.gnoeckly.joke.JokeStatus;
import at.stoneforge.gnoeckly.sticker.StickerService;
import at.stoneforge.gnoeckly.wallet.WalletService;
import at.stoneforge.midgard.user.User;
import at.stoneforge.midgard.user.UserRepository;
import at.stoneforge.midgard.web.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProfileService {

    private final UserProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final JokeRepository jokeRepository;
    private final WalletService walletService;
    private final StickerService stickerService;

    public ProfileService(UserProfileRepository profileRepository, UserRepository userRepository,
                          JokeRepository jokeRepository, WalletService walletService, StickerService stickerService) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.jokeRepository = jokeRepository;
        this.walletService = walletService;
        this.stickerService = stickerService;
    }

    @Transactional(readOnly = true)
    public MeResponse me(UUID userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User nicht gefunden"));
        UserProfile profile = requireProfile(userId);
        return new MeResponse(userId, user.getEmail(), profile.getNickname(), profile.getBio(), user.isSuperAdmin(),
                walletService.get(userId).getBalance(), jokeRepository.karmaOf(userId));
    }

    @Transactional
    public MeResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        UserProfile profile = requireProfile(userId);
        if (!profile.getNickname().equalsIgnoreCase(request.nickname())
                && profileRepository.existsByNicknameIgnoreCaseAndDeletedAtIsNull(request.nickname())) {
            throw new NicknameTakenException(request.nickname());
        }
        profile.setNickname(request.nickname());
        profile.setBio(request.bio());
        return me(userId);
    }

    @Transactional(readOnly = true)
    public PublicProfileResponse publicProfile(UUID userId) {
        UserProfile profile = requireProfile(userId);
        return new PublicProfileResponse(userId, profile.getNickname(), profile.getBio(),
                jokeRepository.karmaOf(userId),
                jokeRepository.countByAuthorIdAndStatusAndDeletedAtIsNull(userId, JokeStatus.APPROVED),
                stickerService.collection(userId));
    }

    /** Nickname-Map fuer Listen (Feed, Rankings, Sticker-Verleiher) in einer Query. */
    @Transactional(readOnly = true)
    public Map<UUID, String> nicknames(Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return profileRepository.findByUserIdInAndDeletedAtIsNull(userIds).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, UserProfile::getNickname, (a, b) -> a));
    }

    private UserProfile requireProfile(UUID userId) {
        return profileRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profil nicht gefunden"));
    }

    static <T> Map<UUID, T> byId(Collection<T> items, Function<T, UUID> idOf) {
        return items.stream().collect(Collectors.toMap(idOf, Function.identity(), (a, b) -> a));
    }
}
