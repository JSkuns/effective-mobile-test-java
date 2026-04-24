package com.example.bankcards.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Сервис для шифрования и дешифрования чувствительных данных (например, номеров карт).
 * <br>
 * Использует симметричное шифрование AES-256 в режиме ECB.
 * <br>
 * <b>Конфигурация:</b>
 * encryption.secret-key=32-символьный-ключ-для-AES-256
 */
@Service
public class EncryptionService {

    private static final Logger logger = LoggerFactory.getLogger(EncryptionService.class);

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final int EXPECTED_KEY_LENGTH = 32;
    @Value("${encryption.secret-key}")
    private String secretKey;

    /**
     * Шифрует строку с использованием алгоритма AES-256.
     * <br>
     * <b>Алгоритм:</b>
     * <ol>
     *   <li>Валидирует длину ключа (должен быть 32 байта для AES-256)</li>
     *   <li>Создаёт {@link SecretKeySpec} из строки ключа</li>
     *   <li>Инициализирует {@link Cipher} в режиме шифрования</li>
     *   <li>Шифрует данные и кодирует результат в Base64</li>
     * </ol>
     *
     * @param plainText открытый текст для шифрования
     * @return зашифрованная строка в кодировке Base64
     * @throws IllegalArgumentException если ключ имеет неверную длину или входные данные {@code null}
     * @throws RuntimeException         если произошла ошибка шифрования
     */
    public String encrypt(String plainText) {
        if (plainText == null) {
            throw new IllegalArgumentException("Plain text cannot be null");
        }
        validateKey();

        try {
            logger.debug("Encrypting data");

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            SecretKeySpec keySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8),
                    ALGORITHM
            );

            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            String result = Base64.getEncoder().encodeToString(encrypted);
            logger.debug("Data encrypted successfully");

            return result;

        } catch (Exception e) {
            logger.error("Failed to encrypt data: {}", e.getMessage());
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Дешифрует строку, ранее зашифрованную методом {@link #encrypt(String)}.
     * <br>
     * <b>Алгоритм:</b>
     * <ol>
     *   <li>Валидирует длину ключа</li>
     *   <li>Декодирует Base64-строку в байты</li>
     *   <li>Инициализирует {@link Cipher} в режиме дешифрования</li>
     *   <li>Дешифрует данные и возвращает строку в UTF-8</li>
     * </ol>
     *
     * @param encryptedText зашифрованная строка в кодировке Base64
     * @return расшифрованный открытый текст
     * @throws IllegalArgumentException если входные данные {@code null} или некорректны
     * @throws RuntimeException         если произошла ошибка дешифрования
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isBlank()) {
            throw new IllegalArgumentException("Encrypted text cannot be null or empty");
        }
        validateKey();

        try {
            logger.debug("Decrypting data");

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            SecretKeySpec keySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8),
                    ALGORITHM
            );

            cipher.init(Cipher.DECRYPT_MODE, keySpec);

            // Сначала декодируем Base64
            byte[] decoded;
            try {
                decoded = Base64.getDecoder().decode(encryptedText);
            } catch (IllegalArgumentException e) {
                logger.error("Invalid Base64 input: {}", e.getMessage());
                throw new RuntimeException("Decryption failed: invalid Base64 input", e);
            }

            byte[] decrypted = cipher.doFinal(decoded);

            String result = new String(decrypted, StandardCharsets.UTF_8);
            logger.debug("Data decrypted successfully");

            return result;

        } catch (IllegalArgumentException e) {
            // Пробрасываем ошибки валидации как есть
            throw e;
        } catch (Exception e) {
            logger.error("Failed to decrypt data: {}", e.getMessage());
            throw new RuntimeException("Decryption failed", e);
        }
    }

    /**
     * Валидирует длину секретного ключа.
     * <br>
     * Для AES-256 ключ должен быть ровно 32 байта (256 бит).
     *
     * @throws IllegalStateException если ключ имеет неверную длину
     */
    private void validateKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != EXPECTED_KEY_LENGTH) {
            String errorMsg = String.format(
                    "Invalid key length: expected %d bytes, got %d bytes. " +
                            "For AES-256, the key must be exactly 32 characters.",
                    EXPECTED_KEY_LENGTH, keyBytes.length
            );
            logger.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }
    }

}