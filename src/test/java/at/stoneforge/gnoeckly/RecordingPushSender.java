package at.stoneforge.gnoeckly;

import at.stoneforge.gnoeckly.push.PushMessage;
import at.stoneforge.gnoeckly.push.PushSender;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Test-Ersatz fuer den echten Versand: merkt sich Nachrichten, kann Fehler und ungueltige Tokens simulieren. */
public class RecordingPushSender implements PushSender {

    public record Sent(List<String> tokens, PushMessage message) {
    }

    private final List<Sent> sent = new CopyOnWriteArrayList<>();
    private volatile boolean failing;
    private volatile List<String> invalidTokens = List.of();

    @Override
    public List<String> send(Collection<String> tokens, PushMessage message) {
        if (failing) {
            throw new IllegalStateException("simulierter Versandfehler");
        }
        sent.add(new Sent(List.copyOf(tokens), message));
        return invalidTokens;
    }

    public List<Sent> sent() {
        return new ArrayList<>(sent);
    }

    /** Nachrichten, die an mindestens eines der Tokens gingen. */
    public List<PushMessage> messagesTo(String token) {
        return sent.stream().filter(entry -> entry.tokens().contains(token)).map(Sent::message).toList();
    }

    public void reset() {
        sent.clear();
        failing = false;
        invalidTokens = List.of();
    }

    public void setFailing(boolean failing) {
        this.failing = failing;
    }

    public void setInvalidTokens(List<String> invalidTokens) {
        this.invalidTokens = invalidTokens;
    }
}
