package com.mailanalyzer.controller;

import com.mailanalyzer.dto.response.UserResponse;
import com.mailanalyzer.entity.User;
import com.mailanalyzer.mapper.EmailMapper;
import com.mailanalyzer.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication-related endpoints.
 *
 * <p>The actual OAuth2 login flow is handled by Spring Security at
 * {@code /oauth2/authorization/google}.  This controller only provides
 * the session state endpoints consumed by the React frontend.
 *
 * <p>The authenticated user is always resolved from Spring Security's
 * {@code Authentication} object – never from a request parameter or body.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final EmailMapper emailMapper;

    /**
     * GET /api/auth/me
     *
     * <p>Returns the currently logged-in user's profile.  The React frontend
     * calls this on app startup to determine if the user is authenticated and
     * to populate the navbar (name, picture).
     *
     * <p>Returns 401 (handled by Spring Security) if the session is missing
     * or expired.
     *
     * @param authentication injected by Spring Security from the HTTP session
     * @return UserResponse DTO (no sensitive fields)
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        User user = userService.resolveUser(authentication);
        return ResponseEntity.ok(emailMapper.toUserResponse(user));
    }

    /**
     * GET /api/auth/logout
     *
     * <p>Logout is handled by Spring Security's logout filter configured in
     * {@link com.mailanalyzer.config.SecurityConfig}.  This method is a
     * fallback that should not normally be reached.
     */
    @GetMapping("/logout")
    public ResponseEntity<Void> logout() {
        // Spring Security's logout filter intercepts before this runs.
        return ResponseEntity.ok().build();
    }
}
