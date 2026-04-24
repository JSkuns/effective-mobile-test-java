package com.example.bankcards.service;

import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Сервис для управления пользователями системы.
 * <br>
 * Предоставляет операции для:
 * <ul>
 *   <li>Получения списка всех пользователей</li>
 *   <li>Поиска пользователя по имени (логину)</li>
 *   <li>Изменения роли пользователя (повышение до администратора)</li>
 * </ul>
 * <br>
 * <b>Безопасность:</b> все методы, изменяющие данные, должны вызываться
 * только из контроллеров с проверкой прав доступа (например, через {@code @PreAuthorize}).
 * <br>
 * <b>Транзакционность:</b> класс помечен {@code @Transactional(readOnly = true)},
 * поэтому все публичные методы по умолчанию выполняются в режиме только для чтения.
 * Методы с модификацией данных переопределяют это поведение.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    /**
     * Возвращает список всех пользователей системы.
     * <br>
     * <b>Внимание:</b> метод возвращает сущности {@link User} напрямую.
     * Для защиты чувствительных данных (пароли, персональная информация)
     * рекомендуется маппить результат в DTO перед возвратом в контроллер.
     *
     * @return список всех пользователей (может быть пустым, но не {@code null})
     */
    public List<User> getAllUsers() {
        logger.debug("Fetching all users");
        return userRepository.findAll();
    }

    /**
     * Находит пользователя по имени (логину).
     *
     * @param username имя пользователя для поиска
     * @return найденный пользователь
     * @throws UsernameNotFoundException если пользователь с таким именем не найден
     * @throws IllegalArgumentException  если {@code username} равен {@code null} или пустой
     */
    public User getUserByUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }

        logger.debug("Looking for user by username: {}", username);

        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.warn("User not found with username: {}", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });
    }

    /**
     * Назначает роль {@code ADMIN} указанному пользователю.
     * <br>
     * <b>Требования безопасности:</b> этот метод должен вызываться только
     * из эндпоинтов, защищённых проверкой прав (например, {@code @PreAuthorize("hasRole('ADMIN')")}).
     * Сам сервис не проверяет права вызывающего пользователя.
     *
     * @param username имя пользователя, которому нужно назначить роль администратора
     * @throws UsernameNotFoundException   если пользователь не найден
     * @throws IllegalStateException       если пользователь уже является администратором
     * @throws IllegalArgumentException    если {@code username} равен {@code null} или пустой
     */
    @Transactional
    public void makeAdmin(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }

        logger.info("Attempting to promote user '{}' to ADMIN role", username);

        User user = getUserByUsername(username);

        if (user.getRole() == Role.ADMIN) {
            logger.debug("User '{}' is already an admin", username);
            throw new IllegalStateException("User is already an admin: " + username);
        }

        user.setRole(Role.ADMIN);
        userRepository.save(user);

        logger.info("Successfully promoted user '{}' to ADMIN role", username);
    }

}