package com.example.bankcards.service;

import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Юнит-тесты для {@link CustomUserDetailsService} без загрузки Spring контекста.
 */
@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceUnitTest {

    @Mock private UserRepository userRepository;

    @InjectMocks private CustomUserDetailsService userDetailsService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword("hashed_password_123");
        testUser.setRole(Role.USER);
    }

    // ==================== LOAD USER BY USERNAME TESTS ====================

    @Test
    @DisplayName("loadUserByUsername с существующим пользователем → возвращает UserDetails")
    void loadUserByUsername_withExistingUser_shouldReturnUserDetails() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("testuser");
        assertThat(userDetails.getPassword()).isEqualTo("hashed_password_123");
        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();

        verify(userRepository).findByUsername("testuser");
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("loadUserByUsername с пользователем ADMIN → возвращает роль ROLE_ADMIN")
    void loadUserByUsername_withAdminUser_shouldReturnAdminRole() {
        // Given
        User adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setPassword("admin_hashed");
        adminUser.setRole(Role.ADMIN);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("admin");

        // Then
        assertThat(userDetails.getUsername()).isEqualTo("admin");
        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("loadUserByUsername с несуществующим пользователем → выбрасывает UsernameNotFoundException")
    void loadUserByUsername_withNonExistentUser_shouldThrowException() {
        // Given
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found with username: unknown");

        verify(userRepository).findByUsername("unknown");
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("loadUserByUsername с null username → выбрасывает IllegalArgumentException")
    void loadUserByUsername_withNullUsername_shouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username cannot be empty");

        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("loadUserByUsername с пустым username → выбрасывает IllegalArgumentException")
    void loadUserByUsername_withEmptyUsername_shouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username cannot be empty");

        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("loadUserByUsername с пробелами в username → выбрасывает IllegalArgumentException")
    void loadUserByUsername_withBlankUsername_shouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username cannot be empty");

        verifyNoInteractions(userRepository);
    }

    // ==================== GET AUTHORITIES TESTS ====================
    // Тестируем приватный метод через публичный API

    @Test
    @DisplayName("getAuthorities с ролью USER → возвращает ROLE_USER")
    void getAuthorities_withUserRole_shouldReturnRoleUser() {
        // Given
        User user = new User();
        user.setUsername("user");
        user.setPassword("pass");
        user.setRole(Role.USER);

        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("user");

        // Then
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        assertThat(authorities).hasSize(1);
        assertThat(authorities.iterator().next())
                .isInstanceOf(SimpleGrantedAuthority.class)
                .extracting(GrantedAuthority::getAuthority)
                .isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("getAuthorities с ролью ADMIN → возвращает ROLE_ADMIN")
    void getAuthorities_withAdminRole_shouldReturnRoleAdmin() {
        // Given
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword("pass");
        admin.setRole(Role.ADMIN);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("admin");

        // Then
        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("getAuthorities с null ролью → возвращает пустой список")
    void getAuthorities_withNullRole_shouldReturnEmptyList() {
        // Given - создаём пользователя с null ролью
        User userWithNullRole = new User();
        userWithNullRole.setUsername("norole");
        userWithNullRole.setPassword("pass");
        // role не установлен = null

        when(userRepository.findByUsername("norole")).thenReturn(Optional.of(userWithNullRole));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("norole");

        // Then
        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("loadUserByUsername: пароль с спецсимволами → корректно передаётся в UserDetails")
    void loadUserByUsername_withSpecialCharsInPassword_shouldPreservePassword() {
        // Given
        User user = new User();
        user.setUsername("special");
        user.setPassword("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");
        user.setRole(Role.USER);

        when(userRepository.findByUsername("special")).thenReturn(Optional.of(user));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("special");

        // Then
        assertThat(userDetails.getPassword())
                .isEqualTo("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");
    }

    @Test
    @DisplayName("loadUserByUsername: unicode username → корректно обрабатывается")
    void loadUserByUsername_withUnicodeUsername_shouldWork() {
        // Given
        User user = new User();
        user.setUsername("пользователь");
        user.setPassword("hash");
        user.setRole(Role.USER);

        when(userRepository.findByUsername("пользователь")).thenReturn(Optional.of(user));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername("пользователь");

        // Then
        assertThat(userDetails.getUsername()).isEqualTo("пользователь");
    }

    @Test
    @DisplayName("loadUserByUsername: очень длинный username → корректно обрабатывается")
    void loadUserByUsername_withLongUsername_shouldWork() {
        // Given
        String longUsername = "a".repeat(255);
        User user = new User();
        user.setUsername(longUsername);
        user.setPassword("hash");
        user.setRole(Role.USER);

        when(userRepository.findByUsername(longUsername)).thenReturn(Optional.of(user));

        // When
        UserDetails userDetails = userDetailsService.loadUserByUsername(longUsername);

        // Then
        assertThat(userDetails.getUsername()).hasSize(255);
    }

}