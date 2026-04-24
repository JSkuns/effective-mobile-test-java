package com.example.bankcards.service;

import com.example.bankcards.security.JwtUtils;
import com.example.bankcards.dto.request.LoginRequest;
import com.example.bankcards.dto.response.JwtResponse;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.UnauthorizedAccessException;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Сервис для операций аутентификации и регистрации пользователей.
 * <br>
 * Отвечает за:
 * <ul>
 *   <li>Аутентификацию пользователя по логину и паролю</li>
 *   <li>Генерацию и возврат JWT-токена после успешного входа</li>
 *   <li>Регистрацию новых пользователей с ролью {@code ROLE_USER}</li>
 * </ul>
 * <br>
 * <b>Безопасность:</b> пароли никогда не хранятся в открытом виде —
 * используется {@link PasswordEncoder} с алгоритмом BCrypt.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    /**
     * Аутентифицирует пользователя и генерирует JWT-токен.
     * <p>
     * Алгоритм работы:
     * <ol>
     *   <li>Проверяет учётные данные через {@link AuthenticationManager}</li>
     *   <li>Загружает пользователя из БД для получения роли</li>
     *   <li>Генерирует JWT с claims: username, role</li>
     *   <li>Возвращает токен в ответе</li>
     * </ol>
     * </p>
     *
     * @param request данные для входа (логин и пароль)
     * @return {@link JwtResponse} с сгенерированным токеном
     * @throws UnauthorizedAccessException если пользователь не найден после аутентификации
     * @throws IllegalArgumentException    если запрос содержит пустые поля
     */
    public JwtResponse login(LoginRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank() ||
                request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Username and password cannot be empty");
        }

        // Аутентифицируем через Spring Security
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Загружаем пользователя для получения роли
        String username = authentication.getName();
        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() -> new UnauthorizedAccessException(
                        "User not found after authentication: " + username));

        // Формируем claims для токена
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());

        String token = jwtUtils.generateToken(username, claims);

        return new JwtResponse(token);
    }

    /**
     * Регистрирует нового пользователя в системе.
     * <br>
     * Новый пользователь автоматически получает роль {@code ROLE_USER}.
     * Пароль хешируется перед сохранением в БД.
     *
     * @param request данные для регистрации (логин и пароль)
     * @throws IllegalArgumentException если запрос содержит пустые поля
     * @throws IllegalStateException    если пользователь с таким логином уже существует
     */
    @Transactional
    public void register(LoginRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank() ||
                request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Username and password cannot be empty");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalStateException("Username already taken: " + request.getUsername());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);

        userRepository.save(user);
    }

}