package at.stoneforge.gnoeckly.push;

import java.util.Collection;
import java.util.List;

/**
 * Versandkanal fuer Push-Nachrichten. Implementierungen duerfen keine Entities anfassen (laufen ggf.
 * in einem eigenen Thread ohne {@code TenantContext}) und liefern die Tokens zurueck, die der
 * Provider als dauerhaft ungueltig gemeldet hat; {@link PushService} raeumt sie danach auf.
 */
public interface PushSender {

    List<String> send(Collection<String> tokens, PushMessage message);
}
