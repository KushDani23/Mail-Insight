package com.mailanalyzer.repository;

import com.mailanalyzer.entity.ConnectedAccount;
import com.mailanalyzer.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link ConnectedAccount} entities.
 *
 * <p>Every query that fetches accounts MUST include the {@code user}
 * parameter to enforce user-level isolation.
 */
@Repository
public interface ConnectedAccountRepository extends JpaRepository<ConnectedAccount, UUID> {

    /**
     * Returns all Gmail accounts connected by the given user.
     * Used by AccountController to list accounts on the dashboard.
     */
    List<ConnectedAccount> findAllByUser(User user);

    /**
     * Find a specific connected account by user and Gmail address.
     * Used when saving tokens after OAuth2 consent (to update vs. create).
     */
    Optional<ConnectedAccount> findByUserAndGmailAddress(User user, String gmailAddress);

    /**
     * Returns the user's first connected account (primary account).
     * Used by the custom OAuth2AuthorizedClientService to load tokens.
     */
    Optional<ConnectedAccount> findFirstByUserOrderByCreatedAtAsc(User user);

    /**
     * Check if the user has connected a specific Gmail address.
     * Prevents duplicate connections.
     */
    boolean existsByUserAndGmailAddress(User user, String gmailAddress);

    /**
     * Count how many Gmail accounts the user has connected.
     */
    long countByUser(User user);
}
