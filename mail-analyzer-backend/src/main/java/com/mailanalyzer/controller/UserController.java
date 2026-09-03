package com.mailanalyzer.controller;

import com.mailanalyzer.dto.request.AiKeyRequest;
import com.mailanalyzer.entity.User;
import com.mailanalyzer.service.AiKeyService;
import com.mailanalyzer.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for user-level settings.
 *
 * <h3>Endpoints</h3>
 * <ul>
 * <li>POST /api/user/ai-key — save or update Gemini API key</li>
 * <li>GET /api/user/ai-key/status — check if key exists (never returns the
 * key)</li>
 * <li>DELETE /api/user/ai-key — remove stored key</li>
 * </ul>
 *
 * <p>
 * The authenticated user is always resolved from Spring Security's
 * {@code Authentication} object — never from the request body or params.
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AiKeyService aiKeyService;

    // ── POST /api/user/ai-key ─────────────────────────────────────────────

    /**
     * Saves or updates the user's Gemini API key.
     *
     * <p>
     * The key is encrypted with AES-256-GCM before storage.
     * The raw key is NEVER logged or returned to the frontend.
     *
     * @param request        validated request body containing the raw API key
     * @param authentication injected by Spring Security
     * @return 200 OK with a success message
     */
    @PostMapping("/ai-key")
    public ResponseEntity<Map<String, String>> saveApiKey(
            @Valid @RequestBody AiKeyRequest request,
            Authentication authentication) {

        User user = userService.resolveUser(authentication);
        aiKeyService.saveKey(user, request);
        return ResponseEntity.ok(Map.of("message", "Gemini API key saved successfully."));
    }

    // ── GET /api/user/ai-key/status ───────────────────────────────────────

    /**
     * Returns whether the user has already saved a Gemini API key.
     *
     * <p>
     * The frontend uses this to show/hide the "Save Key" form in Settings.
     * The actual key value is NEVER included in the response.
     *
     * @return {@code {"hasKey": true/false}}
     */
    @GetMapping("/ai-key/status")
    public ResponseEntity<Map<String, Boolean>> getApiKeyStatus(Authentication authentication) {
        User user = userService.resolveUser(authentication);
        boolean hasKey = aiKeyService.hasKey(user);
        return ResponseEntity.ok(Map.of("hasKey", hasKey));
    }

    // ── DELETE /api/user/ai-key ───────────────────────────────────────────

    /**
     * Deletes the user's stored Gemini API key.
     *
     * <p>
     * After deletion, calling {@code POST /api/emails/analyze} will return
     * 404 until the user saves a new key.
     *
     * @return 204 No Content on success
     */
    @DeleteMapping("/ai-key")
    public ResponseEntity<Void> deleteApiKey(Authentication authentication) {
        User user = userService.resolveUser(authentication);
        aiKeyService.deleteKey(user);
        return ResponseEntity.noContent().build();
    }
}
