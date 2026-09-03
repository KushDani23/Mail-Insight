package com.mailanalyzer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Miscellaneous application beans.
 *
 * <h3>Spring AI / Gemini</h3>
 * Spring AI's Google GenAI auto-configuration is excluded via
 * {@code spring.autoconfigure.exclude} in application.properties.
 * This means there is NO global ChatClient bean.
 *
 * <p>{@link com.mailanalyzer.service.GeminiService} creates a new
 * {@code ChatClient} per-request using each user's own Gemini API key.
 * This is intentional: users own and consume their own keys.
 *
 * <h3>RestTemplate</h3>
 * A shared {@link RestTemplate} bean is used for:
 * <ul>
 *   <li>OAuth2 access token refresh (Google token endpoint)</li>
 *   <li>Secondary Gmail account OAuth2 code exchange</li>
 *   <li>Google userinfo endpoint calls</li>
 * </ul>
 */
@Configuration
public class GeminiConfig {

    /**
     * Shared RestTemplate for outbound HTTP calls.
     * Not used for Gemini (Spring AI handles that via ChatClient).
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
