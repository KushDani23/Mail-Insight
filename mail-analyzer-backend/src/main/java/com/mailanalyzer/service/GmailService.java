package com.mailanalyzer.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.mailanalyzer.entity.ConnectedAccount;
import com.mailanalyzer.entity.User;
import com.mailanalyzer.entity.UserSyncMetadata;
import com.mailanalyzer.exception.GmailException;
import com.mailanalyzer.repository.ConnectedAccountRepository;
import com.mailanalyzer.repository.UserSyncMetadataRepository;
import com.mailanalyzer.util.GmailMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import com.mailanalyzer.util.EncryptionUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.*;

/**
 * Service for all Gmail API interactions.
 *
 * <h3>Token Refresh Strategy</h3>
 * Before every Gmail API call, {@link #getValidAccessToken(ConnectedAccount)}
 * checks if the stored access token is expired.  If so, it automatically
 * calls Google's token endpoint with the stored refresh token to get a new
 * access token, then updates the {@link ConnectedAccount} record.
 *
 * <h3>New Email Detection</h3>
 * Two strategies are used:
 * <ol>
 *   <li><b>History API (fast)</b> – if a Gmail History ID was stored from
 *       the previous sync, we query only messages that arrived after that ID.</li>
 *   <li><b>Full scan + dedup (first sync)</b> – on first sync, we fetch the
 *       most recent {@code MAX_MESSAGES_FIRST_SYNC} messages and filter out
 *       already-analyzed ones via {@link com.mailanalyzer.repository.EmailRepository#existsByGmailMessageId(String)}.</li>
 * </ol>
 *
 * <h3>Privacy</h3>
 * The email body is decoded from base64url, included in the
 * {@link GmailMessageDto#getBody()} field for Gemini analysis, and then
 * discarded.  It is NEVER written to the database.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GmailService {

    private static final String USER_ID = "me";   // Gmail API "me" = authenticated user
    private static final int MAX_MESSAGES_FIRST_SYNC = 100;
    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";

    @Value("${app.gmail.application-name}")
    private String applicationName;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    private final ConnectedAccountRepository accountRepository;
    private final UserSyncMetadataRepository syncMetadataRepository;
    private final EncryptionUtil encryptionUtil;
    private final RestTemplate restTemplate;

    // ── Public API ───────────────────────────────────────────────────────

    /**
     * Counts how many new (un-analyzed) emails exist across all of this user's
     * connected Gmail accounts, without fetching full message bodies.
     *
     * <p>Used by GET /api/emails/new-count for the dashboard button state.
     *
     * @param user          the authenticated user
     * @param alreadySeenIds set of gmailMessageIds already stored in our DB
     * @return total count of new message IDs
     */
    @Transactional(readOnly = true)
    public int countNewMessages(User user, Set<String> alreadySeenIds) {
        List<ConnectedAccount> accounts = accountRepository.findAllByUser(user);
        int total = 0;

        for (ConnectedAccount account : accounts) {
            try {
                Gmail gmail = buildGmailService(account);
                List<String> messageIds = listRecentMessageIds(gmail, account, user);
                long newCount = messageIds.stream()
                        .filter(id -> !alreadySeenIds.contains(id))
                        .count();
                total += (int) newCount;
            } catch (Exception e) {
                log.warn("Failed to count new messages for account={}: {}", account.getGmailAddress(), e.getMessage());
                // Continue with other accounts rather than failing the entire request
            }
        }
        return total;
    }

    /**
     * Fetches the full content (headers + body) of all new emails across all
     * connected accounts.  Returns a list of {@link GmailMessageDto} objects
     * ready to be sent to Gemini for analysis.
     *
     * <p>The body field is populated for AI analysis but NEVER persisted.
     *
     * @param user          the authenticated user
     * @param alreadySeenIds set of gmailMessageIds already stored in our DB
     * @return list of new email DTOs
     */
    @Transactional
    public List<GmailMessageDto> fetchNewMessages(User user, Set<String> alreadySeenIds) {
        List<ConnectedAccount> accounts = accountRepository.findAllByUser(user);
        List<GmailMessageDto> allNew = new ArrayList<>();

        for (ConnectedAccount account : accounts) {
            try {
                List<GmailMessageDto> newForAccount = fetchNewMessagesForAccount(account, user, alreadySeenIds);
                allNew.addAll(newForAccount);
                log.info("Fetched {} new messages from account={}", newForAccount.size(), account.getGmailAddress());
            } catch (GmailException e) {
                throw e; // rethrow to be handled by GlobalExceptionHandler
            } catch (Exception e) {
                log.error("Error fetching messages for account={}: {}", account.getGmailAddress(), e.getMessage());
                throw new GmailException("Failed to fetch emails from " + account.getGmailAddress() + ": " + e.getMessage());
            }
        }
        return allNew;
    }

    /**
     * Updates the stored History ID for all accounts after a successful analysis.
     * Must be called by {@link EmailService} after emails have been saved.
     */
    @Transactional
    public void updateHistoryIds(User user) {
        List<ConnectedAccount> accounts = accountRepository.findAllByUser(user);
        for (ConnectedAccount account : accounts) {
            try {
                Gmail gmail = buildGmailService(account);
                String currentHistoryId = getLatestHistoryId(gmail);
                if (currentHistoryId != null) {
                    UserSyncMetadata meta = syncMetadataRepository.findByAccount(account)
                            .orElse(UserSyncMetadata.builder()
                                    .user(user)
                                    .account(account)
                                    .build());
                    meta.setHistoryId(currentHistoryId);
                    meta.setLastSyncedAt(Instant.now());
                    syncMetadataRepository.save(meta);
                }
            } catch (Exception e) {
                log.warn("Failed to update historyId for account={}: {}", account.getGmailAddress(), e.getMessage());
            }
        }
    }

    // ── Per-account fetching ─────────────────────────────────────────────

    private List<GmailMessageDto> fetchNewMessagesForAccount(ConnectedAccount account,
                                                              User user,
                                                              Set<String> alreadySeenIds)
            throws IOException, GeneralSecurityException {

        Gmail gmail = buildGmailService(account);
        List<String> newMessageIds = getNewMessageIds(gmail, account, user, alreadySeenIds);

        List<GmailMessageDto> result = new ArrayList<>();
        for (String messageId : newMessageIds) {
            try {
                Message message = gmail.users().messages()
                        .get(USER_ID, messageId)
                        .setFormat("FULL")
                        .execute();
                GmailMessageDto dto = parseMessage(message, account.getGmailAddress());
                if (dto != null) result.add(dto);
            } catch (IOException e) {
                log.warn("Failed to fetch message id={}: {}", messageId, e.getMessage());
                // Skip this message, continue with others
            }
        }
        return result;
    }

    private List<String> getNewMessageIds(Gmail gmail, ConnectedAccount account,
                                           User user, Set<String> alreadySeenIds)
            throws IOException {

        UserSyncMetadata syncMeta = syncMetadataRepository.findByAccount(account).orElse(null);

        if (syncMeta != null && syncMeta.getHistoryId() != null) {
            // Strategy 1: Incremental sync via Gmail History API
            try {
                return fetchViaHistoryApi(gmail, syncMeta.getHistoryId(), alreadySeenIds);
            } catch (IOException e) {
                log.warn("History API failed for account={}, falling back to full scan. Error: {}",
                        account.getGmailAddress(), e.getMessage());
                // Fall through to Strategy 2
            }
        }

        // Strategy 2: Full scan of recent messages with dedup
        return fetchViaFullScan(gmail, alreadySeenIds);
    }

    private List<String> fetchViaHistoryApi(Gmail gmail, String startHistoryId,
                                             Set<String> alreadySeenIds) throws IOException {
        List<String> newIds = new ArrayList<>();
        String pageToken = null;

        do {
            ListHistoryResponse historyResponse = gmail.users().history()
                    .list(USER_ID)
                    .setStartHistoryId(new java.math.BigInteger(startHistoryId))
                    .setHistoryTypes(List.of("messageAdded"))
                    .setPageToken(pageToken)
                    .execute();

            if (historyResponse.getHistory() != null) {
                for (History history : historyResponse.getHistory()) {
                    if (history.getMessagesAdded() != null) {
                        for (HistoryMessageAdded added : history.getMessagesAdded()) {
                            String msgId = added.getMessage().getId();
                            if (!alreadySeenIds.contains(msgId)) {
                                newIds.add(msgId);
                            }
                        }
                    }
                }
            }
            pageToken = historyResponse.getNextPageToken();
        } while (pageToken != null);

        return newIds;
    }

    private List<String> fetchViaFullScan(Gmail gmail, Set<String> alreadySeenIds) throws IOException {
        List<String> newIds = new ArrayList<>();

        ListMessagesResponse response = gmail.users().messages()
                .list(USER_ID)
                .setMaxResults((long) MAX_MESSAGES_FIRST_SYNC)
                .execute();

        if (response.getMessages() != null) {
            for (Message msg : response.getMessages()) {
                if (!alreadySeenIds.contains(msg.getId())) {
                    newIds.add(msg.getId());
                }
            }
        }
        return newIds;
    }

    private List<String> listRecentMessageIds(Gmail gmail, ConnectedAccount account, User user)
            throws IOException {
        UserSyncMetadata syncMeta = syncMetadataRepository.findByAccount(account).orElse(null);
        Set<String> emptySet = Collections.emptySet();
        if (syncMeta != null && syncMeta.getHistoryId() != null) {
            try {
                return fetchViaHistoryApi(gmail, syncMeta.getHistoryId(), emptySet);
            } catch (IOException e) {
                // Fall back to full scan
            }
        }
        return fetchViaFullScan(gmail, emptySet);
    }

    // ── Message Parsing ──────────────────────────────────────────────────

    private GmailMessageDto parseMessage(Message message, String sourceAccount) {
        if (message.getPayload() == null) return null;

        List<MessagePartHeader> headers = message.getPayload().getHeaders();
        String sender  = extractHeader(headers, "From");
        String subject = extractHeader(headers, "Subject");
        String dateStr = extractHeader(headers, "Date");

        Instant receivedAt = parseDate(message.getInternalDate());
        String body = extractBody(message.getPayload());

        // If body is completely empty, use the snippet as a fallback
        if (body == null || body.isBlank()) {
            body = message.getSnippet();
        }

        return GmailMessageDto.builder()
                .gmailMessageId(message.getId())
                .sourceAccount(sourceAccount)
                .sender(sender)
                .subject(subject != null ? subject : "(no subject)")
                .body(body != null ? body : "")
                .receivedAt(receivedAt)
                .build();
    }

    private String extractHeader(List<MessagePartHeader> headers, String name) {
        if (headers == null) return null;
        return headers.stream()
                .filter(h -> h.getName().equalsIgnoreCase(name))
                .map(MessagePartHeader::getValue)
                .findFirst()
                .orElse(null);
    }

    private Instant parseDate(Long internalDate) {
        if (internalDate == null) return Instant.now();
        return Instant.ofEpochMilli(internalDate);
    }

    /**
     * Extracts plain-text body from a Gmail MessagePart (supports multipart).
     * Decodes base64url encoding used by the Gmail API.
     */
    private String extractBody(MessagePart part) {
        if (part == null) return null;

        String mimeType = part.getMimeType();

        // Simple text/plain message
        if ("text/plain".equalsIgnoreCase(mimeType)) {
            return decodeBase64Url(part.getBody().getData());
        }

        // text/html fallback (strip tags simply, full HTML parser not worth it here)
        if ("text/html".equalsIgnoreCase(mimeType)) {
            String html = decodeBase64Url(part.getBody().getData());
            return html != null ? html.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim() : null;
        }

        // Multipart: recurse into parts, prefer text/plain
        if (mimeType != null && mimeType.startsWith("multipart/") && part.getParts() != null) {
            // First pass: look for text/plain
            for (MessagePart subPart : part.getParts()) {
                if ("text/plain".equalsIgnoreCase(subPart.getMimeType())) {
                    String text = decodeBase64Url(subPart.getBody().getData());
                    if (text != null && !text.isBlank()) return text;
                }
            }
            // Second pass: recurse into nested multipart
            for (MessagePart subPart : part.getParts()) {
                String nested = extractBody(subPart);
                if (nested != null && !nested.isBlank()) return nested;
            }
        }

        return null;
    }

    private String decodeBase64Url(String data) {
        if (data == null || data.isBlank()) return null;
        byte[] decoded = Base64.getUrlDecoder().decode(data);
        return new String(decoded, StandardCharsets.UTF_8);
    }

    // ── Gmail client construction ────────────────────────────────────────

    /**
     * Builds a Gmail API client using the account's access token.
     * Automatically refreshes the token if it is expired.
     */
    private Gmail buildGmailService(ConnectedAccount account)
            throws IOException, GeneralSecurityException {

        String accessToken = getValidAccessToken(account);

        AccessToken googleAccessToken = new AccessToken(accessToken, account.getTokenExpiresAt() != null
                ? Date.from(account.getTokenExpiresAt())
                : null);

        GoogleCredentials credentials = GoogleCredentials.create(googleAccessToken);

        return new Gmail.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials)
        ).setApplicationName(applicationName).build();
    }

    /**
     * Returns a valid (non-expired) access token.
     * If the stored token is expired, uses the refresh token to obtain a new one.
     */
    private String getValidAccessToken(ConnectedAccount account) {
        if (!account.isAccessTokenExpired()) {
            return encryptionUtil.decrypt(account.getAccessToken());
        }

        // Access token expired – use refresh token
        log.info("Access token expired for account={}, refreshing...", account.getGmailAddress());

        if (account.getRefreshToken() == null) {
            throw new GmailException(
                "No refresh token available for account " + account.getGmailAddress() +
                ". The user must reconnect this account."
            );
        }

        String decryptedRefresh = encryptionUtil.decrypt(account.getRefreshToken());
        return refreshAccessToken(account, decryptedRefresh);
    }

    /**
     * Calls Google's token endpoint to exchange a refresh token for a new access token.
     * Updates the {@link ConnectedAccount} with the new token and expiry.
     */
    private String refreshAccessToken(ConnectedAccount account, String refreshToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(TOKEN_ENDPOINT, request, Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new GmailException("Token refresh failed for account: " + account.getGmailAddress());
            }

            Map<?, ?> tokenData = response.getBody();
            String newAccessToken = (String) tokenData.get("access_token");
            Integer expiresIn = (Integer) tokenData.get("expires_in");

            Instant newExpiry = Instant.now().plusSeconds(expiresIn != null ? expiresIn : 3600);

            // Update stored tokens
            account.setAccessToken(encryptionUtil.encrypt(newAccessToken));
            account.setTokenExpiresAt(newExpiry);
            accountRepository.save(account);

            log.info("Successfully refreshed access token for account={}", account.getGmailAddress());
            return newAccessToken;

        } catch (GmailException e) {
            throw e;
        } catch (Exception e) {
            log.error("Token refresh error for account={}: {}", account.getGmailAddress(), e.getMessage());
            throw new GmailException("Token refresh failed: " + e.getMessage());
        }
    }

    private String getLatestHistoryId(Gmail gmail) {
        try {
            Profile profile = gmail.users().getProfile(USER_ID).execute();
            return profile.getHistoryId() != null ? profile.getHistoryId().toString() : null;
        } catch (IOException e) {
            log.warn("Could not fetch Gmail profile for historyId: {}", e.getMessage());
            return null;
        }
    }
}
