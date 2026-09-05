package com.mailinsight.dto.response;

public record NewEmailCountResponse(int count, boolean analyzeEnabled) {
    public static NewEmailCountResponse of(int count) {
        return new NewEmailCountResponse(count, count > 0);
    }
}
