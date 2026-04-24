package com.example.bankcards.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Конфигурация CORS (Cross-Origin Resource Sharing) для Spring Boot.
 * <br>
 * Разрешает кросс-доменные запросы от фронтенда к REST API.
 * Настройки адаптированы для JWT-аутентификации (с передачей заголовков авторизации).
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS">CORS Documentation</a>
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = getCorsConfiguration();

        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }

    private static CorsConfiguration getCorsConfiguration() {
        CorsConfiguration config = new CorsConfiguration();

        // Разрешенные источники
        // TODO: указать конкретные домены для production (можно вынести в конфигурацию application-prod.yml)!
        config.addAllowedOriginPattern("http://localhost:8080/");

        // Разрешенные заголовки
        config.addAllowedHeader("*");

        // Разрешенные методы
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("OPTIONS");
        return config;
    }

}