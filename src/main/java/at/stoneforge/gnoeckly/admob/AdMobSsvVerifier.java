package at.stoneforge.gnoeckly.admob;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;
import java.util.Optional;

/**
 * Prueft die ECDSA-Signatur eines AdMob-SSV-Callbacks nach Googles Vorgabe: signiert ist der rohe
 * Query-String bis unmittelbar VOR {@code &signature=} (alle Parameter davor, URL-kodiert wie
 * empfangen), Signatur ist base64url-kodiertes DER-ECDSA (P-256/SHA-256); {@code key_id} steht
 * hinter {@code signature} und ist damit nicht Teil der Nachricht.
 */
@Component
public class AdMobSsvVerifier {

    private static final Logger log = LoggerFactory.getLogger(AdMobSsvVerifier.class);
    private static final String SIGNATURE_MARKER = "&signature=";

    private final AdMobVerifierKeyProvider keyProvider;

    public AdMobSsvVerifier(AdMobVerifierKeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

    public boolean verify(String rawQueryString, long keyId, String signatureBase64Url) {
        if (rawQueryString == null || signatureBase64Url == null) {
            return false;
        }
        int marker = rawQueryString.indexOf(SIGNATURE_MARKER);
        if (marker <= 0) {
            return false;
        }
        Optional<PublicKey> key = keyProvider.key(keyId);
        if (key.isEmpty()) {
            log.warn("AdMob SSV: unbekannte key_id {}", keyId);
            return false;
        }
        byte[] message = rawQueryString.substring(0, marker).getBytes(StandardCharsets.UTF_8);
        try {
            byte[] signature = Base64.getUrlDecoder().decode(signatureBase64Url);
            Signature verifier = Signature.getInstance("SHA256withECDSA");
            verifier.initVerify(key.get());
            verifier.update(message);
            return verifier.verify(signature);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            log.warn("AdMob SSV: Signaturpruefung fehlgeschlagen: {}", e.toString());
            return false;
        }
    }
}
