package com.mailanalyzer.service;

import com.mailanalyzer.dto.response.AnalyzeResultResponse;
import com.mailanalyzer.dto.response.DashboardStatsResponse;
import com.mailanalyzer.dto.response.NewEmailCountResponse;
import com.mailanalyzer.entity.Email;
import com.mailanalyzer.entity.User;
import com.mailanalyzer.enums.EmailCategory;
import com.mailanalyzer.enums.EmailPriority;
import com.mailanalyzer.repository.ConnectedAccountRepository;
import com.mailanalyzer.repository.EmailRepository;
import com.mailanalyzer.util.AnalyzedEmailDto;
import com.mailanalyzer.util.GmailMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Core orchestrator for the email analysis pipeline.
 *
 * <h3>Pipeline (POST /api/emails/analyze)</h3>
 * <pre>
 *   GmailService.fetchNewMessages()      → raw emails (body in memory only)
 *        ↓
 *   GeminiService.analyzeEmails()         → single batch prompt → Gemini
 *        ↓
 *   GeminiResponseParser.parseAndValidate → validated DTOs
 *        ↓
 *   EmailService.persistAnalyzedEmails()  → store metadata + summary only
 *        ↓
 *   GmailService.updateHistoryIds()       → save sync checkpoint
 * </pre>
 *
 * <h3>Efficiency guarantees</h3>
 * <ul>
 *   <li>0 new emails → 0 Gemini requests (early return)</li>
 *   <li>N new emails → 1 Gemini request (batched)</li>
 *   <li>Already-analyzed emails are never re-analyzed ({@code existsByGmailMessageId})</li>
 * </ul>
 *
 * <h3>User isolation</h3>
 * Every method receives the {@link User} entity resolved from Spring Security.
 * Every repository query filters by {@code user_id}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final EmailRepository emailRepository;
    private final ConnectedAccountRepository accountRepository;
    private final GmailService gmailService;
    private final GeminiService geminiService;
    private final AiKeyService aiKeyService;

    // ── New Email Count ──────────────────────────────────────────────────

    /**
     * Counts new (un-analyzed) emails across all connected Gmail accounts.
     *
     * <p>If count == 0, the frontend disables the "Analyze" button and no
     * Gemini request is ever made.
     */
    @Transactional(readOnly = true)
    public NewEmailCountResponse getNewEmailCount(User user) {
        Set<String> alreadySeenIds = getAllStoredGmailMessageIds(user);
        int newCount = gmailService.countNewMessages(user, alreadySeenIds);

        log.info("User={} has {} new emails (alreadySeen={})", user.getId(), newCount, alreadySeenIds.size());
        return NewEmailCountResponse.of(newCount);
    }

    // ── Analyze Pipeline ─────────────────────────────────────────────────

    /**
     * Full analysis pipeline: fetch → batch → Gemini → validate → persist.
     *
     * @param user the authenticated user
     * @return result containing count of analyzed and skipped emails
     */
    @Transactional
    public AnalyzeResultResponse analyzeEmails(User user) {
        // Step 1: Determine which emails are already analyzed
        Set<String> alreadySeenIds = getAllStoredGmailMessageIds(user);

        // Step 2: Fetch new raw emails from Gmail (body is in memory only)
        List<GmailMessageDto> newEmails = gmailService.fetchNewMessages(user, alreadySeenIds);

        if (newEmails.isEmpty()) {
            log.info("No new emails to analyze for user={}", user.getId());
            return new AnalyzeResultResponse(0, 0, "No new emails found.");
        }

        log.info("Fetched {} new emails for user={}, sending to Gemini...", newEmails.size(), user.getId());

        // Step 3: Get user's Gemini API key (decrypted, in memory only)
        String apiKey = aiKeyService.getDecryptedKey(user);

        // Step 4: Single batch Gemini request (N emails → 1 request)
        List<AnalyzedEmailDto> analyzed = geminiService.analyzeEmails(apiKey, newEmails);

        // Step 5: Persist validated results (metadata + summary only, no body)
        int saved = persistAnalyzedEmails(user, newEmails, analyzed);
        int skipped = newEmails.size() - saved;

        // Step 6: Update Gmail History IDs for next incremental sync
        gmailService.updateHistoryIds(user);

        log.info("Analysis complete for user={}: {} analyzed, {} skipped", user.getId(), saved, skipped);
        return new AnalyzeResultResponse(saved, skipped,
                "Successfully analyzed " + saved + " emails.");
    }

    // ── Email Queries (Dashboard) ────────────────────────────────────────

    /**
     * Returns paginated emails for the user (all categories).
     */
    @Transactional(readOnly = true)
    public Page<Email> getEmails(User user, Pageable pageable) {
        return emailRepository.findAllByUserOrderByReceivedAtDesc(user, pageable);
    }

    /**
     * Returns paginated emails for the user filtered by category.
     */
    @Transactional(readOnly = true)
    public Page<Email> getEmailsByCategory(User user, EmailCategory category, Pageable pageable) {
        return emailRepository.findAllByUserAndCategoryOrderByReceivedAtDesc(user, category, pageable);
    }

    /**
     * Returns paginated emails for the user filtered by priority.
     */
    @Transactional(readOnly = true)
    public Page<Email> getEmailsByPriority(User user, EmailPriority priority, Pageable pageable) {
        return emailRepository.findAllByUserAndPriorityOrderByReceivedAtDesc(user, priority, pageable);
    }

    /**
     * Returns dashboard statistics: total emails, count per category, count per priority.
     */
    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats(User user) {
        long total = emailRepository.countByUser(user);

        // Count per category → Map<EmailCategory, Long>
        Map<EmailCategory, Long> byCategory = emailRepository.countByCategoryForUser(user)
                .stream()
                .collect(Collectors.toMap(
                        row -> (EmailCategory) row[0],
                        row -> (Long) row[1],
                        Long::sum,
                        LinkedHashMap::new
                ));

        // Count per priority → Map<EmailPriority, Long>
        Map<EmailPriority, Long> byPriority = emailRepository.countByPriorityForUser(user)
                .stream()
                .collect(Collectors.toMap(
                        row -> (EmailPriority) row[0],
                        row -> (Long) row[1],
                        Long::sum,
                        LinkedHashMap::new
                ));

        long accountsCount = accountRepository.countByUser(user);

        return new DashboardStatsResponse(total, byCategory, byPriority, accountsCount);
    }

    // ── Private helpers ──────────────────────────────────────────────────

    /**
     * Returns all gmailMessageIds already stored for this user.
     * Used to avoid re-analyzing or re-fetching already-processed emails.
     */
    private Set<String> getAllStoredGmailMessageIds(User user) {
        // Fetch all pages (OK because we only need the IDs, not full entities)
        return emailRepository.findAllByUserOrderByReceivedAtDesc(user, Pageable.unpaged())
                .stream()
                .map(Email::getGmailMessageId)
                .collect(Collectors.toSet());
    }

    /**
     * Persists validated analysis results as Email entities.
     *
     * <p>For each {@link AnalyzedEmailDto}, we find the matching
     * {@link GmailMessageDto} (by gmailMessageId) to get the sender,
     * subject, receivedAt, and sourceAccount.  The email body from the
     * GmailMessageDto is intentionally NOT stored.
     *
     * @return number of emails successfully saved
     */
    private int persistAnalyzedEmails(User user,
                                       List<GmailMessageDto> rawEmails,
                                       List<AnalyzedEmailDto> analyzed) {

        // Index raw emails by gmailMessageId for O(1) lookup
        Map<String, GmailMessageDto> rawById = rawEmails.stream()
                .collect(Collectors.toMap(GmailMessageDto::getGmailMessageId, e -> e));

        int saved = 0;

        for (AnalyzedEmailDto dto : analyzed) {
            // Final dedup check: skip if somehow already in DB
            if (emailRepository.existsByGmailMessageId(dto.getGmailMessageId())) {
                log.debug("Skipping duplicate gmailMessageId={}", dto.getGmailMessageId());
                continue;
            }

            GmailMessageDto raw = rawById.get(dto.getGmailMessageId());
            if (raw == null) {
                log.warn("No raw email found for gmailMessageId={}, skipping", dto.getGmailMessageId());
                continue;
            }

            Email email = Email.builder()
                    .user(user)
                    .gmailMessageId(dto.getGmailMessageId())
                    .sourceAccount(raw.getSourceAccount())
                    .sender(raw.getSender())
                    .subject(raw.getSubject())
                    .summary(dto.getSummary())          // AI-generated
                    .category(dto.getCategory())         // validated enum
                    .priority(dto.getPriority())         // validated 1–5
                    .receivedAt(raw.getReceivedAt())
                    .build();
            // NOTE: raw.getBody() is intentionally NOT stored anywhere.

            emailRepository.save(email);
            saved++;
        }

        return saved;
    }
}
