package com.mailanalyzer.service;

import com.mailanalyzer.dto.request.AiKeyRequest;
import com.mailanalyzer.entity.User;
import com.mailanalyzer.entity.UserAiKey;
import com.mailanalyzer.exception.ResourceNotFoundException;
import com.mailanalyzer.repository.UserAiKeyRepository;
import com.mailanalyzer.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages the user's Gemini API key lifecycle.
 *
 * <h3>Security guarantees</h3>
 * <ul>
 *   <li>The raw API key is encrypted with AES-256-GCM before persistence.</li>
 *   <li>The raw key is NEVER logged.</li>
 *   <li>The raw key is NEVER returned to the frontend.</li>
 *   <li>Decryption only happens inside {@link GeminiService} at the moment
 *       a Gemini request is made, and the decrypted value stays on the
 *       call stack (never stored in a field or cache).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiKeyService {

    private final UserAiKeyRepository aiKeyRepository;
    private final EncryptionUtil encryptionUtil;

    /**
     * Saves or updates the user's Gemini API key.
     * The raw key from the request is encrypted before storage.
     *
     * @param user    the authenticated user
     * @param request contains the raw API key (validated by @Valid)
     */
    @Transactional
    public void saveKey(User user, AiKeyRequest request) {
        UserAiKey aiKey = aiKeyRepository.findByUser(user)
                .orElse(UserAiKey.builder()
                        .user(user)
                        .provider("GEMINI")
                        .build());

        aiKey.setEncryptedApiKey(encryptionUtil.encrypt(request.apiKey()));
        aiKeyRepository.save(aiKey);

        log.info("Saved Gemini API key for user={}", user.getId());
    }

    /**
     * Returns the decrypted Gemini API key for the user.
     * Called ONLY by {@link GeminiService} — never exposed to the frontend.
     *
     * @param user the authenticated user
     * @return raw API key string
     * @throws ResourceNotFoundException if the user has not saved a key yet
     */
    @Transactional(readOnly = true)
    public String getDecryptedKey(User user) {
        UserAiKey aiKey = aiKeyRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No Gemini API key found. Please save your API key in Settings first."
                ));
        return encryptionUtil.decrypt(aiKey.getEncryptedApiKey());
    }

    /**
     * Checks whether the user has saved a Gemini API key.
     * Used by the frontend to show/hide the "Save Key" form.
     *
     * @param user the authenticated user
     * @return true if a key exists in the database
     */
    @Transactional(readOnly = true)
    public boolean hasKey(User user) {
        return aiKeyRepository.existsByUser(user);
    }

    /**
     * Deletes the user's stored Gemini API key.
     *
     * @param user the authenticated user
     * @throws ResourceNotFoundException if no key exists
     */
    @Transactional
    public void deleteKey(User user) {
        UserAiKey aiKey = aiKeyRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No Gemini API key found to delete."
                ));
        aiKeyRepository.delete(aiKey);
        log.info("Deleted Gemini API key for user={}", user.getId());
    }
}
