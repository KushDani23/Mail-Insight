package com.mailanalyzer.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Email priority levels (1 = most important, 5 = least important).
 *
 * <p>Priority is determined by the {@link EmailCategory} and enforced by
 * {@link com.mailanalyzer.util.GeminiResponseParser}.  The AI is told the
 * expected priority for each category in the analysis prompt, so the
 * returned priority value must match the category's expected priority.
 */
@Getter
@RequiredArgsConstructor
public enum EmailPriority {

    PRIORITY_1(1, "High – time-sensitive or high-impact"),
    PRIORITY_2(2, "Medium-High – learning and growth"),
    PRIORITY_3(3, "Medium – content and community"),
    PRIORITY_4(4, "Low – marketing and noise"),
    PRIORITY_5(5, "Lowest – club and social activities");

    private final int level;
    private final String description;

    /**
     * Returns the EmailPriority for a raw integer level (1–5).
     *
     * @param level integer 1-5
     * @return matching EmailPriority
     * @throws IllegalArgumentException if level is out of range
     */
    public static EmailPriority fromLevel(int level) {
        for (EmailPriority p : values()) {
            if (p.level == level) return p;
        }
        throw new IllegalArgumentException("Invalid priority level: " + level + ". Must be 1–5.");
    }
}
