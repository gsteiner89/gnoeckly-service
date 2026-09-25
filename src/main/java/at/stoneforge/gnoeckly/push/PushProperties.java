package at.stoneforge.gnoeckly.push;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * {@code gnoeckly.push.*}: {@code enabled} schaltet den echten FCM-Versand ein (dann ist
 * {@code credentialsPath} Pflicht), {@code async=false} versendet inline (Tests).
 */
@ConfigurationProperties(prefix = "gnoeckly.push")
public record PushProperties(@DefaultValue("false") boolean enabled,
                             @DefaultValue("") String credentialsPath,
                             @DefaultValue("true") boolean async) {
}
