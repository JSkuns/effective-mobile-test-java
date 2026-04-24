package com.example.bankcards.entity;

import lombok.Getter;

/**
 * Статусы банковской карты.
 * Используется в сущности Card для отслеживания жизненного цикла карты.
 */
@Getter
public enum CardStatus {

    ACTIVE("Активна"),           // Карта активна и готова к использованию
    BLOCKED("Заблокирована"),    // Карта заблокирована пользователем или админом
    EXPIRED("Истек срок");       // Срок действия карты истёк

    private final String description;

    CardStatus(String description) {
        this.description = description;
    }

}