package at.stoneforge.gnoeckly.config;

import at.stoneforge.gnoeckly.tenant.DefaultTenantFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Filter-Reihenfolge (alle vor der Security-Chain bei {@code DEFAULT_FILTER_ORDER}):
 * Midgards OpenEntityManagerInViewFilter (-20) < Midgards TenantFilter (-10) <
 * {@link DefaultTenantFilter} (-5) < Security-Chain (0). Damit hat der DefaultTenantFilter sowohl
 * eine gebundene Session als auch das Ergebnis der JWT-Tenant-Aufloesung vor sich.
 */
@Configuration
@EnableScheduling
public class GnoecklyWebConfiguration {

    /** Tageswechsel des Streaks; Tests ersetzen den Bean durch eine verstellbare Clock. */
    @Bean
    public Clock clock(@Value("${gnoeckly.timezone:Europe/Vienna}") String timezone) {
        return Clock.system(ZoneId.of(timezone));
    }

    @Bean
    public FilterRegistrationBean<DefaultTenantFilter> defaultTenantFilterRegistration(DefaultTenantFilter filter) {
        FilterRegistrationBean<DefaultTenantFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(SecurityProperties.DEFAULT_FILTER_ORDER - 5);
        return registration;
    }
}
