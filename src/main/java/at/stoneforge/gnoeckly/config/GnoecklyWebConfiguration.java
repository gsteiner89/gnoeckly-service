package at.stoneforge.gnoeckly.config;

import at.stoneforge.gnoeckly.tenant.DefaultTenantFilter;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Filter-Reihenfolge (alle vor der Security-Chain bei {@code DEFAULT_FILTER_ORDER}):
 * Midgards OpenEntityManagerInViewFilter (-20) < Midgards TenantFilter (-10) <
 * {@link DefaultTenantFilter} (-5) < Security-Chain (0). Damit hat der DefaultTenantFilter sowohl
 * eine gebundene Session als auch das Ergebnis der JWT-Tenant-Aufloesung vor sich.
 */
@Configuration
public class GnoecklyWebConfiguration {

    @Bean
    public FilterRegistrationBean<DefaultTenantFilter> defaultTenantFilterRegistration(DefaultTenantFilter filter) {
        FilterRegistrationBean<DefaultTenantFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(SecurityProperties.DEFAULT_FILTER_ORDER - 5);
        return registration;
    }
}
