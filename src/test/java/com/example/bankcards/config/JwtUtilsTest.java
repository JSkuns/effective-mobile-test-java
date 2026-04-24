package com.example.bankcards.config;

import com.example.bankcards.security.JwtUtils;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit-тесты для {@link JwtUtils} без загрузки Spring контекста.
 */
class JwtUtilsTest {

    private JwtUtils jwtUtils;
    private SecretKey testKey;

    @BeforeEach
    void setUp() {
        String testSecret = "dGhpcy1pcy1hLXNlY3JldC1rZXktZm9yLXRlc3RpbmctcHVycG9zZXMtb25seQ==";
        long testExpirationMs = 3_600_000; // 1 час
        testKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(testSecret));

        jwtUtils = new JwtUtils();
        setPrivateField(jwtUtils, "jwtSecret", testSecret);
        setPrivateField(jwtUtils, "jwtExpirationMs", testExpirationMs);
    }

    @Test
    void generateAndValidateToken() {
        // Given
        String username = "testUser";
        Map<String, Object> claims = Map.of("role", "ADMIN");

        // When
        String token = jwtUtils.generateToken(username, claims);
        boolean isValid = jwtUtils.validateToken(token);
        String extractedUsername = jwtUtils.getUsernameFromToken(token);

        // Then
        assertThat(isValid).isTrue();
        assertThat(extractedUsername).isEqualTo(username);
    }

    @Test
    void expiredToken_shouldBeInvalid() throws InterruptedException {
        // Given: создаём токен с истёкшим сроком вручную
        String expiredToken = Jwts.builder()
                .subject("testUser")
                .issuedAt(new Date(System.currentTimeMillis() - 10_000))
                .expiration(new Date(System.currentTimeMillis() - 1_000)) // уже истёк
                .signWith(testKey)
                .compact();

        // When & Then
        assertThat(jwtUtils.validateToken(expiredToken)).isFalse();
    }

    @Test
    void tokenWithWrongSignature_shouldBeInvalid() {
        // Given: токен, подписанный другим ключом
        SecretKey wrongKey = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode("YW5vdGhlci1zZWNyZXQta2V5LWZvci10ZXN0aW5nLXB1cnBvc2Vz")
        );

        String wrongToken = Jwts.builder()
                .subject("testUser")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(wrongKey)
                .compact();

        // When & Then
        assertThat(jwtUtils.validateToken(wrongToken)).isFalse();
    }

    @Test
    void validateToken_withNullToken_shouldReturnFalse() {
        assertThat(jwtUtils.validateToken(null)).isFalse();
    }

    @Test
    void validateToken_withEmptyToken_shouldReturnFalse() {
        assertThat(jwtUtils.validateToken("")).isFalse();
    }

    @Test
    void validateToken_withMalformedToken_shouldReturnFalse() {
        assertThat(jwtUtils.validateToken("not.a.valid.jwt")).isFalse();
    }

    @Test
    void generateToken_withEmptyUsername_shouldThrowException() {
        assertThatThrownBy(() -> jwtUtils.generateToken("", Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ==================== Вспомогательные методы ====================

    /**
     * Устанавливает значение приватного поля через рефлексию (для тестов).
     */
    private void setPrivateField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }

}