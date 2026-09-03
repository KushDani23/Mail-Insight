package com.mailanalyzer.repository;

import com.mailanalyzer.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link User} entities.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by Google's stable OIDC subject identifier.
     * Used during OAuth2 login to check if the user already exists.
     *
     * @param googleSub the "sub" claim from the Google OIDC token
     */
    Optional<User> findByGoogleSub(String googleSub);

    /**
     * Check if a user with the given Google sub already exists.
     * Used to decide whether to create a new user or update an existing one.
     */
    boolean existsByGoogleSub(String googleSub);
}
