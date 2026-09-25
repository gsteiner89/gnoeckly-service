package at.stoneforge.gnoeckly;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test-Ersatzbeans: verstellbare {@link MutableClock} und {@link RecordingPushSender}. Explizites
 * {@code @Import} noetig, weil {@code @TestConfiguration}s bei {@code @SpringBootTest(classes = ...)}
 * nicht automatisch gefunden werden; derselbe Import in mehreren ITs teilt sich den Spring-Context.
 */
@TestConfiguration
public class TestSupportConfiguration {

    @Bean
    @Primary
    MutableClock mutableClock() {
        return new MutableClock();
    }

    @Bean
    @Primary
    RecordingPushSender recordingPushSender() {
        return new RecordingPushSender();
    }
}
