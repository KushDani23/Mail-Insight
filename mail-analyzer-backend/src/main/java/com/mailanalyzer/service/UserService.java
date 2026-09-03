package com.mailanalyzer.service;

import com.mailanalyzer.entity.User;
import com.mailanalyzer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for user-related operations.
 *
 * <p>The most important method here is {@link #findOrCreateUser}, which is
 * called on every Google OAuth2 login.  If the user's Google account has
 * logged in before, we update their profile info.  If it's a new account,
 * we create a new {@link User} row.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    /**
     * Creates a new {@link User} from OIDC claims, or updates an existing one.
     *
     * @param oidcUser the OIDC principal returned by Google
     * @return the persisted User entity
     */
    @Transactional
    public User findOrCreateUser(OidcUser oidcUser) {
        String googleSub = oidcUser.getSubject();

        return userRepository.findByGoogleSub(googleSub)
                .map(existingUser -> {
                    // Update mutable profile fields on every login
                    existingUser.setName(oidcUser.getFullName());
                    existingUser.setPictureUrl(oidcUser.getPicture());
                    existingUser.setEmail(oidcUser.getEmail());
                    User saved = userRepository.save(existingUser);
                    log.info("Updated existing user: id={}", saved.getId());
                    return saved;
                })
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .googleSub(googleSub)
                            .email(oidcUser.getEmail())
                            .name(oidcUser.getFullName())
                            .pictureUrl(oidcUser.getPicture())
                            .build();
                    User saved = userRepository.save(newUser);
                    log.info("Created new user: id={} email={}", saved.getId(), saved.getEmail());
                    return saved;
                });
    }

    /**
     * Resolves the currently authenticated user from a Spring Security Authentication.
     *
     * <p>Used by controllers that receive {@code Authentication} as a method parameter.
     * Throws {@link IllegalStateException} if the principal is not an OidcUser
     * (should not happen in normal usage – all logins go through Google OAuth2).
     */
    @Transactional(readOnly = true)
    public User resolveUser(Authentication authentication) {
        String googleSub = authentication.getName();
        return userRepository.findByGoogleSub(googleSub)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated principal not found in database: sub=" + googleSub));
    }
}
