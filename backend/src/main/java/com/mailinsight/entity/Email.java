package com.mailinsight.entity;

import com.mailinsight.enums.EmailCategory;
import com.mailinsight.enums.EmailPriority;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "emails", indexes = {
        @Index(name = "idx_email_user_id", columnList = "user_id"),
        @Index(name = "idx_email_category", columnList = "category"),
        @Index(name = "idx_email_priority", columnList = "priority"),
        @Index(name = "idx_email_received_at", columnList = "received_at"),
        @Index(name = "idx_email_source_account", columnList = "source_account")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Email {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "gmail_message_id", nullable = false, unique = true, length = 255)
    private String gmailMessageId;

    @Column(name = "source_account", nullable = false, length = 255)
    private String sourceAccount;

    @Column(name = "sender", length = 500)
    private String sender;

    @Column(name = "subject", columnDefinition = "TEXT")
    private String subject;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 100)
    private EmailCategory category;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "priority", nullable = false)
    private EmailPriority priority;

    @Column(name = "received_at")
    private Instant receivedAt;

    @CreationTimestamp
    @Column(name = "analyzed_at", updatable = false)
    private Instant analyzedAt;
}
