package com.example.bankcards.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Юнит-тесты для {@link CardMasker} без загрузки Spring контекста.
 */
@ExtendWith(MockitoExtension.class)
class CardMaskerUnitTest {

    @Mock private EncryptionService encryptionService;

    @InjectMocks private CardMasker cardMasker;

    // ==================== NULL/EMPTY INPUT TESTS ====================

    @Test
    @DisplayName("mask с null → возвращает \"****\"")
    void mask_withNullInput_shouldReturnSafeMask() {
        // When
        String result = cardMasker.mask(null);

        // Then
        assertThat(result).isEqualTo("****");
        verifyNoInteractions(encryptionService);
    }

    @Test
    @DisplayName("mask с пустой строкой → возвращает \"****\"")
    void mask_withEmptyString_shouldReturnSafeMask() {
        // When
        String result = cardMasker.mask("");

        // Then
        assertThat(result).isEqualTo("****");
        verifyNoInteractions(encryptionService);
    }

    @Test
    @DisplayName("mask с пробелами → возвращает \"****\"")
    void mask_withBlankString_shouldReturnSafeMask() {
        // When
        String result = cardMasker.mask("   ");

        // Then
        assertThat(result).isEqualTo("****");
        verifyNoInteractions(encryptionService);
    }

    // ==================== SUCCESSFUL DECRYPTION TESTS ====================

    @Test
    @DisplayName("mask с валидным 16-значным номером → стандартный формат маски")
    void mask_withValid16DigitCard_shouldReturnStandardFormat() throws Exception {
        // Given
        String encrypted = "encrypted_4276123456789012";
        when(encryptionService.decrypt(encrypted)).thenReturn("4276123456789012");

        // When
        String result = cardMasker.mask(encrypted);

        // Then
        assertThat(result).isEqualTo("**** **** **** 9012");
        verify(encryptionService).decrypt(encrypted);
    }

    @Test
    @DisplayName("mask с 15-значным номером (Amex) → последние 4 цифры видны")
    void mask_with15DigitCard_shouldShowLastFour() throws Exception {
        // Given
        String encrypted = "encrypted_378282246310005";
        when(encryptionService.decrypt(encrypted)).thenReturn("378282246310005");

        // When
        String result = cardMasker.mask(encrypted);

        // Then
        assertThat(result).isEqualTo("**** **** **** 0005");
    }

    @Test
    @DisplayName("mask с номером из 4 цифр → маскирует с префиксом")
    void mask_with4DigitCard_shouldReturnMasked() throws Exception {
        // Given
        String encrypted = "encrypted_1234";
        when(encryptionService.decrypt(encrypted)).thenReturn("1234");

        // When
        String result = cardMasker.mask(encrypted);

        // Then - даже 4-значный номер маскируется
        assertThat(result).isEqualTo("**** **** **** 1234");
    }

    @Test
    @DisplayName("mask с коротким номером (< 4 цифр) → возвращает как есть")
    void mask_withShortCard_shouldReturnAsIs() throws Exception {
        // Given
        String encrypted = "encrypted_123";
        when(encryptionService.decrypt(encrypted)).thenReturn("123");

        // When
        String result = cardMasker.mask(encrypted);

        // Then
        assertThat(result).isEqualTo("123");
        verify(encryptionService).decrypt(encrypted);
    }

    // ==================== ERROR HANDLING TESTS ====================

    @Test
    @DisplayName("mask с ошибкой расшифровки → возвращает безопасную маску")
    void mask_withDecryptionError_shouldReturnSafeMask() throws Exception {
        // Given
        String encrypted = "invalid_encrypted_data";
        when(encryptionService.decrypt(encrypted))
                .thenThrow(new RuntimeException("Decryption failed"));

        // When
        String result = cardMasker.mask(encrypted);

        // Then
        assertThat(result).isEqualTo("**** **** **** ****");
        verify(encryptionService).decrypt(encrypted);
    }

    @Test
    @DisplayName("mask с IllegalArgumentException при расшифровке → возвращает безопасную маску")
    void mask_withIllegalArgumentException_shouldReturnSafeMask() throws Exception {
        // Given
        when(encryptionService.decrypt(anyString()))
                .thenThrow(new IllegalArgumentException("Invalid Base64"));

        // When
        String result = cardMasker.mask("bad_input");

        // Then
        assertThat(result).isEqualTo("**** **** **** ****");
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("mask с номером, содержащим нецифровые символы → маскирует последние 4 символа")
    void mask_withNonDigitCardNumber_shouldMaskLastFourChars() throws Exception {
        // Given - гипотетический случай с форматированным номером
        String encrypted = "encrypted_4276-1234-5678-9012";
        when(encryptionService.decrypt(encrypted)).thenReturn("4276-1234-5678-9012");

        // When
        String result = cardMasker.mask(encrypted);

        // Then - последние 4 символа (включая дефис) будут видны
        assertThat(result).isEqualTo("**** **** **** 9012");
    }

    @Test
    @DisplayName("mask вызывается несколько раз → каждый раз расшифровывает заново")
    void mask_multipleCalls_shouldDecryptEachTime() throws Exception {
        // Given
        String encrypted = "encrypted_card";
        when(encryptionService.decrypt(encrypted))
                .thenReturn("4276123456789012")
                .thenReturn("5555123456789012");

        // When
        String result1 = cardMasker.mask(encrypted);
        String result2 = cardMasker.mask(encrypted);

        // Then
        assertThat(result1).isEqualTo("**** **** **** 9012");
        assertThat(result2).isEqualTo("**** **** **** 9012");
        verify(encryptionService, times(2)).decrypt(encrypted);
    }

    @Test
    @DisplayName("mask с очень длинным номером → показывает последние 4 цифры")
    void mask_withVeryLongCardNumber_shouldShowLastFour() throws Exception {
        // Given
        String longNumber = "1".repeat(100) + "5678";
        String encrypted = "encrypted_long";
        when(encryptionService.decrypt(encrypted)).thenReturn(longNumber);

        // When
        String result = cardMasker.mask(encrypted);

        // Then
        assertThat(result).isEqualTo("**** **** **** 5678");
    }

    // ==================== LOGGING VERIFICATION ====================

    @Test
    @DisplayName("mask с null → не вызывает encryptionService")
    void mask_withNull_shouldNotCallEncryptionService() {
        // When
        cardMasker.mask(null);

        // Then
        verifyNoInteractions(encryptionService);
    }

    @Test
    @DisplayName("mask с пустой строкой → не вызывает encryptionService")
    void mask_withEmpty_shouldNotCallEncryptionService() {
        // When
        cardMasker.mask("");

        // Then
        verifyNoInteractions(encryptionService);
    }

}