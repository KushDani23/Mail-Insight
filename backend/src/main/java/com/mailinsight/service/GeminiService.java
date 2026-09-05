package com.mailinsight.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailinsight.exception.GeminiException;
import com.mailinsight.util.AnalyzedEmailDto;
import com.mailinsight.util.GeminiResponseParser;
import com.mailinsight.util.GmailMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key=";
    private final GeminiResponseParser responseParser;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public List<AnalyzedEmailDto> analyzeEmails(String decryptedApiKey, List<GmailMessageDto> emails) {
        if (emails.isEmpty()) {
            throw new IllegalArgumentException("Cannot call Gemini with an empty email list");
        }

        log.info("Sending {} emails to Gemini (gemini-3.6-flash)", emails.size());

        String prompt = buildBatchPrompt(emails);
        String rawResponse = callGeminiApi(decryptedApiKey, prompt);

        if (rawResponse == null || rawResponse.isBlank()) {
            throw new GeminiException("Gemini returned an empty response");
        }

        List<String> sentIds = emails.stream()
                .map(GmailMessageDto::getGmailMessageId)
                .toList();
        return responseParser.parseAndValidate(rawResponse, sentIds);
    }

    @SuppressWarnings("unchecked")
    private String callGeminiApi(String apiKey, String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Gemini REST API request body
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "temperature", 0.0,
                        "responseMimeType", "application/json"));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        String url = GEMINI_API_URL + apiKey;

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new GeminiException("Gemini API returned non-2xx status: " + response.getStatusCode());
            }

            // Navigate: body → candidates[0] → content → parts[0] → text
            Map<String, Object> body = response.getBody();
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");

            if (candidates == null || candidates.isEmpty()) {
                throw new GeminiException("Gemini response contained no candidates");
            }

            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");

            if (parts == null || parts.isEmpty()) {
                throw new GeminiException("Gemini response candidate had no parts");
            }

            return (String) parts.get(0).get("text");

        } catch (GeminiException e) {
            throw e;
        } catch (HttpClientErrorException e) {
            log.error("Gemini API HTTP error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 400) {
                throw new GeminiException("Invalid Gemini API key or request. Please check your API key in Settings.");
            }
            if (e.getStatusCode().value() == 429) {
                throw new GeminiException("Gemini API quota exceeded. Please try again later.");
            }
            throw new GeminiException("Gemini API error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected Gemini API call failure: {}", e.getMessage());
            throw new GeminiException("Gemini API request failed: " + e.getMessage());
        }
    }

    private String buildBatchPrompt(List<GmailMessageDto> emails) {
        String emailsJson;
        try {
            List<Object> emailArray = emails.stream().map(e -> {
                java.util.LinkedHashMap<String, Object> map = new java.util.LinkedHashMap<>();
                map.put("id", e.getGmailMessageId());
                map.put("from", e.getSender());
                map.put("subject", e.getSubject());
                map.put("body", truncateBody(e.getBody()));
                return (Object) map;
            }).collect(Collectors.toList());
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
                   - "summary": 1-2 sentence plain-text summary of the email key information
                   - "category": exactly one of the categories listed below
                   - "priority": integer 1-5 matching the category priority group

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
                """
                + emailsJson;
    }

    // truncate body to avoid token limit
    private String truncateBody(String body) {
        if (body == null)
            return "";
        return body.length() > 1500 ? body.substring(0, 1500) + "..." : body;
    }
}
