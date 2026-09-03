package com.mailanalyzer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Stores a user's Gemini AI Studio API key (AES-256-GCM encrypted).
 *
 * <p><b>Security rules:</b>
 * <ul>
 *   <li>The raw API key is NEVER stored or logged.</li>
 *   <li>{@code encryptedApiKey} is encrypted with
 *       {@link com.mailanalyzer.util.EncryptionUtil} before being saved.</li>
 *   <li>The key is decrypted only inside
 *       {@link com.mailanalyzer.service.GeminiService} at the moment a
 *       Gemini request is made, and is not returned to the frontend.</li>
 * </ul>
 *
 * <p>Future providers (OPEN_ROUTER, OLLAMA) can be added to the
 * {@code provider} column without schema changes.
 */
@Entity
@Table(name = "user_ai_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** One API key per user (enforced by the unique constraint on the FK). */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * AI provider. Default is "GEMINI".
     * Future values: "OPEN_ROUTER", "OLLAMA".
     */
    @Column(name = "provider", nullable = false, length = 50)
    @Builder.Default
    private String provider = "GEMINI";

    /**
     * AES-256-GCM encrypted Gemini API key.
     * Never expose or log this value.
     */
    @Column(name = "encrypted_api_key", nullable = false, columnDefinition = "TEXT")
    private String encryptedApiKey;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
