package com.mailinsight.dto.response;

public record AnalyzeResultResponse(int analyzed, int skipped, String message) {
}
