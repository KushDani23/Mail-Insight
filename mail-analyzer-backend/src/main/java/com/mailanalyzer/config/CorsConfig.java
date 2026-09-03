package com.mailanalyzer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS configuration.
 *
 * <p>Allows requests only from the configured frontend origin (Vercel URL).
 * The {@code FRONTEND_ORIGIN} environment variable must be set to the
 * exact URL of the React frontend (e.g. {@code https://mail-analyzer.vercel.app}).
 *
 * <p>{@code allowCredentials = true} is required so that the browser
 * sends the HTTP-only session cookie ({@code JSESSIONID}) with each request.
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origin}")
    private String allowedOrigin;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow only the configured frontend origin – no wildcards when
        // credentials are involved.
        config.setAllowedOrigins(List.of(allowedOrigin));

        // Allow all standard HTTP methods used by our REST API
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Allow common headers + Content-Type for JSON requests
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));

        // Must be true so the browser sends the JSESSIONID cookie
        config.setAllowCredentials(true);

        // Cache preflight response for 1 hour
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
