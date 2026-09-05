package com.mailinsight.exception;

public class GmailException extends RuntimeException {

    public GmailException(String message) {
        super(message);
    }

    public GmailException(String message, Throwable cause) {
        super(message, cause);
    }
}
