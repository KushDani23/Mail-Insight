package com.mailanalyzer.config;

import com.mailanalyzer.oauth2.CustomOAuth2AuthorizedClientService;
import com.mailanalyzer.oauth2.OAuth2LoginSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Central Spring Security configuration.
 *
 * <h3>Session Strategy</h3>
 * We use HTTP-only session cookies.  No JWTs.  Spring Security stores the
 * authenticated principal in the server-side HttpSession.  The session ID
 * is sent to the browser as a {@code JSESSIONID} HTTP-only cookie.
 *
 * <h3>User Isolation</h3>
 * The authenticated user is always extracted from
 * {@code SecurityContextHolder.getContext().getAuthentication()} inside
 * each service.  The frontend NEVER passes a user_id in request bodies.
 *
 * <h3>CSRF</h3>
 * CSRF is disabled for REST API endpoints because:
 * <ul>
 *   <li>The frontend is a separate origin (Vercel) and uses CORS.</li>
 *   <li>SameSite=None cookies are used, which provides some CSRF protection.</li>
 *   <li>Custom {@code Origin} header validation via CORS acts as a CSRF guard.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final CustomOAuth2AuthorizedClientService authorizedClientService;

    public SecurityConfig(OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
                          CustomOAuth2AuthorizedClientService authorizedClientService) {
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
        this.authorizedClientService = authorizedClientService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ── CORS: delegate to CorsConfig ─────────────────────────────────
            .cors(cors -> cors.configure(http))

            // ── CSRF: disabled for REST API ───────────────────────────────────
            .csrf(AbstractHttpConfigurer::disable)

            // ── Authorization Rules ───────────────────────────────────────────
            .authorizeHttpRequests(auth -> auth
                // Public endpoints – no authentication required
                .requestMatchers(
                    "/login",
                    "/oauth2/**",
                    "/login/oauth2/code/**",
                    "/api/auth/logout",
                    "/error"
                ).permitAll()
                // Account OAuth2 callback for secondary Gmail accounts
                .requestMatchers(HttpMethod.GET, "/api/accounts/oauth2/callback").permitAll()
                // All other API endpoints require authentication
                .requestMatchers("/api/**").authenticated()
                .anyRequest().authenticated()
            )

            // ── OAuth2 Login ──────────────────────────────────────────────────
            .oauth2Login(oauth2 -> oauth2
                .authorizedClientService(authorizedClientService)
                .successHandler(oAuth2LoginSuccessHandler)
                .failureHandler(authFailureHandler())
            )

            // ── Logout ────────────────────────────────────────────────────────
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/api/auth/logout", "GET"))
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler((req, res, auth) -> res.setStatus(HttpServletResponse.SC_OK))
            )

            // ── Session Management ────────────────────────────────────────────
            .securityContext(context ->
                context.securityContextRepository(new HttpSessionSecurityContextRepository())
            );

        return http.build();
    }

    /**
     * Returns 401 instead of redirecting to login on auth failure.
     * Prevents the React SPA from receiving an HTML redirect page.
     */
    @Bean
    public AuthenticationFailureHandler authFailureHandler() {
        return (request, response, exception) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"OAuth2 authentication failed\"}");
        };
    }

    /**
     * Configures the OAuth2AuthorizedClientManager with automatic token refresh.
     *
     * <p>When an access token expires, Spring Security will automatically use
     * the stored refresh token to obtain a new access token via the
     * {@code refreshToken()} provider – no manual intervention required.
     */
    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository) {

        OAuth2AuthorizedClientProvider authorizedClientProvider =
            OAuth2AuthorizedClientProviderBuilder.builder()
                .authorizationCode()
                .refreshToken()    // <-- enables auto-refresh of expired tokens
                .build();

        var authorizedClientManager = new org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager(
            clientRegistrationRepository,
            new org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository()
        );
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return authorizedClientManager;
    }
}
