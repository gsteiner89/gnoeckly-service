package at.stoneforge.gnoeckly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Midgards eigene Beans/Entities/Repositories kommen ueber midgard-cores AutoConfiguration.imports
 * (MidgardComponentScanConfiguration), decken aber nur at.stoneforge.midgard ab. Fuer Gnoecklys
 * eigenes Root-Package braucht es trotzdem dieses explizite {@code @EntityScan}/
 * {@code @EnableJpaRepositories}: sobald irgendwo im Kontext ein explizites
 * {@code @EnableJpaRepositories} existiert (Midgards), schaltet sich Spring Boots impliziter
 * Default-Scan des {@code @SpringBootApplication}-Packages komplett ab (in saga-service live so
 * aufgefallen, siehe SagaApplication).
 */
@SpringBootApplication
@EntityScan(basePackages = "at.stoneforge.gnoeckly")
@EnableJpaRepositories(basePackages = "at.stoneforge.gnoeckly")
@ConfigurationPropertiesScan(basePackages = "at.stoneforge.gnoeckly")
public class GnoecklyApplication {

    public static void main(String[] args) {
        SpringApplication.run(GnoecklyApplication.class, args);
    }
}
