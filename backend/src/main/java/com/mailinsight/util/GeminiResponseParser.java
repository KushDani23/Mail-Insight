package com.mailinsight.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailinsight.enums.EmailCategory;
import com.mailinsight.enums.EmailPriority;
import com.mailinsight.exception.GeminiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class GeminiResponseParser {

    private final ObjectMapper objectMapper;

    public List<AnalyzedEmailDto> parseAndValidate(String rawResponse, List<String> sentIds) {
        String cleanedJson = stripMarkdownFences(rawResponse);

        List<Map<String, Object>> items;
        try {
            items = objectMapper.readValue(cleanedJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.error("Gemini returned invalid JSON. First 500 chars: {}",
                    rawResponse.substring(0, Math.min(rawResponse.length(), 500)));
            throw new GeminiException("Gemini returned malformed JSON: " + e.getMessage());
        }

        if (items == null || items.isEmpty()) {
            throw new GeminiException("Gemini returned an empty JSON array");
        }

        Set<String> validIds = Set.copyOf(sentIds);
        List<AnalyzedEmailDto> results = new ArrayList<>();
        int skipped = 0;

        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> item = items.get(i);
            try {
                AnalyzedEmailDto dto = validateSingleItem(item, validIds, i);
                results.add(dto);
            } catch (IllegalArgumentException e) {
                skipped++;
                log.warn("Skipping invalid Gemini result at index {}: {}", i, e.getMessage());
            }
        }

        log.info("Gemini response parsed: {} valid, {} skipped out of {} total",
                results.size(), skipped, items.size());

        if (results.isEmpty()) {
            throw new GeminiException(
                    "All " + items.size() + " Gemini results failed validation. " +
                            "Check logs for details.");
        }

        return results;
    }

    // Private validation
    private AnalyzedEmailDto validateSingleItem(Map<String, Object> item,
            Set<String> validIds,
            int index) {
        // 1. gmail_message_id – required, must match what we sent
        String messageId = getStringField(item, "gmail_message_id", index);
        if (!validIds.contains(messageId)) {
            throw new IllegalArgumentException(
                    "gmail_message_id '" + messageId + "' was not in the original batch");
        }

        // 2. summary – required, non-blank
        String summary = getStringField(item, "summary", index);
        if (summary.isBlank()) {
            throw new IllegalArgumentException("summary is blank at index " + index);
        }

        // 3. category – required, must be a valid EmailCategory enum value
        String categoryStr = getStringField(item, "category", index);
        EmailCategory category;
        try {
            category = EmailCategory.valueOf(categoryStr.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid category '" + categoryStr + "' at index " + index +
                            ". Must be one of: " + java.util.Arrays.toString(EmailCategory.values()));
        }

        // 4. priority – required, integer 1–5
        int priorityInt = getIntField(item, "priority", index);
        EmailPriority priority;
        try {
            priority = EmailPriority.fromLevel(priorityInt);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid priority " + priorityInt + " at index " + index + ". Must be 1–5.");
        }

        return AnalyzedEmailDto.builder()
                .gmailMessageId(messageId)
                .summary(summary)
                .category(category)
                .priority(priority)
                .build();
    }

    // Field extractors
    private String getStringField(Map<String, Object> item, String field, int index) {
        Object value = item.get(field);
        if (value == null) {
            throw new IllegalArgumentException("Missing required field '" + field + "' at index " + index);
        }
        return value.toString().trim();
    }

    private int getIntField(Map<String, Object> item, String field, int index) {
        Object value = item.get(field);
        if (value == null) {
            throw new IllegalArgumentException("Missing required field '" + field + "' at index " + index);
        }
        if (value instanceof Number num) {
            return num.intValue();
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Field '" + field + "' is not a valid integer at index " + index + ": " + value);
        }
    }

    // JSON cleanup
    private String stripMarkdownFences(String raw) {
        if (raw == null)
            return "";
        String trimmed = raw.trim();

        // Remove ```json ... ``` or ```
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }

        return trimmed.trim();
    }
}
