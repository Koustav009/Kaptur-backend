package com.koustav.kaptur.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Central CORS policy for the API.
 *
 * Why this lives in a {@code CorsConfigurationSource} bean and is enabled via
 * {@code http.cors(...)} in {@link com.koustav.kaptur.security.SecurityConfig}:
 * Spring Security's filter chain runs BEFORE Spring MVC, so if CORS is only
 * configured with {@code WebMvcConfigurer}, browser preflight (OPTIONS) requests
 * are rejected by {@code anyRequest().authenticated()} with a 401 before any CORS
 * header is written. Wiring it through the security filter chain lets
 * {@code CorsFilter} answer preflight requests and attach CORS headers to real
 * responses.
 *
 * Origins use PATTERNS (not exact origins) because Flutter Web (`flutter run -d
 * chrome`) serves the app on a random localhost port every run. Patterns such as
 * {@code http://localhost:*} match any port, so the frontend does not need to be
 * re-whitelisted on every launch. Extra origins are configurable via
 * {@code app.cors.allowed-origin-patterns}.
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origin-patterns:http://localhost:[*],http://127.0.0.1:[*]}")
    private List<String> allowedOriginPatterns;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Dynamic-port friendly origin patterns; matched origins are echoed back.
        config.setAllowedOriginPatterns(allowedOriginPatterns);

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"));

        // The Flutter client sends Content-Type + Authorization (Accept is added by the
        // browser). Enumerated explicitly because allowCredentials(true) means the
        // wildcard "*" is not honored for headers.
        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With",
                "Cache-Control",
                "Pragma"));

        // Response headers the browser is allowed to read cross-origin.
        config.setExposedHeaders(List.of("Location", "Content-Disposition"));

        // Bearer-token API (no cookies), but kept permissive so cookie/credentialed
        // flows keep working; safe because allowed origins are echoed, never "*".
        config.setAllowCredentials(true);

        // Cache preflight results for 1 hour to cut down on OPTIONS chatter.
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
