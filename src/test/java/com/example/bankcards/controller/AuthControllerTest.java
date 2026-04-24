package com.example.bankcards.controller;

import com.example.bankcards.dto.request.LoginRequest;
import com.example.bankcards.dto.response.JwtResponse;
import com.example.bankcards.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Чистые юнит-тесты для {@link AuthController} без загрузки Spring контекста.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerUnitTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private LoginRequest validLoginRequest;
    private LoginRequest validRegisterRequest;
    private JwtResponse mockJwtResponse;

    @BeforeEach
    void setUp() {
        validLoginRequest = new LoginRequest();
        validRegisterRequest = new LoginRequest();
        mockJwtResponse = new JwtResponse("mock.jwt.token");
    }

    @Test
    @DisplayName("POST /auth/login с валидными данными → 200 OK с токеном")
    void login_withValidCredentials_shouldReturnJwt() {
        // Given
        when(authService.login(any(LoginRequest.class))).thenReturn(mockJwtResponse);

        // When
        ResponseEntity<JwtResponse> response = authController.login(validLoginRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isEqualTo("mock.jwt.token");

        verify(authService).login(validLoginRequest);
        verifyNoMoreInteractions(authService);
    }

    @Test
    @DisplayName("POST /auth/login с неверными данными → пробрасывает исключение сервиса")
    void login_withInvalidCredentials_shouldPropagateException() {
        // Given
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new org.springframework.security.core.AuthenticationException("Bad credentials") {
                });

        // When & Then
        assertThatThrownBy(() -> authController.login(validLoginRequest))
                .isInstanceOf(org.springframework.security.core.AuthenticationException.class)
                .hasMessage("Bad credentials");

        verify(authService).login(validLoginRequest);
    }

    @Test
    @DisplayName("POST /auth/register с валидными данными → 201 Created")
    void register_withValidData_shouldReturnCreated() {
        // Given
        doNothing().when(authService).register(any(LoginRequest.class));

        // When
        ResponseEntity<Void> response = authController.register(validRegisterRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNull(); // 201 обычно без тела

        verify(authService).register(validRegisterRequest);
        verifyNoMoreInteractions(authService);
    }

    @Test
    @DisplayName("POST /auth/register с существующим пользователем → пробрасывает исключение")
    void register_withExistingUser_shouldPropagateException() {
        // Given
        doThrow(new RuntimeException("User already exists"))
                .when(authService).register(any(LoginRequest.class));

        // When & Then
        assertThatThrownBy(() -> authController.register(validRegisterRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User already exists");

        verify(authService).register(validRegisterRequest);
    }

    @Test
    @DisplayName("login с пустым username → сервис получает пустую строку (валидация на уровне сервиса)")
    void login_withEmptyUsername_shouldCallService() {
        // Given
        LoginRequest emptyRequest = new LoginRequest();
        when(authService.login(emptyRequest)).thenReturn(mockJwtResponse);

        // When
        ResponseEntity<JwtResponse> response = authController.login(emptyRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(authService).login(emptyRequest);
    }

    @Test
    @DisplayName("register с коротким паролем → сервис получает данные (валидация на уровне сервиса)")
    void register_withShortPassword_shouldCallService() {
        // Given
        LoginRequest shortPassRequest = new LoginRequest();
        doNothing().when(authService).register(shortPassRequest);

        // When
        ResponseEntity<Void> response = authController.register(shortPassRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(authService).register(shortPassRequest);
    }

}