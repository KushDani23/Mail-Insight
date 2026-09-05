package com.mailinsight.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

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

    public static EmailPriority fromLevel(int level) {
        for (EmailPriority p : values()) {
            if (p.level == level)
                return p;
        }
        throw new IllegalArgumentException("Invalid priority level: " + level + ". Must be 1–5.");
    }
}
