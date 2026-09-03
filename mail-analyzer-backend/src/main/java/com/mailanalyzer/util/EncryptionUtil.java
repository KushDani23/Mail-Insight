package com.mailanalyzer.util;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption utility for storing sensitive data in the database.
 *
 * <h3>What gets encrypted</h3>
 * <ul>
 *   <li>OAuth2 access tokens (in {@link com.mailanalyzer.entity.ConnectedAccount})</li>
 *   <li>OAuth2 refresh tokens (in {@link com.mailanalyzer.entity.ConnectedAccount})</li>
 *   <li>Gemini API keys (in {@link com.mailanalyzer.entity.UserAiKey})</li>
 * </ul>
 *
 * <h3>Algorithm</h3>
 * AES-256-GCM (authenticated encryption) with a random 12-byte IV per
 * encryption call.  The IV is prepended to the ciphertext so that each
 * encrypted value is self-contained and decryptable without external state.
 *
 * <h3>Key management</h3>
 * The 256-bit secret key is read from the {@code ENCRYPTION_SECRET} environment
 * variable (base64-encoded, 32 raw bytes).  Generate one with:
 * <pre>openssl rand -base64 32</pre>
 */
@Component
@Slf4j
public class EncryptionUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH   = 12;  // bytes
    private static final int GCM_TAG_LENGTH  = 128; // bits

    @Value("${app.encryption.secret}")
    private String base64Secret;

    private SecretKey secretKey;

    @PostConstruct
    void init() {
        byte[] keyBytes = Base64.getDecoder().decode(base64Secret);
        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                "ENCRYPTION_SECRET must be exactly 32 bytes (256 bits) when decoded from base64. " +
                "Got " + keyBytes.length + " bytes. Generate with: openssl rand -base64 32"
            );
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
        log.info("EncryptionUtil initialized (AES-256-GCM)");
    }

    // ── Public API ───────────────────────────────────────────────────────

    /**
     * Encrypts a plaintext string using AES-256-GCM.
     *
     * @param plaintext the value to encrypt (e.g. an OAuth2 token or API key)
     * @return base64-encoded string containing {@code IV || ciphertext || GCM-tag}
     * @throws RuntimeException if encryption fails
     */
    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // Prepend IV to ciphertext: [12-byte IV][ciphertext+tag]
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);

            return Base64.getEncoder().encodeToString(buffer.array());

        } catch (Exception e) {
            log.error("Encryption failed: {}", e.getMessage());
            throw new RuntimeException("Failed to encrypt value", e);
        }
    }

    /**
     * Decrypts a base64-encoded ciphertext produced by {@link #encrypt(String)}.
     *
     * @param encryptedBase64 base64 string containing {@code IV || ciphertext || tag}
     * @return the original plaintext
     * @throws RuntimeException if decryption fails (wrong key, tampered data, etc.)
     */
    public String decrypt(String encryptedBase64) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedBase64);

            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);

            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, java.nio.charset.StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("Decryption failed: {}", e.getMessage());
            throw new RuntimeException("Failed to decrypt value", e);
        }
    }
}
