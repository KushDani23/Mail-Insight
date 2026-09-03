package com.mailanalyzer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks the last sync state for a single {@link ConnectedAccount}.
 *
 * <p>The {@code historyId} field stores Gmail's History ID from the last
 * successful sync.  On the next sync, {@link com.mailanalyzer.service.GmailService}
 * uses the History API to fetch only new/changed messages since that ID,
 * making incremental sync very efficient.
 *
 * <p>If {@code historyId} is {@code null} (first sync), GmailService falls
 * back to fetching the most recent N messages and filtering out already-
 * analyzed ones via {@link com.mailanalyzer.repository.EmailRepository#existsByGmailMessageId(String)}.
 */
@Entity
@Table(name = "user_sync_metadata")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSyncMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** The specific connected account this metadata tracks. */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private ConnectedAccount account;

    /** Gmail History ID from the last successful message sync (UTC). */
    @Column(name = "history_id", length = 255)
    private String historyId;

    /** UTC timestamp of the last successful sync for this account. */
    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;
}
