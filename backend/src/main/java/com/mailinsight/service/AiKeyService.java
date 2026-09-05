package com.mailinsight.service;

import com.mailinsight.dto.request.AiKeyRequest;
import com.mailinsight.entity.User;
import com.mailinsight.entity.UserAiKey;
import com.mailinsight.exception.ResourceNotFoundException;
import com.mailinsight.repository.UserAiKeyRepository;
import com.mailinsight.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiKeyService {

    private final UserAiKeyRepository aiKeyRepository;
    private final EncryptionUtil encryptionUtil;

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

    @Transactional(readOnly = true)
    public String getDecryptedKey(User user) {
        UserAiKey aiKey = aiKeyRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No Gemini API key found. Please save your API key in Settings first."));
        return encryptionUtil.decrypt(aiKey.getEncryptedApiKey());
    }

    @Transactional(readOnly = true)
    public boolean hasKey(User user) {
        return aiKeyRepository.existsByUser(user);
    }

    @Transactional
    public void deleteKey(User user) {
        UserAiKey aiKey = aiKeyRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No Gemini API key found to delete."));
        aiKeyRepository.delete(aiKey);
        log.info("Deleted Gemini API key for user={}", user.getId());
    }
}
