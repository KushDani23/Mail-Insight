package com.mailinsight.oauth2;

import com.mailinsight.entity.User;
import com.mailinsight.service.UserService;
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

        // Step 2: Retrieve the OAuth2AuthorizedClient (contains access + refreshtokens)
        Object clientAttr = request.getSession(false) != null
                ? request.getSession().getAttribute(
                        org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository.class
                                .getName() + ".AUTHORIZED_CLIENTS")
                : null;

        var clientRepository = new org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository();
        OAuth2AuthorizedClient authorizedClient = clientRepository.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(),
                authentication,
                request);

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
