package com.mailinsight.service;

import com.mailinsight.dto.response.AnalyzeResultResponse;
import com.mailinsight.dto.response.DashboardStatsResponse;
import com.mailinsight.dto.response.NewEmailCountResponse;
import com.mailinsight.entity.Email;
import com.mailinsight.entity.User;
import com.mailinsight.enums.EmailCategory;
import com.mailinsight.enums.EmailPriority;
import com.mailinsight.repository.ConnectedAccountRepository;
import com.mailinsight.repository.EmailRepository;
import com.mailinsight.util.AnalyzedEmailDto;
import com.mailinsight.util.GmailMessageDto;
import com.mailinsight.exception.InsufficientEmailsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final EmailRepository emailRepository;
    private final ConnectedAccountRepository accountRepository;
    private final GmailService gmailService;
    private final GeminiService geminiService;
    private final AiKeyService aiKeyService;

    @Transactional(readOnly = true)
    public NewEmailCountResponse getNewEmailCount(User user) {
        Set<String> alreadySeenIds = getAllStoredGmailMessageIds(user);
        int newCount = gmailService.countNewMessages(user, alreadySeenIds);

        log.info("User={} has {} new emails (alreadySeen={})", user.getId(), newCount, alreadySeenIds.size());
        return NewEmailCountResponse.of(newCount);
    }

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

        // Guard: enforce minimum batch size to respect Google AI Studio rate limits
        final int MINIMUM_EMAILS_REQUIRED = 10;
        if (newEmails.size() < MINIMUM_EMAILS_REQUIRED) {
            log.info("Analysis blocked for user={}: only {}/{} new emails",
                    user.getId(), newEmails.size(), MINIMUM_EMAILS_REQUIRED);
            throw new InsufficientEmailsException(newEmails.size(), MINIMUM_EMAILS_REQUIRED);
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

    @Transactional(readOnly = true)
    public Page<Email> getEmails(User user, Pageable pageable) {
        return emailRepository.findAllByUserOrderByReceivedAtDesc(user, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Email> getEmailsByCategory(User user, EmailCategory category, Pageable pageable) {
        return emailRepository.findAllByUserAndCategoryOrderByReceivedAtDesc(user, category, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Email> getEmailsByPriority(User user, EmailPriority priority, Pageable pageable) {
        return emailRepository.findAllByUserAndPriorityOrderByReceivedAtDesc(user, priority, pageable);
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats(User user) {
        long total = emailRepository.countByUser(user);

        Map<EmailCategory, Long> byCategory = emailRepository.countByCategoryForUser(user)
                .stream()
                .collect(Collectors.toMap(
                        row -> (EmailCategory) row[0],
                        row -> (Long) row[1],
                        Long::sum,
                        LinkedHashMap::new));

        Map<EmailPriority, Long> byPriority = emailRepository.countByPriorityForUser(user)
                .stream()
                .collect(Collectors.toMap(
                        row -> (EmailPriority) row[0],
                        row -> (Long) row[1],
                        Long::sum,
                        LinkedHashMap::new));

        long accountsCount = accountRepository.countByUser(user);

        return new DashboardStatsResponse(total, byCategory, byPriority, accountsCount);
    }

    private Set<String> getAllStoredGmailMessageIds(User user) {
        return emailRepository.findAllByUserOrderByReceivedAtDesc(user, Pageable.unpaged())
                .stream()
                .map(Email::getGmailMessageId)
                .collect(Collectors.toSet());
    }

    private int persistAnalyzedEmails(User user,
            List<GmailMessageDto> rawEmails,
            List<AnalyzedEmailDto> analyzed) {

        Map<String, GmailMessageDto> rawById = rawEmails.stream()
                .collect(Collectors.toMap(GmailMessageDto::getGmailMessageId, e -> e));

        int saved = 0;

        for (AnalyzedEmailDto dto : analyzed) {
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
                    .summary(dto.getSummary())
                    .category(dto.getCategory())
                    .priority(dto.getPriority())
                    .receivedAt(raw.getReceivedAt())
                    .build();
            emailRepository.save(email);
            saved++;
        }
        return saved;
    }
}
