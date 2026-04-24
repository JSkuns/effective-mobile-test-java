package com.example.bankcards.service;

import com.example.bankcards.security.JwtUtils;
import com.example.bankcards.dto.request.LoginRequest;
import com.example.bankcards.dto.response.JwtResponse;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.UnauthorizedAccessException;
import com.example.bankcards.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Юнит-тесты для {@link AuthService} без загрузки Spring контекста.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceUnitTest {

    @Mock private UserRepository userRepository;
    @Mock private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtils jwtUtils;

    @InjectMocks private AuthService authService;

    private LoginRequest validLoginRequest;
    private LoginRequest validRegisterRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        validLoginRequest = new LoginRequest();
        validLoginRequest.setUsername("testuser");
        validLoginRequest.setPassword("password123");
        validRegisterRequest = new LoginRequest();
        validRegisterRequest.setUsername("newuser");
        validRegisterRequest.setPassword("newpass456");

        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword("hashed_password");
        testUser.setRole(Role.USER);
    }

    // ==================== LOGIN TESTS ====================

    @Test
    @DisplayName("login с валидными данными → возвращает JwtResponse с токеном")
    void login_withValidCredentials_shouldReturnJwtResponse() {
        // Given
        String expectedToken = "mock.jwt.token";
        Authentication mockAuthentication = mock(Authentication.class);
        when(mockAuthentication.getName()).thenReturn("testuser");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuthentication);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(jwtUtils.generateToken(eq("testuser"), any())).thenReturn(expectedToken);

        // When
        JwtResponse response = authService.login(validLoginRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo(expectedToken);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByUsername("testuser");
        verify(jwtUtils).generateToken(eq("testuser"), any());
    }

    @Test
    @DisplayName("login с неверным паролем → выбрасывает BadCredentialsException")
    void login_withInvalidPassword_shouldThrowBadCredentialsException() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // When & Then
        assertThatThrownBy(() -> authService.login(validLoginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verifyNoInteractions(userRepository, jwtUtils);
    }

    @Test
    @DisplayName("login: пользователь не найден после аутентификации → UnauthorizedAccessException")
    void login_userNotFoundAfterAuth_shouldThrowUnauthorizedAccessException() {
        // Given
        Authentication mockAuthentication = mock(Authentication.class);
        when(mockAuthentication.getName()).thenReturn("testuser");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuthentication);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.login(validLoginRequest))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessageContaining("User not found after authentication");

        verify(userRepository).findByUsername("testuser");
        verifyNoInteractions(jwtUtils);
    }

    @Test
    @DisplayName("login с пустым username → выбрасывает IllegalArgumentException")
    void login_withEmptyUsername_shouldThrowIllegalArgumentException() {
        // Given
        LoginRequest emptyRequest = new LoginRequest();

        // When & Then
        assertThatThrownBy(() -> authService.login(emptyRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username and password cannot be empty");

        verifyNoInteractions(authenticationManager, userRepository, jwtUtils);
    }

    @Test
    @DisplayName("login с пустым password → выбрасывает IllegalArgumentException")
    void login_withEmptyPassword_shouldThrowIllegalArgumentException() {
        // Given
        LoginRequest emptyRequest = new LoginRequest();

        // When & Then
        assertThatThrownBy(() -> authService.login(emptyRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username and password cannot be empty");

        verifyNoInteractions(authenticationManager, userRepository, jwtUtils);
    }

    // ==================== REGISTER TESTS ====================

    @Test
    @DisplayName("register с новым пользователем → сохраняет пользователя с ролью USER")
    void register_withNewUser_shouldSaveUserWithUserRole() {
        // Given
        String encodedPassword = "encoded_password";
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("newpass456")).thenReturn(encodedPassword);

        // When
        authService.register(validRegisterRequest);

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo("newuser");
        assertThat(savedUser.getPassword()).isEqualTo(encodedPassword);
        assertThat(savedUser.getRole()).isEqualTo(Role.USER);

        verify(passwordEncoder).encode("newpass456");
    }

    @Test
    @DisplayName("register с существующим username → выбрасывает IllegalStateException")
    void register_withExistingUsername_shouldThrowIllegalStateException() {
        // Given
        when(userRepository.existsByUsername("newuser")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.register(validRegisterRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Username already taken: newuser");

        verify(userRepository).existsByUsername("newuser");
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("register с пустым username → выбрасывает IllegalArgumentException")
    void register_withEmptyUsername_shouldThrowIllegalArgumentException() {
        // Given
        LoginRequest emptyRequest = new LoginRequest();

        // When & Then
        assertThatThrownBy(() -> authService.register(emptyRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username and password cannot be empty");

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("register с пустым password → выбрасывает IllegalArgumentException")
    void register_withEmptyPassword_shouldThrowIllegalArgumentException() {
        // Given
        LoginRequest emptyRequest = new LoginRequest();

        // When & Then
        assertThatThrownBy(() -> authService.register(emptyRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username and password cannot be empty");

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("register → пароль хешируется перед сохранением")
    void register_shouldEncodePasswordBeforeSaving() {
        // Given
        String rawPassword = "newpass456";
        String encodedPassword = "$2a$10$encoded";
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);

        // When
        authService.register(validRegisterRequest);

        // Then
        verify(passwordEncoder).encode(rawPassword);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo(encodedPassword);
    }

}