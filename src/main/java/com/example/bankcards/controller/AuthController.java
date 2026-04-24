package com.example.bankcards.controller;

import com.example.bankcards.dto.request.LoginRequest;
import com.example.bankcards.dto.response.JwtResponse;
import com.example.bankcards.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер для операций аутентификации и регистрации пользователей.
 * <br>
 * Предоставляет эндпоинты для входа в систему и создания новых учётных записей.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Аутентифицирует пользователя и возвращает JWT-токен.
     *
     * @param request данные для входа (логин и пароль)
     * @return {@code 200 OK} с JWT-токеном и информацией о пользователе при успешной аутентификации
     * @throws org.springframework.security.core.AuthenticationException если учётные данные неверны
     */
    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Регистрирует нового пользователя в системе.
     * <br>
     * После успешной регистрации пользователь получает роль {@code ROLE_USER}
     * и может войти в систему, используя предоставленные учётные данные.
     *
     * @param request данные для регистрации
     * @return {@code 201 Created} при успешном создании пользователя
     * @throws RuntimeException если пользователь с таким логином уже существует
     */
    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody LoginRequest request) {
        authService.register(request);
        return ResponseEntity.status(201).build();
    }

}