package at.stoneforge.gnoeckly.push;

/** Eine Push-Nachricht; {@code route} ist der App-interne Deep-Link, den ein Tipp auf die Notification oeffnet. */
public record PushMessage(String title, String body, String route) {
}
