package at.stoneforge.gnoeckly.push;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Geraete-Token des eingeloggten Users (Registrierung nach Login/Berechtigung, Abmeldung bei Logout/Opt-out). */
@RestController
@RequestMapping("/api/v1/me/push-tokens")
public class PushController {

    private final PushService pushService;

    public PushController(PushService pushService) {
        this.pushService = pushService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void register(@AuthenticationPrincipal UUID userId, @Valid @RequestBody RegisterPushTokenRequest request) {
        pushService.register(userId, request.token(), request.platform());
    }

    /** Token als Query-Parameter: FCM-Tokens sind lang und enthalten Zeichen, die im Pfad stoeren. */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unregister(@AuthenticationPrincipal UUID userId, @RequestParam String token) {
        pushService.unregister(userId, token);
    }
}
