package com.example.bankcards.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Исключение, выбрасываемое когда пользователь пытается получить доступ к ресурсу,
 * который ему не принадлежит (например, чужая карта).
 */
@ResponseStatus(HttpStatus.FORBIDDEN) // Возвращает 403 Forbidden
public class UnauthorizedAccessException extends RuntimeException {

    public UnauthorizedAccessException(String message) {
        super(message);
    }

}