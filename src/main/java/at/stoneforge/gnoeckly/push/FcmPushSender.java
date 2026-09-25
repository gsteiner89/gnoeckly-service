package at.stoneforge.gnoeckly.push;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Echter Versand ueber Firebase Cloud Messaging (Android direkt, iOS ueber APNs-Key in Firebase).
 * Nur aktiv mit {@code gnoeckly.push.enabled=true}; die Service-Account-JSON liegt ausserhalb des
 * Repos ({@code GNOECKLY_FIREBASE_CREDENTIALS}, siehe docs/push-fcm.md).
 */
@Component
@ConditionalOnProperty(name = "gnoeckly.push.enabled", havingValue = "true")
public class FcmPushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(FcmPushSender.class);

    private final FirebaseMessaging messaging;

    public FcmPushSender(PushProperties properties) throws IOException {
        if (properties.credentialsPath().isBlank()) {
            throw new IllegalStateException(
                    "gnoeckly.push.enabled=true braucht gnoeckly.push.credentials-path (GNOECKLY_FIREBASE_CREDENTIALS)");
        }
        try (InputStream credentials = new FileInputStream(properties.credentialsPath())) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentials))
                    .build();
            FirebaseApp app = FirebaseApp.getApps().isEmpty() ? FirebaseApp.initializeApp(options) : FirebaseApp.getInstance();
            this.messaging = FirebaseMessaging.getInstance(app);
        }
    }

    @Override
    public List<String> send(Collection<String> tokens, PushMessage message) {
        List<String> tokenList = List.copyOf(tokens);
        MulticastMessage multicast = MulticastMessage.builder()
                .addAllTokens(tokenList)
                .setNotification(Notification.builder().setTitle(message.title()).setBody(message.body()).build())
                .putData("route", message.route())
                .setAndroidConfig(AndroidConfig.builder().setPriority(AndroidConfig.Priority.NORMAL).build())
                .build();
        List<String> invalid = new ArrayList<>();
        try {
            BatchResponse batch = messaging.sendEachForMulticast(multicast);
            List<SendResponse> responses = batch.getResponses();
            for (int i = 0; i < responses.size(); i++) {
                SendResponse response = responses.get(i);
                if (!response.isSuccessful()) {
                    FirebaseMessagingException exception = response.getException();
                    if (exception != null && exception.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                        invalid.add(tokenList.get(i));
                    } else {
                        log.warn("FCM-Versand fehlgeschlagen: {}", exception == null ? "?" : exception.getMessage());
                    }
                }
            }
        } catch (FirebaseMessagingException e) {
            log.warn("FCM-Versand fehlgeschlagen: {}", e.getMessage());
        }
        return invalid;
    }
}
