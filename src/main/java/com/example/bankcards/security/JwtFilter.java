package com.example.bankcards.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Фильтр, который проверяет JWT-токен в заголовке Authorization.
 * Если токен валиден — устанавливает аутентификацию в SecurityContext.
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtFilter.class);

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    /**
     * Конструктор
     *
     * @param jwtUtils утилита для работы с JWT (валидация, извлечение данных)
     * @param userDetailsService сервис для загрузки данных пользователя
     */
    public JwtFilter(JwtUtils jwtUtils, UserDetailsService userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Основной метод фильтрации запросов.
     * <br>
     * Алгоритм работы:
     * <ol>
     *   <li>Извлекает JWT из заголовка {@code Authorization: Bearer <token>}</li>
     *   <li>Если токен присутствует и валиден — загружает пользователя</li>
     *   <li>Создаёт {@link UsernamePasswordAuthenticationToken} и устанавливает в контекст</li>
     *   <li>Продолжает цепочку фильтров независимо от результата</li>
     * </ol>
     *
     * @param request текущий HTTP-запрос
     * @param response текущий HTTP-ответ
     * @param filterChain цепочка фильтров для продолжения обработки
     * @throws ServletException если произошла ошибка сервлета
     * @throws IOException если произошла ошибка ввода-вывода
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);

            if (jwt != null && jwtUtils.validateToken(jwt)) {
                String username = jwtUtils.getUsernameFromToken(jwt);
                logger.debug("JWT validated for user: {}", username);

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);
                logger.debug("Authentication set for user: {}", username);
            }
        } catch (Exception ex) {
            logger.warn("JWT authentication failed: {}", ex.getMessage());
            logger.debug("Details:", ex);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Извлекает JWT-токен из заголовка {@code Authorization}.
     * <br>
     * Ожидает формат: {@code Bearer <token>}. Регистр префикса не чувствителен.
     *
     * @param request HTTP-запрос
     * @return токен без префикса {@code "Bearer "}, или {@code null}, если не найден
     */
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        return null;
    }

}