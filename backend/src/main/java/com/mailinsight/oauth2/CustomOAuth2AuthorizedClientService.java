package com.mailinsight.oauth2;

import com.mailinsight.entity.ConnectedAccount;
import com.mailinsight.entity.User;
import com.mailinsight.repository.ConnectedAccountRepository;
import com.mailinsight.repository.UserRepository;
import com.mailinsight.util.EncryptionUtil;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2AuthorizedClientService implements OAuth2AuthorizedClientService {

    private final ConnectedAccountRepository accountRepository;
    private final UserRepository userRepository;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final EncryptionUtil encryptionUtil;

    @Override
    @Transactional
    public void saveAuthorizedClient(OAuth2AuthorizedClient authorizedClient, Authentication principal) {
        if (!(principal instanceof OAuth2AuthenticationToken oauthToken)) {
            return;
        }

        String googleSub = principal.getName();
        User user = userRepository.findByGoogleSub(googleSub).orElse(null);

        if (user == null) {
            log.debug(
                    "saveAuthorizedClient: User not yet created for sub={}, skipping (will be handled by success handler)",
                    googleSub);
            return;
        }

        String gmailAddress = resolveGmailAddress(oauthToken);
        if (gmailAddress == null)
            return;

        saveOrUpdateTokens(user, gmailAddress, authorizedClient);
    }

    @Override
    @Transactional(readOnly = true)
    public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String clientRegistrationId,
            String principalName) {
        User user = userRepository.findByGoogleSub(principalName).orElse(null);
        if (user == null)
            return null;

        ConnectedAccount account = accountRepository.findFirstByUserOrderByCreatedAtAsc(user).orElse(null);
        if (account == null || account.getAccessToken() == null)
            return null;

        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId(clientRegistrationId);
        if (registration == null)
            return null;

        try {
            String decryptedAccess = encryptionUtil.decrypt(account.getAccessToken());
            OAuth2AccessToken accessToken = new OAuth2AccessToken(
                    OAuth2AccessToken.TokenType.BEARER,
                    decryptedAccess,
                    null,
                    account.getTokenExpiresAt());

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

    @Override
    @Transactional
    public void removeAuthorizedClient(String clientRegistrationId, String principalName) {
        userRepository.findByGoogleSub(principalName)
                .ifPresent(user -> accountRepository.findFirstByUserOrderByCreatedAtAsc(user).ifPresent(account -> {
                    account.setAccessToken(null);
                    account.setRefreshToken(null);
                    account.setTokenExpiresAt(null);
                    accountRepository.save(account);
                    log.info("Cleared OAuth2 tokens for user={}", user.getId());
                }));
    }

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

    private String resolveGmailAddress(OAuth2AuthenticationToken token) {
        if (token.getPrincipal() instanceof OidcUser oidcUser) {
            return oidcUser.getEmail();
        }
        return null;
    }
}
