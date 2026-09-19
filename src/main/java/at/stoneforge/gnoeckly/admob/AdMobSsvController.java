package at.stoneforge.gnoeckly.admob;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * AdMob Server-Side-Verification-Callback (in der AdMob-Konsole als Callback-URL eingetragen,
 * oeffentlich per HTTPS). Google ruft {@code GET} mit {@code ad_network, ad_unit, custom_data,
 * key_id, reward_amount, reward_item, signature, timestamp, transaction_id, user_id} auf.
 * Ungueltige Signatur → 400 (Google wiederholt einige Male, gebucht wird nichts). Alles andere →
 * 200, auch bei Duplikat oder unbekanntem User, damit Google nicht endlos wiederholt. Tokenlos,
 * Tenant kommt vom DefaultTenantFilter.
 */
@RestController
@RequestMapping("/api/v1/public/admob/ssv")
public class AdMobSsvController {

    private static final Logger log = LoggerFactory.getLogger(AdMobSsvController.class);

    private final AdMobSsvVerifier verifier;
    private final AdMobSsvService ssvService;

    public AdMobSsvController(AdMobSsvVerifier verifier, AdMobSsvService ssvService) {
        this.verifier = verifier;
        this.ssvService = ssvService;
    }

    @GetMapping
    public ResponseEntity<Void> callback(HttpServletRequest request,
                                         @RequestParam("key_id") long keyId,
                                         @RequestParam("signature") String signature,
                                         @RequestParam("transaction_id") String transactionId,
                                         @RequestParam(value = "user_id", required = false) String userId,
                                         @RequestParam(value = "reward_amount", required = false, defaultValue = "0") long rewardAmount,
                                         @RequestParam(value = "ad_unit", required = false) String adUnit) {
        // Roh aus dem Request, NICHT aus dekodierten @RequestParams zusammengesetzt - die Signatur
        // gilt fuer die Bytes, wie Google sie gesendet hat.
        if (!verifier.verify(request.getQueryString(), keyId, signature)) {
            log.warn("AdMob SSV abgelehnt: ungueltige Signatur (transaction_id {})", transactionId);
            return ResponseEntity.badRequest().build();
        }
        UUID parsedUserId = parseUuid(userId);
        if (parsedUserId == null) {
            log.warn("AdMob SSV ohne verwertbare user_id '{}' (transaction_id {}) - keine Buchung", userId, transactionId);
            return ResponseEntity.ok().build();
        }
        try {
            ssvService.credit(parsedUserId, transactionId, adUnit, rewardAmount);
        } catch (DataIntegrityViolationException raceOnUniqueIndex) {
            log.debug("AdMob SSV: transaction_id {} parallel bereits gebucht", transactionId);
        }
        return ResponseEntity.ok().build();
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
