package com.example.bankcards.controller;

import com.example.bankcards.dto.request.CreateCardRequest;
import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.service.CardService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * Контроллер для операций с банковскими картами пользователей.
 * <br>
 * Предоставляет эндпоинты для создания, просмотра, блокировки карт
 * и выполнения переводов между картами. Все операции выполняются
 * от имени аутентифицированного пользователя.
 * <br>
 * <b>Аутентификация:</b> все эндпоинты требуют валидный JWT-токен
 * в заголовке {@code Authorization: Bearer <token>}.
 * <br>
 * <b>Примечание:</b> документация API описана в файле {@code openapi.yaml}.
 */
@RestController
@RequestMapping("/cards")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth") // Требует JWT токен
public class CardController {

    private final CardService cardService;

    /**
     * Создаёт новую карту для текущего пользователя.
     *
     * @param userDetails данные аутентифицированного пользователя
     * @param request параметры создания карты
     * @return {@code 200 OK} с данными созданной карты
     * @throws RuntimeException если произошла ошибка при создании карты
     */
    @PostMapping
    public ResponseEntity<CardResponse> createCard(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateCardRequest request) {
        return ResponseEntity.ok(cardService.createCard(userDetails.getUsername(), request));
    }

    /**
     * Возвращает данные карты по идентификатору.
     * <br>
     * Доступна только карта, принадлежащая текущему пользователю.
     *
     * @param userDetails данные аутентифицированного пользователя
     * @param id идентификатор запрашиваемой карты
     * @return {@code 200 OK} с данными карты
     * @throws com.example.bankcards.exception.CardNotFoundException если карта не найдена или не принадлежит пользователю
     */
    @GetMapping("/{id}")
    public ResponseEntity<CardResponse> getCard(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(cardService.getCardById(userDetails.getUsername(), id));
    }

    /**
     * Возвращает список всех карт текущего пользователя с пагинацией.
     *
     * @param userDetails данные аутентифицированного пользователя
     * @param pageable параметры пагинации (страница, размер, сортировка)
     * @return {@code 200 OK} со страницей карт пользователя
     */
    @GetMapping
    public ResponseEntity<Page<CardResponse>> getAllCards(
            @AuthenticationPrincipal UserDetails userDetails,
            Pageable pageable) {
        return ResponseEntity.ok(cardService.getAllCardsByOwner(userDetails.getUsername(), pageable));
    }

    /**
     * Выполняет перевод средств между картами.
     * <br>
     * Перевод возможен между картами одного пользователя
     * или на карту другого пользователя (в зависимости от бизнес-логики).
     *
     * @param userDetails данные аутентифицированного пользователя (отправитель)
     * @param request параметры перевода (карта отправителя, карта получателя, сумма)
     * @return {@code 204 No Content} при успешном выполнении перевода
     * @throws jakarta.validation.ConstraintViolationException если запрос не прошёл валидацию
     * @throws com.example.bankcards.exception.InsufficientFundsException если недостаточно средств
     * @throws com.example.bankcards.exception.CardNotFoundException если карта заблокирована
     */
    @PostMapping("/transfer")
    public ResponseEntity<Void> transfer(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TransferRequest request) {
        cardService.transfer(userDetails.getUsername(), request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Блокирует карту по идентификатору.
     * <br>
     * Заблокированная карта не может использоваться для операций.
     *
     * @param userDetails данные аутентифицированного пользователя
     * @param id идентификатор карты для блокировки
     * @return {@code 204 No Content} при успешной блокировке
     * @throws com.example.bankcards.exception.CardNotFoundException если карта не найдена или не принадлежит
     * пользователю или если карта уже заблокирована
     */
    @PatchMapping("/{id}/block")
    public ResponseEntity<Void> blockCard(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        cardService.blockCard(userDetails.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Разблокирует ранее заблокированную карту.
     *
     * @param userDetails данные аутентифицированного пользователя
     * @param id идентификатор карты для разблокировки
     * @return {@code 204 No Content} при успешной разблокировке
     * @throws com.example.bankcards.exception.CardNotFoundException если карта не найдена или не принадлежит пользователю
     */
    @PatchMapping("/{id}/unblock")
    public ResponseEntity<Void> unblockCard(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        cardService.unblockCard(userDetails.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Возвращает текущий баланс карты.
     *
     * @param userDetails данные аутентифицированного пользователя
     * @param id идентификатор карты
     * @return {@code 200 OK} с текущим балансом карты
     * @throws com.example.bankcards.exception.CardNotFoundException если карта не найдена или не принадлежит пользователю
     */
    @GetMapping("/{id}/balance")
    public ResponseEntity<BigDecimal> getBalance(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(cardService.getBalance(userDetails.getUsername(), id));
    }

}