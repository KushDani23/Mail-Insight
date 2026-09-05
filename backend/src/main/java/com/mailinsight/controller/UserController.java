package com.mailinsight.controller;

import com.mailinsight.dto.request.AiKeyRequest;
import com.mailinsight.entity.User;
import com.mailinsight.service.AiKeyService;
import com.mailinsight.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AiKeyService aiKeyService;

    @PostMapping("/ai-key")
    public ResponseEntity<Map<String, String>> saveApiKey(
            @Valid @RequestBody AiKeyRequest request,
            Authentication authentication) {

        User user = userService.resolveUser(authentication);
        aiKeyService.saveKey(user, request);
        return ResponseEntity.ok(Map.of("message", "Gemini API key saved successfully."));
    }

    @GetMapping("/ai-key/status")
    public ResponseEntity<Map<String, Boolean>> getApiKeyStatus(Authentication authentication) {
        User user = userService.resolveUser(authentication);
        boolean hasKey = aiKeyService.hasKey(user);
        return ResponseEntity.ok(Map.of("hasKey", hasKey));
    }

    @DeleteMapping("/ai-key")
    public ResponseEntity<Void> deleteApiKey(Authentication authentication) {
        User user = userService.resolveUser(authentication);
        aiKeyService.deleteKey(user);
        return ResponseEntity.noContent().build();
    }
}
