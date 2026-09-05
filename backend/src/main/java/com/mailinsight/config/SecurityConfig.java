package com.mailinsight.config;

import com.mailinsight.oauth2.CustomOAuth2AuthorizedClientService;
import com.mailinsight.oauth2.OAuth2LoginSuccessHandler;
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
                .cors(cors -> cors.configure(http))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/login",
                                "/oauth2/**",
                                "/login/oauth2/code/**",
                                "/api/auth/logout",
                                "/error")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/accounts/oauth2/callback").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2
                        .authorizedClientService(authorizedClientService)
                        .successHandler(oAuth2LoginSuccessHandler)
                        .failureHandler(authFailureHandler()))
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/api/auth/logout", "GET"))
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((req, res, auth) -> res.setStatus(HttpServletResponse.SC_OK)))
                .securityContext(
                        context -> context.securityContextRepository(new HttpSessionSecurityContextRepository()));

        return http.build();
    }

    @Bean
    public AuthenticationFailureHandler authFailureHandler() {
        return (request, response, exception) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"OAuth2 authentication failed\"}");
        };
    }

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository) {

        OAuth2AuthorizedClientProvider authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
                .authorizationCode()
                .refreshToken()
                .build();

        var authorizedClientManager = new org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager(
                clientRegistrationRepository,
                new org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository());
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return authorizedClientManager;
    }
}
