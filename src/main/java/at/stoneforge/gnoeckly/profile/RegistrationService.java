package at.stoneforge.gnoeckly.profile;

import at.stoneforge.gnoeckly.config.GnoecklySettings;
import at.stoneforge.gnoeckly.tenant.DefaultTenantResolver;
import at.stoneforge.gnoeckly.wallet.WalletService;
import at.stoneforge.midgard.security.AuthenticationService;
import at.stoneforge.midgard.security.LoginResponse;
import at.stoneforge.midgard.user.User;
import at.stoneforge.midgard.user.UserRepository;
import at.stoneforge.midgard.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Self-Registration - Midgard selbst hat keinen Register-Endpoint (nur Login/Refresh). Legt in
 * einer Transaktion Midgard-User (Argon2 via {@link UserService}), Profil und Wallet mit
 * Startguthaben an und gibt direkt Tokens zurueck, damit die App ohne zweiten Login-Schritt
 * weiterkommt. Der Tenant ist immer der Default-Tenant (Single-Tenant-App).
 */
@Service
public class RegistrationService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final WalletService walletService;
    private final GnoecklySettings settings;
    private final DefaultTenantResolver defaultTenantResolver;
    private final AuthenticationService authenticationService;

    public RegistrationService(UserService userService, UserRepository userRepository,
                               UserProfileRepository profileRepository, WalletService walletService,
                               GnoecklySettings settings, DefaultTenantResolver defaultTenantResolver,
                               AuthenticationService authenticationService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.walletService = walletService;
        this.settings = settings;
        this.defaultTenantResolver = defaultTenantResolver;
        this.authenticationService = authenticationService;
    }

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.findByEmail(email).isPresent()) {
            throw new EmailTakenException();
        }
        if (profileRepository.existsByNicknameIgnoreCaseAndDeletedAtIsNull(request.nickname())) {
            throw new NicknameTakenException(request.nickname());
        }
        UUID tenantId = defaultTenantResolver.defaultTenantId()
                .orElseThrow(() -> new IllegalStateException("Default-Tenant nicht vorhanden - Bootstrap-Upgrade gelaufen?"));

        User user = userService.create(tenantId, email, request.password(), null, null, "de");

        UserProfile profile = new UserProfile();
        profile.setUserId(user.getId());
        profile.setNickname(request.nickname());
        profileRepository.save(profile);

        walletService.createForUser(user.getId(), settings.welcomeCoins());

        return authenticationService.login(email, request.password());
    }
}
