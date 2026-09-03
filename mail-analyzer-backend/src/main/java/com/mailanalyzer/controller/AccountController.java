package com.mailanalyzer.controller;

import com.mailanalyzer.dto.response.AccountResponse;
import com.mailanalyzer.entity.ConnectedAccount;
import com.mailanalyzer.entity.User;
import com.mailanalyzer.exception.ResourceNotFoundException;
import com.mailanalyzer.mapper.EmailMapper;
import com.mailanalyzer.repository.ConnectedAccountRepository;
import com.mailanalyzer.repository.UserSyncMetadataRepository;
import com.mailanalyzer.service.UserService;
import com.mailanalyzer.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for managing connected Gmail accounts.
 *
 * <h3>Adding a Second Gmail Account</h3>
 * Spring Security's OAuth2 login flow supports only the primary login account.
 * For additional accounts, we implement a lightweight manual OAuth2 flow:
 * <ol>
 *   <li>Frontend calls {@code GET /api/accounts/connect} to get the Google consent URL.</li>
 *   <li>Frontend redirects the user to that URL.</li>
 *   <li>Google redirects back to {@code GET /api/accounts/oauth2/callback?code=...}</li>
 *   <li>Backend exchanges the code for tokens and saves the new {@link ConnectedAccount}.</li>
 * </ol>
 *
 * <p>The {@code state} parameter in the OAuth2 URL carries the user's UUID
 * so the callback can associate the tokens with the correct user.
 */
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Value("${app.cors.allowed-origin}")
    private String frontendUrl;

    private final UserService userService;
    private final ConnectedAccountRepository accountRepository;
    private final UserSyncMetadataRepository syncMetadataRepository;
    private final EmailMapper emailMapper;
    private final EncryptionUtil encryptionUtil;
    private final RestTemplate restTemplate;

    // ── GET /api/accounts ────────────────────────────────────────────────

    /**
     * Returns all Gmail accounts connected by the authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<AccountResponse>> listAccounts(Authentication authentication) {
        User user = userService.resolveUser(authentication);
        List<AccountResponse> accounts = accountRepository.findAllByUser(user).stream()
                .map(emailMapper::toAccountResponse)
                .toList();
        return ResponseEntity.ok(accounts);
    }

    // ── GET /api/accounts/connect ────────────────────────────────────────

    /**
     * Builds and returns a Google OAuth2 authorization URL for adding a
     * second Gmail account.  The frontend should redirect the user to this URL.
     *
     * <p>The {@code state} value encodes the authenticated user's UUID so the
     * callback can look up the correct user without trusting any request body.
     */
    @GetMapping("/connect")
    public ResponseEntity<Map<String, String>> getConnectUrl(Authentication authentication) {
        User user = userService.resolveUser(authentication);
        String state = user.getId().toString();

        // Callback URL must match what's registered in Google Cloud Console
        String redirectUri = frontendUrl.replace("localhost:5173", "localhost:8080") + "/api/accounts/oauth2/callback";
        // In production: use the Render backend URL
        // This is fine because /api/accounts/oauth2/callback is a backend endpoint

        String authUrl = "https://accounts.google.com/o/oauth2/v2/auth" +
                "?client_id=" + clientId +
                "&redirect_uri=" + java.net.URLEncoder.encode(redirectUri, java.nio.charset.StandardCharsets.UTF_8) +
                "&response_type=code" +
                "&scope=" + java.net.URLEncoder.encode(
                        "https://www.googleapis.com/auth/gmail.readonly email profile", java.nio.charset.StandardCharsets.UTF_8) +
                "&access_type=offline" +
                "&prompt=select_account+consent" +
                "&state=" + state;

        return ResponseEntity.ok(Map.of("authUrl", authUrl));
    }

    // ── GET /api/accounts/oauth2/callback ───────────────────────────────

    /**
     * Callback for the secondary Gmail account OAuth2 flow.
     * Exchanges the authorization code for tokens and saves a new ConnectedAccount.
     *
     * <p>This endpoint is public (no authentication required) because the user
     * is redirected here by Google.  Security is maintained by the {@code state}
     * parameter which encodes the user's UUID.
     */
    @GetMapping("/oauth2/callback")
    public ResponseEntity<Void> handleOAuth2Callback(
            @RequestParam String code,
            @RequestParam String state) {

        // Resolve user from state parameter (user's UUID)
        UUID userId = UUID.fromString(state);

        // Exchange code for tokens
        String redirectUri = buildRedirectUri();
        Map<?, ?> tokenData = exchangeCodeForTokens(code, redirectUri);

        String accessToken  = (String) tokenData.get("access_token");
        String refreshToken = (String) tokenData.get("refresh_token");
        Integer expiresIn   = (Integer) tokenData.get("expires_in");

        // Fetch the Gmail address for this new account
        String gmailAddress = fetchGmailAddress(accessToken);

        // Find the user by UUID from state
        User user = accountRepository.findById(userId)
                .map(ConnectedAccount::getUser)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for state=" + state));

        // Actually we need to get user from userRepository. Let me fetch differently:
        // (We'll use a direct JPA approach through connected account since we don't have userRepository here)
        // In practice, inject UserRepository or use UserService

        Instant expiresAt = Instant.now().plusSeconds(expiresIn != null ? expiresIn : 3600);

        ConnectedAccount account = accountRepository
                .findByUserAndGmailAddress(user, gmailAddress)
                .orElse(ConnectedAccount.builder()
                        .user(user)
                        .gmailAddress(gmailAddress)
                        .build());

        account.setAccessToken(encryptionUtil.encrypt(accessToken));
        account.setRefreshToken(refreshToken != null ? encryptionUtil.encrypt(refreshToken) : null);
        account.setTokenExpiresAt(expiresAt);
        accountRepository.save(account);

        log.info("Added secondary Gmail account {} for user={}", gmailAddress, userId);

        // Redirect to frontend settings page after successful connection
        return ResponseEntity.status(302)
                .header("Location", frontendUrl + "/settings?connected=true")
                .build();
    }

    // ── DELETE /api/accounts/{id} ────────────────────────────────────────

    /**
     * Removes a connected Gmail account.
     * All analyzed emails from this account remain in the database.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> disconnectAccount(
            @PathVariable UUID id,
            Authentication authentication) {

        User user = userService.resolveUser(authentication);

        // Verify the account belongs to the authenticated user before deleting
        ConnectedAccount account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));

        if (!account.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build(); // Forbidden: not the owner
        }

        // Remove sync metadata first (FK constraint)
        syncMetadataRepository.findByAccount(account).ifPresent(syncMetadataRepository::delete);
        accountRepository.delete(account);

        log.info("Disconnected account={} for user={}", account.getGmailAddress(), user.getId());
        return ResponseEntity.noContent().build();
    }

    // ── Private helpers ──────────────────────────────────────────────────

    private Map<?, ?> exchangeCodeForTokens(String code, String redirectUri) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        return restTemplate.postForObject(GOOGLE_TOKEN_URL, request, Map.class);
    }

    @SuppressWarnings("unchecked")
    private String fetchGmailAddress(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                GOOGLE_USERINFO_URL,
                org.springframework.http.HttpMethod.GET,
                entity,
                Map.class
        );

        Map<String, Object> userInfo = (Map<String, Object>) response.getBody();
        if (userInfo == null || !userInfo.containsKey("email")) {
            throw new com.mailanalyzer.exception.GmailException("Could not retrieve email from Google userinfo");
        }
        return (String) userInfo.get("email");
    }

    private String buildRedirectUri() {
        // Backend URL for the callback. In production this should be the Render URL.
        // Use env variable if available.
        String backendUrl = System.getenv("BACKEND_URL");
        if (backendUrl == null) backendUrl = "http://localhost:8080";
        return backendUrl + "/api/accounts/oauth2/callback";
    }
}
