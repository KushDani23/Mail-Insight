package com.mailanalyzer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a registered user of the AI Mail Analyzer.
 *
 * <p>Users are created automatically on first Google OAuth2 login via
 * {@link com.mailanalyzer.oauth2.OAuth2LoginSuccessHandler}.
 *
 * <p>The {@code googleSub} field is the "sub" claim from Google's OIDC
 * token – it is stable, globally unique per Google account, and is used
 * as the principal name by Spring Security.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Google's stable subject identifier (OIDC "sub" claim).
     * Used as the Spring Security principal name.
     */
    @Column(name = "google_sub", nullable = false, unique = true, length = 255)
    private String googleSub;

    /** Primary email of the Google account used to log in. */
    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "picture_url", length = 1024)
    private String pictureUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    // ── Relationships ────────────────────────────────────────────────────

    /** All Gmail accounts connected by this user. */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ConnectedAccount> connectedAccounts = new ArrayList<>();

    /** Analyzed emails belonging to this user. */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Email> emails = new ArrayList<>();

    /** User's Gemini API key (one per user). */
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private UserAiKey aiKey;
}
