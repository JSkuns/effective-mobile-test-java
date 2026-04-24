package com.example.bankcards.security;

import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;

/**
 * Реализация {@link UserDetailsService} для загрузки данных пользователя из базы данных.
 * <br>
 * Используется Spring Security при аутентификации для преобразования имени пользователя
 * в объект {@link UserDetails}, содержащий учётные данные и права доступа.
 * <br>
 * <b>Важно:</b> роль пользователя автоматически преобразуется в формат {@code ROLE_XXX},
 * требуемый Spring Security для работы аннотаций {@code @PreAuthorize} и выражений доступа.
 *
 * @see UserDetailsService
 * @see UserDetails
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserRepository userRepository;

    /**
     * Загружает данные пользователя по имени для аутентификации.
     * <br>
     * Алгоритм работы:
     * <ol>
     *   <li>Поиск пользователя в БД по {@code username}</li>
     *   <li>Если не найден — выбрасывается {@link UsernameNotFoundException}</li>
     *   <li>Преобразование сущности {@link User} в {@link UserDetails}</li>
     *   <li>Маппинг роли в {@link GrantedAuthority} с префиксом {@code ROLE_}</li>
     * </ol>
     *
     * @param username имя пользователя для поиска (логин)
     * @return {@link UserDetails} с данными для аутентификации
     * @throws UsernameNotFoundException если пользователь с таким именем не найден
     * @throws IllegalArgumentException  если {@code username} равен {@code null} или пустой
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }

        logger.debug("Loading user details for username: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.warn("User not found with username: {}", username);
                    return new UsernameNotFoundException("User not found with username: " + username);
                });

        logger.debug("Successfully loaded user: {}", user.getUsername());

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                getAuthorities(user.getRole())
        );
    }

    /**
     * Преобразует роль из enum {@link Role} в коллекцию {@link GrantedAuthority}.
     * <br>
     * Spring Security требует, чтобы роли имели префикс {@code ROLE_} для работы
     * с выражениями доступа вида {@code hasRole('ADMIN')} или {@code hasAuthority('ROLE_ADMIN')}.
     *
     * @param role роль пользователя из базы данных
     * @return коллекция с одним элементом {@link SimpleGrantedAuthority}
     */
    private Collection<? extends GrantedAuthority> getAuthorities(Role role) {
        if (role == null) {
            logger.warn("Role is null, returning empty authorities");
            return Collections.emptyList();
        }

        String authorityName = "ROLE_" + role.name();
        logger.debug("Mapping role {} to authority {}", role, authorityName);

        return Collections.singletonList(new SimpleGrantedAuthority(authorityName));
    }

}