package com.example.bankcards.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Исключение, выбрасываемое когда карта не найдена или пользователь не имеет к ней доступа.
 */
@ResponseStatus(HttpStatus.NOT_FOUND) // Автоматически возвращает 404
public class CardNotFoundException extends RuntimeException {

    public CardNotFoundException(String message) {
        super(message);
    }

}