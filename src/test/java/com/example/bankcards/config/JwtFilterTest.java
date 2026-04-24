package com.example.bankcards.config;

import com.example.bankcards.security.JwtFilter;
import com.example.bankcards.security.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Юнит-тесты для {@link JwtFilter}.
 * <br>
 * Проверяют: вызов JwtUtils, UserDetailsService
 * и установку аутентификации в SecurityContext.
 */
@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    private JwtFilter jwtFilter;

    @BeforeEach
    void setUp() {
        jwtFilter = new JwtFilter(jwtUtils, userDetailsService);
        // Очищаем контекст перед каждым тестом
        SecurityContextHolder.clearContext();
    }

    /**
     * Валидный токен → аутентификация установлена
     */
    @Test
    void doFilterInternal_withValidToken_shouldSetAuthentication() throws Exception {
        // Given
        String token = "valid.jwt.token";
        String username = "testUser";
        UserDetails userDetails = new User(username, "", List.of());

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtils.validateToken(token)).thenReturn(true);
        when(jwtUtils.getUsernameFromToken(token)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);

        // When
        jwtFilter.doFilter(request, response, filterChain);

        // Then
        UsernamePasswordAuthenticationToken authentication =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(userDetails);
        assertThat(authentication.getAuthorities()).containsExactlyInAnyOrderElementsOf(userDetails.getAuthorities());
        verify(filterChain).doFilter(request, response);
    }

    /**
     * Отсутствует токен → запрос продолжается без аутентификации
     */
    @Test
    void doFilterInternal_withoutToken_shouldContinueChain() throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn(null);

        // When
        jwtFilter.doFilter(request, response, filterChain);

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtils, userDetailsService);
    }

    /**
     * Невалидный токен → запрос продолжается, аутентификация не установлена
     */
    @Test
    void doFilterInternal_withInvalidToken_shouldContinueChain() throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid.token");
        when(jwtUtils.validateToken("invalid.token")).thenReturn(false);

        // When
        jwtFilter.doFilter(request, response, filterChain);

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verify(jwtUtils).validateToken("invalid.token");
        verifyNoInteractions(userDetailsService);
    }

    /**
     * Ошибка в userDetailsService → не ломает запрос, логируется
     */
    @Test
    void doFilterInternal_whenUserDetailsServiceThrows_shouldContinueChain() throws Exception {
        // Given
        String token = "valid.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtils.validateToken(token)).thenReturn(true);
        when(jwtUtils.getUsernameFromToken(token)).thenReturn("unknown");
        when(userDetailsService.loadUserByUsername("unknown"))
                .thenThrow(new RuntimeException("User not found"));

        // When
        jwtFilter.doFilter(request, response, filterChain);

        // Then
        // Аутентификация НЕ установлена, но запрос продолжился
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    /**
     * Неверный формат заголовка (без "Bearer ") → игнорируется
     */
    @Test
    void doFilterInternal_withWrongAuthFormat_shouldIgnore() throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        // When
        jwtFilter.doFilter(request, response, filterChain);

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtils);
    }

}