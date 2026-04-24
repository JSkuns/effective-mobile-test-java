package com.example.bankcards.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Исключение, выбрасываемое при попытке перевода суммы, превышающей баланс карты.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST) // Возвращает 400 Bad Request
public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String message) {
        super(message);
    }

}