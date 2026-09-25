package at.stoneforge.gnoeckly.push;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/** Standard ohne Firebase (lokal, Tests): loggt nur, verschickt nichts. */
@Component
@ConditionalOnProperty(name = "gnoeckly.push.enabled", havingValue = "false", matchIfMissing = true)
public class NoopPushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(NoopPushSender.class);

    @Override
    public List<String> send(Collection<String> tokens, PushMessage message) {
        log.debug("Push deaktiviert - '{}' an {} Geraet(e) nicht gesendet.", message.title(), tokens.size());
        return List.of();
    }
}
