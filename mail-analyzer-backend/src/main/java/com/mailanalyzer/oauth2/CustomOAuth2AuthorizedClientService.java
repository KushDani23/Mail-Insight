package com.mailanalyzer.oauth2;

import com.mailanalyzer.entity.ConnectedAccount;
import com.mailanalyzer.entity.User;
import com.mailanalyzer.repository.ConnectedAccountRepository;
import com.mailanalyzer.repository.UserRepository;
import com.mailanalyzer.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA-backed implementation of {@link OAuth2AuthorizedClientService}.
 *
 * <p>Spring Security calls this service to:
 * <ol>
 *   <li>{@link #saveAuthorizedClient} – persist tokens after initial login
 *       or after a background token refresh.</li>
 *   <li>{@link #loadAuthorizedClient} – retrieve stored tokens when the
 *       Gmail API or token refresh needs them.</li>
 *   <li>{@link #removeAuthorizedClient} – clean up tokens on logout/disconnect.</li>
 * </ol>
 *
 * <p>All access tokens and refresh tokens are stored AES-256-GCM encrypted.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2AuthorizedClientService implements OAuth2AuthorizedClientService {

    private final ConnectedAccountRepository accountRepository;
    private final UserRepository userRepository;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final EncryptionUtil encryptionUtil;

    // ── saveAuthorizedClient ─────────────────────────────────────────────

    /**
     * Called by Spring Security after a successful login or token refresh.
     * Encrypts and persists the access + refresh tokens in the
     * {@link ConnectedAccount} row matching the authenticated user's email.
     */
    @Override
    @Transactional
    public void saveAuthorizedClient(OAuth2AuthorizedClient authorizedClient, Authentication principal) {
        if (!(principal instanceof OAuth2AuthenticationToken oauthToken)) {
            return; // not an OAuth2 flow, nothing to save
        }

        String googleSub = principal.getName(); // Google sub = Spring Security principal name
        User user = userRepository.findByGoogleSub(googleSub).orElse(null);

        // User may not exist yet on the very first login
        // (created by OAuth2LoginSuccessHandler which runs AFTER this call).
        // In that case, OAuth2LoginSuccessHandler handles the token save directly.
        if (user == null) {
            log.debug("saveAuthorizedClient: User not yet created for sub={}, skipping (will be handled by success handler)", googleSub);
            return;
        }

        String gmailAddress = resolveGmailAddress(oauthToken);
        if (gmailAddress == null) return;

        saveOrUpdateTokens(user, gmailAddress, authorizedClient);
    }

    // ── loadAuthorizedClient ─────────────────────────────────────────────

    /**
     * Called by Spring Security's token refresh mechanism when it needs the
     * stored client credentials for a given user.
     *
     * @param clientRegistrationId the OAuth2 registration ID (e.g. "google")
     * @param principalName        the user's Google sub
     */
    @Override
    @Transactional(readOnly = true)
    public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String clientRegistrationId,
                                                                      String principalName) {
        User user = userRepository.findByGoogleSub(principalName).orElse(null);
        if (user == null) return null;

        ConnectedAccount account = accountRepository.findFirstByUserOrderByCreatedAtAsc(user).orElse(null);
        if (account == null || account.getAccessToken() == null) return null;

        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId(clientRegistrationId);
        if (registration == null) return null;

        try {
            String decryptedAccess = encryptionUtil.decrypt(account.getAccessToken());
            OAuth2AccessToken accessToken = new OAuth2AccessToken(
                    OAuth2AccessToken.TokenType.BEARER,
                    decryptedAccess,
                    null,
                    account.getTokenExpiresAt()
            );

            OAuth2RefreshToken refreshToken = null;
            if (account.getRefreshToken() != null) {
                String decryptedRefresh = encryptionUtil.decrypt(account.getRefreshToken());
                refreshToken = new OAuth2RefreshToken(decryptedRefresh, null);
            }

            @SuppressWarnings("unchecked")
            T result = (T) new OAuth2AuthorizedClient(registration, principalName, accessToken, refreshToken);
            return result;

        } catch (Exception e) {
            log.error("Failed to decrypt tokens for user={}: {}", user.getId(), e.getMessage());
            return null;
        }
    }

    // ── removeAuthorizedClient ───────────────────────────────────────────

    /**
     * Called on logout. Clears the stored tokens for the given account.
     * The ConnectedAccount row is kept (so the user doesn't need to re-link),
     * but the tokens are nulled out.
     */
    @Override
    @Transactional
    public void removeAuthorizedClient(String clientRegistrationId, String principalName) {
        userRepository.findByGoogleSub(principalName).ifPresent(user ->
            accountRepository.findFirstByUserOrderByCreatedAtAsc(user).ifPresent(account -> {
                account.setAccessToken(null);
                account.setRefreshToken(null);
                account.setTokenExpiresAt(null);
                accountRepository.save(account);
                log.info("Cleared OAuth2 tokens for user={}", user.getId());
            })
        );
    }

    // ── Package-visible helper used by OAuth2LoginSuccessHandler ─────────

    /**
     * Encrypts and persists (or updates) OAuth2 tokens for the given user and Gmail address.
     * Called from both {@link #saveAuthorizedClient} and {@link OAuth2LoginSuccessHandler}.
     */
    @Transactional
    public void saveOrUpdateTokens(User user, String gmailAddress, OAuth2AuthorizedClient authorizedClient) {
        String encryptedAccess = encryptionUtil.encrypt(
                authorizedClient.getAccessToken().getTokenValue());

        String encryptedRefresh = null;
        if (authorizedClient.getRefreshToken() != null) {
            encryptedRefresh = encryptionUtil.encrypt(
                    authorizedClient.getRefreshToken().getTokenValue());
        }

        ConnectedAccount account = accountRepository
                .findByUserAndGmailAddress(user, gmailAddress)
                .orElse(ConnectedAccount.builder()
                        .user(user)
                        .gmailAddress(gmailAddress)
                        .build());

        account.setAccessToken(encryptedAccess);
        account.setRefreshToken(encryptedRefresh);
        account.setTokenExpiresAt(authorizedClient.getAccessToken().getExpiresAt());

        accountRepository.save(account);
        log.info("Saved/updated OAuth2 tokens for user={} gmail={}", user.getId(), gmailAddress);
    }

    // ── Private helpers ──────────────────────────────────────────────────

    private String resolveGmailAddress(OAuth2AuthenticationToken token) {
        if (token.getPrincipal() instanceof OidcUser oidcUser) {
            return oidcUser.getEmail();
        }
        return null;
    }
}
