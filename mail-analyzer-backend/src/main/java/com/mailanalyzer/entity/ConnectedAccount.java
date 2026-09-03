package com.mailanalyzer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * A Gmail account that a user has connected to AI Mail Analyzer.
 *
 * <p>A single {@link User} can have multiple connected Gmail accounts
 * (e.g., personal + college). Each account stores its own encrypted
 * OAuth2 access and refresh tokens.
 *
 * <p><b>Security note:</b> {@code accessToken} and {@code refreshToken}
 * are stored AES-256-GCM encrypted using
 * {@link com.mailanalyzer.util.EncryptionUtil}.  Plain-text tokens are
 * NEVER persisted.
 */
@Entity
@Table(
    name = "connected_accounts",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_connected_account_user_gmail",
        columnNames = {"user_id", "gmail_address"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectedAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Owner of this connected account. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** The Gmail address of this connected account (e.g. kushdani1228@gmail.com). */
    @Column(name = "gmail_address", nullable = false, length = 255)
    private String gmailAddress;

    /**
     * AES-256-GCM encrypted OAuth2 access token.
     * Decrypt with {@link com.mailanalyzer.util.EncryptionUtil#decrypt(String)}.
     */
    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;

    /**
     * AES-256-GCM encrypted OAuth2 refresh token.
     * Used to obtain a new access token when the current one expires.
     */
    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    /** UTC instant at which the stored access token expires. */
    @Column(name = "token_expires_at")
    private Instant tokenExpiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    // ── Convenience method ───────────────────────────────────────────────

    /**
     * Returns {@code true} if the access token is expired or will expire
     * within the next 60 seconds (safety buffer).
     */
    public boolean isAccessTokenExpired() {
        return tokenExpiresAt == null || Instant.now().isAfter(tokenExpiresAt.minusSeconds(60));
    }
}
