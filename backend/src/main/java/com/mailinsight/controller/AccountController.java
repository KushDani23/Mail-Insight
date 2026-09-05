package com.mailinsight.controller;

import com.mailinsight.dto.response.AccountResponse;
import com.mailinsight.entity.ConnectedAccount;
import com.mailinsight.entity.User;
import com.mailinsight.exception.ResourceNotFoundException;
import com.mailinsight.mapper.EmailMapper;
import com.mailinsight.repository.ConnectedAccountRepository;
import com.mailinsight.repository.UserSyncMetadataRepository;
import com.mailinsight.service.UserService;
import com.mailinsight.util.EncryptionUtil;
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

    @GetMapping
    public ResponseEntity<List<AccountResponse>> listAccounts(Authentication authentication) {
        User user = userService.resolveUser(authentication);
        List<AccountResponse> accounts = accountRepository.findAllByUser(user).stream()
                .map(emailMapper::toAccountResponse)
                .toList();
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/connect")
    public ResponseEntity<Map<String, String>> getConnectUrl(Authentication authentication) {
        User user = userService.resolveUser(authentication);
        String state = user.getId().toString();

        String redirectUri = frontendUrl.replace("localhost:5173", "localhost:8080") + "/api/accounts/oauth2/callback";
        String authUrl = "https://accounts.google.com/o/oauth2/v2/auth" +
                "?client_id=" + clientId +
                "&redirect_uri=" + java.net.URLEncoder.encode(redirectUri, java.nio.charset.StandardCharsets.UTF_8) +
                "&response_type=code" +
                "&scope=" + java.net.URLEncoder.encode(
                        "https://www.googleapis.com/auth/gmail.readonly email profile",
                        java.nio.charset.StandardCharsets.UTF_8)
                +
                "&access_type=offline" +
                "&prompt=select_account+consent" +
                "&state=" + state;

        return ResponseEntity.ok(Map.of("authUrl", authUrl));
    }

    @GetMapping("/oauth2/callback")
    public ResponseEntity<Void> handleOAuth2Callback(
            @RequestParam String code,
            @RequestParam String state) {

        UUID userId = UUID.fromString(state);
        String redirectUri = buildRedirectUri();
        Map<?, ?> tokenData = exchangeCodeForTokens(code, redirectUri);

        String accessToken = (String) tokenData.get("access_token");
        String refreshToken = (String) tokenData.get("refresh_token");
        Integer expiresIn = (Integer) tokenData.get("expires_in");
        String gmailAddress = fetchGmailAddress(accessToken);

        User user = accountRepository.findById(userId)
                .map(ConnectedAccount::getUser)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for state=" + state));

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

        return ResponseEntity.status(302)
                .header("Location", frontendUrl + "/settings?connected=true")
                .build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> disconnectAccount(
            @PathVariable UUID id,
            Authentication authentication) {

        User user = userService.resolveUser(authentication);

        ConnectedAccount account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));

        if (!account.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        syncMetadataRepository.findByAccount(account).ifPresent(syncMetadataRepository::delete);
        accountRepository.delete(account);

        log.info("Disconnected account={} for user={}", account.getGmailAddress(), user.getId());
        return ResponseEntity.noContent().build();
    }

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
                Map.class);

        Map<String, Object> userInfo = (Map<String, Object>) response.getBody();
        if (userInfo == null || !userInfo.containsKey("email")) {
            throw new com.mailinsight.exception.GmailException("Could not retrieve email from Google userinfo");
        }
        return (String) userInfo.get("email");
    }

    private String buildRedirectUri() {
        String backendUrl = System.getenv("BACKEND_URL");
        if (backendUrl == null)
            backendUrl = "http://localhost:8080";
        return backendUrl + "/api/accounts/oauth2/callback";
    }
}
