package com.example.bankcards.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

/**
 * Утилита для работы с JWT-токенами (генерация, валидация, извлечение данных).
 * <br>
 * Использует библиотеку JJWT (io.jsonwebtoken) с алгоритмом подписи
 * HMAC-SHA. Секретный ключ и время жизни токена конфигурируются через application.properties.
 *
 * @see <a href="https://github.com/jwtk/jjwt">JJWT Library</a>
 */
@Component
public class JwtUtils {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    /**
     * Валидирует JWT-токен: проверяет подпись, срок действия и формат.
     *
     * @param token строка токена для проверки
     * @return {@code true}, если токен валиден; {@code false} в противном случае
     * @throws IllegalArgumentException если токен {@code null} или пустой
     */
    public boolean validateToken(@Nullable String token) {
        if (token == null || token.isBlank()) {
            logger.warn("Token is null or empty");
            return false;
        }
        try {
            Jwts.parser()
                    .verifyWith(getSignInKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            logger.warn("JWT validation failed: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid token argument: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Извлекает имя пользователя (subject) из валидного JWT-токена.
     *
     * @param token строка токена
     * @return имя пользователя (поле {@code sub}), или {@code null} если токен невалиден
     * @throws IllegalArgumentException если токен {@code null} или пустой
     */
    @Nullable
    public String getUsernameFromToken(@NonNull String token) {
        try {
            return getClaimFromToken(token, Claims::getSubject);
        } catch (Exception e) {
            logger.warn("Failed to extract username from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Извлекает произвольный claim из токена с помощью функции-резолвера.
     *
     * @param token строка токена
     * @param claimsResolver функция для извлечения нужного значения из {@link Claims}
     * @param <T> тип возвращаемого значения
     * @return извлечённое значение, или {@code null} если токен невалиден
     */
    @Nullable
    private <T> T getClaimFromToken(
            @NonNull String token,
            @NonNull Function<Claims, T> claimsResolver) {
        try {
            final Claims claims = getAllClaimsFromToken(token);
            return claimsResolver.apply(claims);
        } catch (Exception e) {
            logger.debug("Failed to resolve claim: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Парсит токен и возвращает все claims (payload).
     *
     * @param token строка токена
     * @return объект {@link Claims} с данными токена
     * @throws JwtException если токен невалиден или подпись не совпадает
     */
    @NonNull
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Создаёт {@link SecretKey} из base64-строки секрета.
     *
     * @return готовый ключ для подписи/верификации токенов
     * @throws IllegalArgumentException если секрет слишком короткий (&lt; 256 бит для HMAC-SHA)
     */
    @NonNull
    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Генерирует новый JWT-токен для пользователя с дополнительными claims.
     *
     * @param username имя пользователя (будет записано в поле {@code sub})
     * @param claims дополнительные данные для токена (например, роли, permissions)
     * @return сгенерированный JWT-токен в строковом формате
     * @throws IllegalArgumentException если username {@code null} или пустой
     */
    @NonNull
    public String generateToken(
            @NonNull String username,
            @Nullable Map<String, Object> claims) {
        if (username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        Date expiration = new Date(System.currentTimeMillis() + jwtExpirationMs);
        logger.debug("Generating token for user '{}' with expiration: {}", username, expiration);
        return buildToken(username, claims, expiration);
    }

    /**
     * Внутренний метод сборки токена с использованием JJWT Builder API.
     *
     * @param username имя пользователя для поля {@code sub}
     * @param claims дополнительные claims (могут быть {@code null})
     * @param expiration дата истечения срока действия токена
     * @return скомпилированный JWT-токен
     */
    @NonNull
    private String buildToken(
            @NonNull String username,
            @Nullable Map<String, Object> claims,
            @NonNull Date expiration) {
        JwtBuilder builder = Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(expiration)
                .signWith(getSignInKey());

        if (claims != null && !claims.isEmpty()) {
            builder.claims(claims);
        }

        return builder.compact();
    }

}