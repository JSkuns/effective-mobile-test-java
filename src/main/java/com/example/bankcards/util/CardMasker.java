package com.example.bankcards.util;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Утилита для безопасного маскирования номера банковской карты.
 * <br>
 * Предназначена для отображения номера карты в пользовательских интерфейсах
 * и логах, скрывая чувствительные данные, но оставляя последние 4 цифры
 * для идентификации карты пользователем.
 * <br>
 * <b>Формат маскирования:</b>
 * <ul>
 *   <li>Стандартные карты (16 цифр): {@code **** **** **** 5678}</li>
 *   <li>Короткие карты (&lt;16 цифр): {@code **** **** *XX} (последние 4 видимы)</li>
 *   <li>Пустые/null значения: {@code ****}</li>
 * </ul>
 * <br>
 * <b>Важно:</b> метод {@link #mask(String)} принимает <b>зашифрованный</b> номер карты
 * и временно расшифровывает его для извлечения последних 4 цифр.
 * Нужно, чтобы результат не логировался и не сохранялся.
 */
@Component 
@RequiredArgsConstructor
public class CardMasker {

    private static final Logger logger = LoggerFactory.getLogger(CardMasker.class);

    private final EncryptionService encryptionService;

    /**
     * Маскирует зашифрованный номер банковской карты.
     * <br>
     * Алгоритм работы:
     * <ol>
     *   <li>Проверяет входные данные на {@code null} или пустоту</li>
     *   <li>Расшифровывает номер карты через {@link EncryptionService}</li>
     *   <li>Извлекает последние 4 цифры</li>
     *   <li>Формирует строку в формате {@code **** **** **** XXXX}</li>
     * </ol>
     *
     * @param encryptedCardNumber зашифрованный номер карты (Base64-encoded AES ciphertext)
     * @return маскированная строка для безопасного отображения
     * @implNote При ошибке расшифровки возвращает {@code "**** **** **** ****"} без раскрытия деталей ошибки
     */
    public String mask(String encryptedCardNumber) {
        if (encryptedCardNumber == null || encryptedCardNumber.isBlank()) {
            logger.debug("Masking null or empty card number");
            return "****";
        }

        try {
            String decrypted = encryptionService.decrypt(encryptedCardNumber);

            // Защита от слишком коротких номеров
            if (decrypted.length() < 4) {
                logger.warn("Card number too short ({} chars), returning as-is", decrypted.length());
                return decrypted;
            }

            // Извлекаем последние 4 цифры
            String lastFour = decrypted.substring(decrypted.length() - 4);
            return "**** **** **** " + lastFour;

        } catch (Exception e) {
            // Не раскрываем детали ошибки пользователю
            logger.error("Failed to mask card number: {}", e.getMessage());
            return "**** **** **** ****";
        }
    }

}