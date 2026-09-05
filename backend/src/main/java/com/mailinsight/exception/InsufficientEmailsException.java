package com.mailinsight.exception;

public class InsufficientEmailsException extends RuntimeException {

    private final int currentCount;
    private final int requiredCount;

    public InsufficientEmailsException(int currentCount, int requiredCount) {
        super(String.format(
                "You have %d new email%s. A minimum of %d new emails is required to run AI analysis. " +
                "This limit helps stay within Google AI Studio's free-tier rate limits.",
                currentCount,
                currentCount == 1 ? "" : "s",
                requiredCount
        ));
        this.currentCount = currentCount;
        this.requiredCount = requiredCount;
    }

    public int getCurrentCount() {
        return currentCount;
    }

    public int getRequiredCount() {
        return requiredCount;
    }
}
