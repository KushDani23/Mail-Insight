package com.mailanalyzer.repository;

import com.mailanalyzer.entity.User;
import com.mailanalyzer.entity.UserAiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link UserAiKey} – stores each user's encrypted Gemini API key.
 */
@Repository
public interface UserAiKeyRepository extends JpaRepository<UserAiKey, UUID> {

    /**
     * Find the AI key record for a specific user.
     * Returns empty if the user has not yet saved a Gemini API key.
     */
    Optional<UserAiKey> findByUser(User user);

    /**
     * Check if the user has already saved an AI key.
     * Used by UserController to return key status without exposing the key.
     */
    boolean existsByUser(User user);
}
