package com.example.bankcards.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Стандартизированный ответ об ошибке для REST API.
 * Используется в GlobalExceptionHandler для возврата единообразных JSON-ошибок.
 */
@Data
@AllArgsConstructor // ← ВАЖНО: генерирует конструктор со всеми полями
public class ErrorResponse {

    private LocalDateTime timestamp;      // Время возникновения ошибки
    private int status;                   // HTTP статус код
    private String error;                 // Краткое название ошибки
    private String message;               // Детальное сообщение об ошибке
    private String path;                  // URL запроса, который вызвал ошибку

    /**
     * Удобный конструктор без timestamp — он будет установлен автоматически.
     */
    public ErrorResponse(int status, String error, String message, String path) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

}