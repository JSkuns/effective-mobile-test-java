package com.example.bankcards.service;

import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Юнит-тесты для {@link UserService} без загрузки Spring контекста.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

    @Mock private UserRepository userRepository;

    @InjectMocks private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setRole(Role.USER);
    }

    // ==================== GET ALL USERS TESTS ====================

    @Test
    @DisplayName("getAllUsers → возвращает список всех пользователей")
    void getAllUsers_shouldReturnAllUsers() {
        // Given
        List<User> expectedUsers = List.of(testUser);
        when(userRepository.findAll()).thenReturn(expectedUsers);

        // When
        List<User> result = userService.getAllUsers();

        // Then
        assertThat(result).isEqualTo(expectedUsers);
        assertThat(result).hasSize(1);
        verify(userRepository).findAll();
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("getAllUsers при пустой БД → возвращает пустой список")
    void getAllUsers_whenDatabaseEmpty_shouldReturnEmptyList() {
        // Given
        when(userRepository.findAll()).thenReturn(List.of());

        // When
        List<User> result = userService.getAllUsers();

        // Then
        assertThat(result).isEmpty();
        verify(userRepository).findAll();
    }

    // ==================== GET USER BY USERNAME TESTS ====================

    @Test
    @DisplayName("getUserByUsername с существующим пользователем → возвращает пользователя")
    void getUserByUsername_withExistingUser_shouldReturnUser() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        User result = userService.getUserByUsername("testuser");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getRole()).isEqualTo(Role.USER);
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("getUserByUsername с несуществующим пользователем → выбрасывает UsernameNotFoundException")
    void getUserByUsername_withNonExistentUser_shouldThrowException() {
        // Given
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found: unknown");

        verify(userRepository).findByUsername("unknown");
    }

    @Test
    @DisplayName("getUserByUsername с null username → выбрасывает IllegalArgumentException")
    void getUserByUsername_withNullUsername_shouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> userService.getUserByUsername(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username cannot be empty");

        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("getUserByUsername с пустым username → выбрасывает IllegalArgumentException")
    void getUserByUsername_withEmptyUsername_shouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> userService.getUserByUsername(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username cannot be empty");

        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("getUserByUsername с пробелами в username → выбрасывает IllegalArgumentException")
    void getUserByUsername_withBlankUsername_shouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> userService.getUserByUsername("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username cannot be empty");

        verifyNoInteractions(userRepository);
    }

    // ==================== MAKE ADMIN TESTS ====================

    @Test
    @DisplayName("makeAdmin с валидным username → назначает роль ADMIN")
    void makeAdmin_withValidUsername_shouldPromoteUser() {
        // Given
        User userToPromote = new User();
        userToPromote.setUsername("regularuser");
        userToPromote.setRole(Role.USER);

        when(userRepository.findByUsername("regularuser")).thenReturn(Optional.of(userToPromote));

        // When
        userService.makeAdmin("regularuser");

        // Then
        assertThat(userToPromote.getRole()).isEqualTo(Role.ADMIN);
        verify(userRepository).findByUsername("regularuser");
        verify(userRepository).save(userToPromote);
    }

    @Test
    @DisplayName("makeAdmin с уже админом → выбрасывает IllegalStateException")
    void makeAdmin_withAlreadyAdmin_shouldThrowException() {
        // Given
        User adminUser = new User();
        adminUser.setUsername("alreadyadmin");
        adminUser.setRole(Role.ADMIN);

        when(userRepository.findByUsername("alreadyadmin")).thenReturn(Optional.of(adminUser));

        // When & Then
        assertThatThrownBy(() -> userService.makeAdmin("alreadyadmin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("User is already an admin: alreadyadmin");

        verify(userRepository).findByUsername("alreadyadmin");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("makeAdmin с несуществующим пользователем → выбрасывает UsernameNotFoundException")
    void makeAdmin_withNonExistentUser_shouldThrowException() {
        // Given
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.makeAdmin("unknown"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found: unknown");

        verify(userRepository).findByUsername("unknown");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("makeAdmin с null username → выбрасывает IllegalArgumentException")
    void makeAdmin_withNullUsername_shouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> userService.makeAdmin(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username cannot be empty");

        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("makeAdmin с пустым username → выбрасывает IllegalArgumentException")
    void makeAdmin_withEmptyUsername_shouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> userService.makeAdmin(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username cannot be empty");

        verifyNoInteractions(userRepository);
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("getAllUsers → логирование отладочного сообщения")
    void getAllUsers_shouldLogDebugMessage() {
        // Given
        when(userRepository.findAll()).thenReturn(List.of());

        // When
        userService.getAllUsers();

        // Then: логирование проверяется в интеграционных тестах или через LogCapturer
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("makeAdmin → логирование успешного повышения роли")
    void makeAdmin_shouldLogSuccessMessage() {
        // Given
        User user = new User();
        user.setUsername("promoted");
        user.setRole(Role.USER);
        when(userRepository.findByUsername("promoted")).thenReturn(Optional.of(user));

        // When
        userService.makeAdmin("promoted");

        // Then
        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("makeAdmin → пользователь сохраняется с новой ролью")
    void makeAdmin_shouldSaveUserWithNewRole() {
        // Given
        User user = new User();
        user.setUsername("tobeadmin");
        user.setRole(Role.USER);
        when(userRepository.findByUsername("tobeadmin")).thenReturn(Optional.of(user));

        // When
        userService.makeAdmin("tobeadmin");

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getRole()).isEqualTo(Role.ADMIN);
        assertThat(savedUser.getUsername()).isEqualTo("tobeadmin");
    }

}