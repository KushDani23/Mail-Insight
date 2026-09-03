package com.mailanalyzer.oauth2;

import com.mailanalyzer.entity.User;
import com.mailanalyzer.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Runs after a successful Google OAuth2 login.
 *
 * <p>Responsibilities:
 * <ol>
 *   <li>Create or update the {@link User} record in the database.</li>
 *   <li>Save the OAuth2 access + refresh tokens to the primary
 *       {@link com.mailanalyzer.entity.ConnectedAccount}.</li>
 *   <li>Redirect the browser to the frontend dashboard.</li>
 * </ol>
 *
 * <p>Token storage is delegated to
 * {@link CustomOAuth2AuthorizedClientService#saveOrUpdateTokens} so
 * the encryption logic lives in one place.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;
    private final CustomOAuth2AuthorizedClientService authorizedClientService;

    @Value("${app.cors.allowed-origin}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            response.sendRedirect(frontendUrl + "/dashboard");
            return;
        }

        OidcUser oidcUser = (OidcUser) oauthToken.getPrincipal();

        // Step 1: Create or update the User entity
        User user = userService.findOrCreateUser(oidcUser);
        log.info("OAuth2 login success for user: id={} email={}", user.getId(), user.getEmail());

        // Step 2: Retrieve the OAuth2AuthorizedClient (contains access + refresh tokens)
        //         It is stored in the HttpSession by Spring Security.
        Object clientAttr = request.getSession(false) != null
            ? request.getSession().getAttribute(
                org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository.class.getName() + ".AUTHORIZED_CLIENTS")
            : null;

        // Use a simpler approach: directly get the authorized client from the session repository
        // Spring Security has already stored the OAuth2AuthorizedClient before calling this handler
        var clientRepository = new org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository();
        OAuth2AuthorizedClient authorizedClient = clientRepository.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(),
                authentication,
                request
        );

        if (authorizedClient != null) {
            // Step 3: Save the tokens encrypted in ConnectedAccount
            authorizedClientService.saveOrUpdateTokens(user, oidcUser.getEmail(), authorizedClient);
        } else {
            log.warn("No OAuth2AuthorizedClient found after login for user={}", user.getId());
        }

        // Step 4: Redirect to frontend dashboard
        response.sendRedirect(frontendUrl + "/dashboard");
    }
}
