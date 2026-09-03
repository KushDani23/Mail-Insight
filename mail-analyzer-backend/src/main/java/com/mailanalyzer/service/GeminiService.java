package com.mailanalyzer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailanalyzer.exception.GeminiException;
import com.mailanalyzer.util.AnalyzedEmailDto;
import com.mailanalyzer.util.GeminiResponseParser;
import com.mailanalyzer.util.GmailMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.google.genai.api.GoogleGenAiApi;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service that communicates with the Gemini API via Spring AI.
 *
 * <h3>Per-User API Key Design</h3>
 * Spring AI's auto-configuration is excluded (no global Gemini key).
 * Instead, each call to {@link #analyzeEmails} builds a fresh
 * {@link ChatClient} using the user's own decrypted Gemini API key.
 * This means:
 * <ul>
 *   <li>Users pay for their own Gemini usage.</li>
 *   <li>One user's quota exhaustion does not affect others.</li>
 *   <li>No API key is shared across users.</li>
 * </ul>
 *
 * <h3>Batch Processing</h3>
 * All new emails (regardless of the connected account count) are sent in
 * a SINGLE Gemini request.  The batch prompt asks Gemini to return a JSON
 * array, one object per email.  This is the core efficiency guarantee:
 * <ul>
 *   <li>100 new emails → 1 Gemini request</li>
 *   <li>0 new emails   → 0 Gemini requests</li>
 * </ul>
 *
 * <h3>Response Flow</h3>
 * Raw Gemini output → {@link GeminiResponseParser} validates structure,
 * categories, priorities → returns {@link AnalyzedEmailDto} list ready
 * for persistence.
 *
 * <p><b>Class-name note:</b> This service uses {@code GoogleGenAiChatModel},
 * {@code GoogleGenAiChatOptions}, and {@code GoogleGenAiApi} from the
 * {@code spring-ai-starter-model-google-genai} dependency (Spring AI 1.0.0).
 * If your build uses a different Spring AI version, verify these class names
 * in the {@code org.springframework.ai.google.genai} package.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    private static final String GEMINI_MODEL = "gemini-1.5-flash";
    private static final double TEMPERATURE  = 0.0;  // deterministic output for JSON

    private final GeminiResponseParser responseParser;
    private final ObjectMapper objectMapper;

    // ── Public API ───────────────────────────────────────────────────────

    /**
     * Analyzes a batch of emails using the provided Gemini API key.
     *
     * <p>Builds one prompt containing all emails as a JSON array, sends it
     * to Gemini via Spring AI's {@code ChatClient}, then delegates parsing
     * and validation to {@link GeminiResponseParser}.
     *
     * @param decryptedApiKey the user's Gemini API key (already decrypted, NOT stored)
     * @param emails          list of new emails to analyze (must be non-empty)
     * @return list of validated {@link AnalyzedEmailDto} objects
     * @throws GeminiException if Gemini fails or returns an invalid response
     */
    public List<AnalyzedEmailDto> analyzeEmails(String decryptedApiKey, List<GmailMessageDto> emails) {
        if (emails.isEmpty()) {
            throw new IllegalArgumentException("Cannot call Gemini with an empty email list");
        }

        log.info("Sending {} emails to Gemini (model={})", emails.size(), GEMINI_MODEL);

        ChatClient client = buildChatClient(decryptedApiKey);
        String prompt = buildBatchPrompt(emails);

        String rawResponse;
        try {
            rawResponse = client.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage());
            throw new GeminiException("Gemini API request failed: " + e.getMessage());
        }

        if (rawResponse == null || rawResponse.isBlank()) {
            throw new GeminiException("Gemini returned an empty response");
        }

        log.debug("Gemini raw response length: {} chars", rawResponse.length());

        // Validate and parse the response
        List<String> sentIds = emails.stream().map(GmailMessageDto::getGmailMessageId).toList();
        return responseParser.parseAndValidate(rawResponse, sentIds);
    }

    // ── Private helpers ──────────────────────────────────────────────────

    /**
     * Programmatically constructs a {@link ChatClient} with the user's own API key.
     *
     * <p>This bypasses Spring AI's auto-configured global ChatClient bean and
     * creates a dedicated client instance for this specific API request.
     */
    private ChatClient buildChatClient(String apiKey) {
        GoogleGenAiApi genAiApi = new GoogleGenAiApi(apiKey);

        GoogleGenAiChatOptions options = GoogleGenAiChatOptions.builder()
                .model(GEMINI_MODEL)
                .temperature(TEMPERATURE)
                .build();

        GoogleGenAiChatModel chatModel = new GoogleGenAiChatModel(genAiApi, options);

        return ChatClient.builder(chatModel).build();
    }

    /**
     * Builds the batch analysis prompt.
     *
     * <p>The prompt instructs Gemini to return ONLY a JSON array (no markdown,
     * no explanation) with one object per email, matching the exact categories
     * and priority scale defined in the system specification.
     */
    private String buildBatchPrompt(List<GmailMessageDto> emails) {
        String emailsJson;
        try {
            List<Object> emailArray = emails.stream().map(e -> new java.util.LinkedHashMap<String, Object>() {{
                put("id", e.getGmailMessageId());
                put("from", e.getSender());
                put("subject", e.getSubject());
                put("body", truncateBody(e.getBody()));
            }}).collect(Collectors.toList());
            emailsJson = objectMapper.writeValueAsString(emailArray);
        } catch (Exception ex) {
            throw new GeminiException("Failed to serialize emails for Gemini prompt: " + ex.getMessage());
        }

        return """
            You are an email categorization assistant. Analyze the following emails and return a JSON array.
            
            RULES:
            1. Return ONLY a valid JSON array. No markdown, no code blocks, no explanation.
            2. Each element must have exactly these fields:
               - "gmail_message_id": the exact same "id" value from the input
               - "summary": 1-2 sentence plain-text summary of the email's key information
               - "category": exactly one of the categories listed below
               - "priority": integer 1-5 matching the category's priority group
            
            CATEGORIES AND PRIORITY:
            Priority 1: CAREER_OPPORTUNITIES, APPLICATION_UPDATES, INTERVIEW_INVITATIONS, CODING_ASSESSMENTS, BANKING_AND_PAYMENTS, SECURITY_ALERTS, COLLEGE_AND_ACADEMICS
            Priority 2: LEARNING_PLATFORMS, CERTIFICATIONS, CODING_PLATFORMS, HACKATHONS, OPEN_SOURCE
            Priority 3: BLOGS, NEWSLETTERS, NEWS_FEEDS, VIDEO_NOTIFICATIONS, WEEKLY_DIGESTS, COMMUNITY_UPDATES
            Priority 4: PROMOTIONS, MARKETING, SPAM, GENERAL_UNIVERSITY
            Priority 5: COMMUNITY_ACTIVITIES, EVENT_INVITATIONS
            
            NOTE: Emails from @dau.ac.in about exams, notices, academics → COLLEGE_AND_ACADEMICS (priority 1).
            Dance/Music/Photography/Drama/Sports club emails → COMMUNITY_ACTIVITIES (priority 5).
            
            EXAMPLE OUTPUT FORMAT:
            [
              {
                "gmail_message_id": "abc123",
                "summary": "LinkedIn sent a Software Engineer internship at Google with application deadline Sep 30.",
                "category": "CAREER_OPPORTUNITIES",
                "priority": 1
              }
            ]
            
            EMAILS TO ANALYZE:
            """ + emailsJson;
    }

    /**
     * Truncates email body to avoid hitting Gemini's context limit.
     * 1500 chars is enough for meaningful analysis without burning tokens.
     */
    private String truncateBody(String body) {
        if (body == null) return "";
        return body.length() > 1500 ? body.substring(0, 1500) + "..." : body;
    }
}
