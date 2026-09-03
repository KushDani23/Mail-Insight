package com.mailanalyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * AI Mail Analyzer – Spring Boot 3.x application entry point.
 *
 * <p>Spring AI's Google GenAI auto-configuration is excluded via
 * {@code spring.autoconfigure.exclude} in application.properties so that
 * no global Gemini API key is required at startup.  Each user's key is
 * retrieved at runtime and used to build a dedicated {@code ChatClient}
 * inside {@link com.mailanalyzer.service.GeminiService}.
 */
@SpringBootApplication
public class MailAnalyzerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MailAnalyzerApplication.class, args);
    }
}
