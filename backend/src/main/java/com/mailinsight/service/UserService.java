package com.mailinsight.service;

import com.mailinsight.entity.User;
import com.mailinsight.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User findOrCreateUser(OidcUser oidcUser) {
        String googleSub = oidcUser.getSubject();

        return userRepository.findByGoogleSub(googleSub)
                .map(existingUser -> {
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

    @Transactional(readOnly = true)
    public User resolveUser(Authentication authentication) {
        String googleSub = authentication.getName();
        return userRepository.findByGoogleSub(googleSub)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated principal not found in database: sub=" + googleSub));
    }
}
