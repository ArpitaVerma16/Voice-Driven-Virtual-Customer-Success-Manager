package com.vcsm.encryption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.logging.Logger;

@Service
public class PIIEncryptionService {

    private static final Logger logger = Logger.getLogger(PIIEncryptionService.class.getName());

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final int AES_KEY_SIZE = 256;

    @Value("${pii.encryption.key:#{null}}")
    private String encryptionKey;

    @Value("${pii.encryption.enabled:true}")
    private boolean encryptionEnabled;

    private SecretKey getSecretKey() {
        if (encryptionKey == null || encryptionKey.isEmpty()) {
            throw new IllegalStateException("PII_ENCRYPTION_KEY environment variable not set");
        }

        byte[] keyBytes = Base64.getDecoder().decode(encryptionKey);
        if (keyBytes.length != AES_KEY_SIZE / 8) {
            throw new IllegalStateException("Invalid encryption key size. Expected 32 bytes for 256-bit AES");
        }

        return new SecretKeySpec(keyBytes, 0, keyBytes.length, "AES");
    }

    public String encryptField(String plaintext) {
        if (!encryptionEnabled || plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }

        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKey secretKey = getSecretKey();
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(GCM_IV_LENGTH + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);

            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            logger.severe("Failed to encrypt PII field: " + e.getMessage());
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public String decryptField(String encryptedData) {
        if (!encryptionEnabled || encryptedData == null || encryptedData.isEmpty()) {
            return encryptedData;
        }

        try {
            byte[] data = Base64.getDecoder().decode(encryptedData);
            ByteBuffer buffer = ByteBuffer.wrap(data);

            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);

            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKey secretKey = getSecretKey();
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
            byte[] plaintext = cipher.doFinal(ciphertext);

            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.severe("Failed to decrypt PII field: " + e.getMessage());
            throw new RuntimeException("Decryption failed", e);
        }
    }

    public String generateEncryptionKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(AES_KEY_SIZE);
            SecretKey key = keyGen.generateKey();
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (Exception e) {
            logger.severe("Failed to generate encryption key: " + e.getMessage());
            throw new RuntimeException("Key generation failed", e);
        }
    }

    public boolean isEncryptionEnabled() {
        return encryptionEnabled;
    }

    public boolean isKeyConfigured() {
        return encryptionKey != null && !encryptionKey.isEmpty();
    }

    public String maskPIIField(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (value.length() <= 4) {
            return "*".repeat(value.length());
        }
        return "*".repeat(value.length() - 4) + value.substring(value.length() - 4);
    }
}
