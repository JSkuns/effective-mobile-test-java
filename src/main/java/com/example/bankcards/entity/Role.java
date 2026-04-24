package com.example.bankcards.entity;

import lombok.Getter;

/**
 * Роли пользователей в системе.
 * Используется в сущности User для определения прав доступа.
 */
@Getter
public enum Role {

    USER("Пользователь"),   // Обычный пользователь: может работать только со своими картами
    ADMIN("Администратор"); // Админ: может управлять всеми картами и пользователями

    private final String description;

    Role(String description) {
        this.description = description;
    }

    /**
     * Метод для получения роли по строке (case-insensitive).
     * Полезно при десериализации из JSON или БД.
     */
    public static Role fromString(String role) {
        for (Role r : Role.values()) {
            if (r.name().equalsIgnoreCase(role)) {
                return r;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + role);
    }

}