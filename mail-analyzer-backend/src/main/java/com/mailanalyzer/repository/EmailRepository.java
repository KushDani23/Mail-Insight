package com.mailanalyzer.repository;

import com.mailanalyzer.entity.Email;
import com.mailanalyzer.entity.User;
import com.mailanalyzer.enums.EmailCategory;
import com.mailanalyzer.enums.EmailPriority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link Email} entities.
 *
 * <p><b>SECURITY INVARIANT:</b> Every query method MUST include the
 * {@code user} parameter.  This enforces that User A can never access
 * User B's emails, even if User B's UUID is somehow guessed.
 * The backend extracts the user from Spring Security's SecurityContext,
 * never from the request body or query parameters.
 */
@Repository
public interface EmailRepository extends JpaRepository<Email, UUID> {

    /**
     * Deduplication check: has this Gmail message already been analyzed?
     *
     * <p>Called before inserting each analyzed email.  The check is global
     * (no user scope) because gmail_message_id is globally unique across all
     * Gmail users, and the column has a UNIQUE constraint.
     *
     * @param gmailMessageId Gmail's stable message ID
     * @return true if the message has already been stored
     */
    boolean existsByGmailMessageId(String gmailMessageId);

    /**
     * All emails for a user, newest first, paginated.
     * Used by the main dashboard email table.
     */
    Page<Email> findAllByUserOrderByReceivedAtDesc(User user, Pageable pageable);

    /**
     * Emails for a user filtered by category, paginated.
     * Used by the "category click" feature on the dashboard.
     */
    Page<Email> findAllByUserAndCategoryOrderByReceivedAtDesc(User user, EmailCategory category, Pageable pageable);

    /**
     * Emails for a user filtered by priority, paginated.
     */
    Page<Email> findAllByUserAndPriorityOrderByReceivedAtDesc(User user, EmailPriority priority, Pageable pageable);

    /**
     * Count of emails per category for a specific user.
     * Used for the dashboard pie chart and stats cards.
     */
    @Query("SELECT e.category, COUNT(e) FROM Email e WHERE e.user = :user GROUP BY e.category")
    List<Object[]> countByCategoryForUser(@Param("user") User user);

    /**
     * Count of emails per priority for a specific user.
     * Used for the dashboard bar chart.
     */
    @Query("SELECT e.priority, COUNT(e) FROM Email e WHERE e.user = :user GROUP BY e.priority")
    List<Object[]> countByPriorityForUser(@Param("user") User user);

    /**
     * Total count of analyzed emails for this user.
     */
    long countByUser(User user);

    /**
     * Emails for a user, filtered by the source Gmail account address.
     */
    Page<Email> findAllByUserAndSourceAccountOrderByReceivedAtDesc(User user, String sourceAccount, Pageable pageable);
}
