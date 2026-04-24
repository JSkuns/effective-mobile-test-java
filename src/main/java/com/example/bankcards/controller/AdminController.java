package com.example.bankcards.controller;

import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер для административных операций.
 * <br>
 * Предоставляет эндпоинты для управления картами и пользователями системы.
 * Все методы требуют роль {@code ADMIN}.
 * <br>
 * <b>Примечание:</b> документация API описана в файле {@code openapi.yaml}.
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final CardService cardService;
    private final UserService userService;

    /**
     * Удаляет карту по идентификатору.
     *
     * @param id идентификатор карты для удаления
     * @return {@code 204 No Content} при успешном удалении
     * @throws com.example.bankcards.exception.CardNotFoundException если карта не найдена
     */
    @DeleteMapping("/cards/{id}")
    public ResponseEntity<Void> deleteCard(@PathVariable Long id) {
        cardService.deleteCard(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Возвращает все карты системы с пагинацией.
     *
     * @param pageable параметры пагинации (page, size, sort)
     * @return {@code 200 OK} со списком всех карт
     */
    @GetMapping("/cards")
    public ResponseEntity<Page<CardResponse>> getAllCards(Pageable pageable) {
        return ResponseEntity.ok(cardService.getAllCards(pageable));
    }

    /**
     * Активирует карту по идентификатору.
     * <br>
     * Может активировать заблокированную или истекшую карту.
     *
     * @param id идентификатор карты для активации
     * @return {@code 204 No Content} при успешной активации
     * @throws com.example.bankcards.exception.CardNotFoundException если карта не найдена
     * @throws IllegalStateException если карта не может быть активирована
     */
    @PatchMapping("/cards/{id}/activate")
    public ResponseEntity<Void> activateCard(@PathVariable Long id) {
        cardService.activateCard(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Возвращает список всех пользователей системы.
     *
     * @return {@code 200 OK} со списком пользователей
     */
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * Назначает роль {@code ADMIN} пользователю.
     * <br>
     * Повышает права доступа указанного пользователя.
     *
     * @param username имя пользователя для повышения прав
     * @return {@code 204 No Content} при успешном изменении роли
     * @throws org.springframework.security.core.userdetails.UsernameNotFoundException если пользователь не найден
     */
    @PatchMapping("/users/{username}/make-admin")
    public ResponseEntity<Void> makeAdmin(@PathVariable String username) {
        userService.makeAdmin(username);
        return ResponseEntity.noContent().build();
    }

}