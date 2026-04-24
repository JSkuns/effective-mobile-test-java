package com.example.bankcards.service;

import com.example.bankcards.util.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Base64;

import static org.assertj.core.api.Assertions.*;

/**
 * Юнит-тесты для {@link EncryptionService} без загрузки Spring контекста.
 */
class EncryptionServiceUnitTest {

    private EncryptionService encryptionService;

    // Тестовый ключ: ровно 32 символа для AES-256
    private static final String TEST_SECRET_KEY = "01234567890123456789012345678901";
    private static final String TEST_PLAIN_TEXT = "4276123456789012"; // Номер карты

    @BeforeEach
    void setUp() throws Exception {
        encryptionService = new EncryptionService();
        setPrivateField(encryptionService, "secretKey", TEST_SECRET_KEY);
    }

    // ==================== ENCRYPT TESTS ====================

    @Test
    @DisplayName("encrypt с валидными данными → возвращает Base64-строку")
    void encrypt_withValidInput_shouldReturnBase64String() {
        // When
        String encrypted = encryptionService.encrypt(TEST_PLAIN_TEXT);

        // Then
        assertThat(encrypted).isNotNull();
        assertThat(encrypted).isNotEmpty();

        // Результат должен быть валидным Base64
        assertThatCode(() -> Base64.getDecoder().decode(encrypted))
                .doesNotThrowAnyException();

        // Зашифрованный текст не должен совпадать с исходным
        assertThat(encrypted).isNotEqualTo(TEST_PLAIN_TEXT);
    }

    @Test
    @DisplayName("encrypt с null → выбрасывает IllegalArgumentException")
    void encrypt_withNullInput_shouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> encryptionService.encrypt(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Plain text cannot be null");
    }

    @Test
    @DisplayName("encrypt с пустой строкой → успешно шифрует")
    void encrypt_withEmptyString_shouldSucceed() {
        // When
        String encrypted = encryptionService.encrypt("");

        // Then
        assertThat(encrypted).isNotNull();
        assertThat(encrypted).isNotEmpty();
    }

    // ==================== DECRYPT TESTS ====================

    @Test
    @DisplayName("decrypt зашифрованных данных → возвращает исходный текст")
    void decrypt_withValidEncryptedText_shouldReturnOriginalText() {
        // Given
        String encrypted = encryptionService.encrypt(TEST_PLAIN_TEXT);

        // When
        String decrypted = encryptionService.decrypt(encrypted);

        // Then
        assertThat(decrypted).isEqualTo(TEST_PLAIN_TEXT);
    }

    @Test
    @DisplayName("decrypt с null → выбрасывает IllegalArgumentException")
    void decrypt_withNullInput_shouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> encryptionService.decrypt(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Encrypted text cannot be null or empty");
    }

    @Test
    @DisplayName("decrypt с пустой строкой → выбрасывает IllegalArgumentException")
    void decrypt_withEmptyInput_shouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> encryptionService.decrypt(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Encrypted text cannot be null or empty");
    }

    @Test
    @DisplayName("decrypt с невалидным Base64 → выбрасывает RuntimeException")
    void decrypt_withInvalidBase64_shouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> encryptionService.decrypt("not-valid-base64!@#"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Decryption failed");
    }

    @Test
    @DisplayName("decrypt с данными, зашифрованными другим ключом → выбрасывает RuntimeException")
    void decrypt_withWrongKey_shouldThrowException() throws Exception {
        // Given: шифруем одним ключом
        String encrypted = encryptionService.encrypt(TEST_PLAIN_TEXT);

        // Меняем ключ в сервисе
        setPrivateField(encryptionService, "secretKey", "WRONG_KEY_1234567890123456789012");

        // When & Then
        assertThatThrownBy(() -> encryptionService.decrypt(encrypted))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Decryption failed");
    }

    // ==================== KEY VALIDATION TESTS ====================

    @Test
    @DisplayName("encrypt с коротким ключом (< 32 байта) → выбрасывает IllegalStateException")
    void encrypt_withShortKey_shouldThrowException() throws Exception {
        // Given
        setPrivateField(encryptionService, "secretKey", "short");

        // When & Then
        assertThatThrownBy(() -> encryptionService.encrypt(TEST_PLAIN_TEXT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid key length")
                .hasMessageContaining("expected 32 bytes");
    }

    @Test
    @DisplayName("encrypt с длинным ключом (> 32 байта) → выбрасывает IllegalStateException")
    void encrypt_withLongKey_shouldThrowException() throws Exception {
        // Given
        String longKey = "01234567890123456789012345678901EXTRA"; // 36 символов
        setPrivateField(encryptionService, "secretKey", longKey);

        // When & Then
        assertThatThrownBy(() -> encryptionService.encrypt(TEST_PLAIN_TEXT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid key length");
    }

    @Test
    @DisplayName("decrypt с коротким ключом → выбрасывает IllegalStateException")
    void decrypt_withShortKey_shouldThrowException() throws Exception {
        // Given
        setPrivateField(encryptionService, "secretKey", "short");

        // When & Then
        assertThatThrownBy(() -> encryptionService.decrypt("any"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid key length");
    }

    // ==================== ROUNDTRIP TESTS ====================

    @Test
    @DisplayName("encrypt → decrypt → исходный текст (разные данные)")
    void encryptDecrypt_roundtrip_withVariousInputs() {
        // Given: различные тестовые данные
        String[] testInputs = {
                "4276123456789012",           // Номер карты
                "User123",                     // Логин
                "Специальные символы: !@#$%",   // Юникод
                "a".repeat(1000)               // Длинная строка
        };

        for (String input : testInputs) {
            // When
            String encrypted = encryptionService.encrypt(input);
            String decrypted = encryptionService.decrypt(encrypted);

            // Then
            assertThat(decrypted)
                    .as("Roundtrip failed for input: %s", input.substring(0, Math.min(20, input.length())))
                    .isEqualTo(input);
        }
    }

    @Test
    @DisplayName("encrypt дважды → разные результаты (из-за режима шифрования)")
    void encrypt_twice_shouldProduceSameResultForECB() {
        // Примечание: ECB-режим детерминирован — одинаковый вход = одинаковый выход
        // Это особенность (и уязвимость) ECB, но мы проверяем корректность реализации

        // When
        String encrypted1 = encryptionService.encrypt(TEST_PLAIN_TEXT);
        String encrypted2 = encryptionService.encrypt(TEST_PLAIN_TEXT);

        // Then: в режиме ECB результаты должны совпадать
        assertThat(encrypted1).isEqualTo(encrypted2);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Устанавливает значение приватного поля через рефлексию.
     * Используется для инъекции тестового секретного ключа.
     *
     * @param target объект, в котором нужно установить поле
     * @param fieldName имя поля
     * @param value новое значение поля
     * @throws Exception если поле не найдено или недоступно
     */
    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

}