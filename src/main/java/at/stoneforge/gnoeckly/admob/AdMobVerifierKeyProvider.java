package at.stoneforge.gnoeckly.admob;

import java.security.PublicKey;
import java.util.Optional;

/** Liefert den oeffentlichen EC-Schluessel zu einer AdMob-{@code key_id}; Tests ersetzen die Bean per {@code @Primary}. */
public interface AdMobVerifierKeyProvider {

    Optional<PublicKey> key(long keyId);

    void refresh();
}
