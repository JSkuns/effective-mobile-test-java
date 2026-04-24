package com.example.bankcards.service;

import com.example.bankcards.dto.request.CreateCardRequest;
import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.exception.UnauthorizedAccessException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardMasker;
import com.example.bankcards.util.EncryptionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Сервис для операций с банковскими картами пользователей.
 * <br>
 * Отвечает за:
 * <ul>
 *   <li>Создание, чтение, обновление и удаление карт</li>
 *   <li>Проверку принадлежности карты пользователю</li>
 *   <li>Выполнение переводов между картами</li>
 *   <li>Блокировку/разблокировку карт</li>
 *   <li>Автоматическую проверку истечения срока действия</li>
 * </ul>
 * <br>
 * <b>Безопасность:</b> все методы, работающие с картами пользователя,
 * проверяют принадлежность карты через {@code findByIdAndOwner}.
 * <br>
 * <b>Транзакционность:</b> класс помечен {@code @Transactional}, поэтому
 * все публичные методы выполняются в рамках одной транзакции.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CardService {

    private static final Logger logger = LoggerFactory.getLogger(CardService.class);

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final EncryptionService encryptionService;
    private final CardMasker cardMasker;

    /**
     * Создаёт новую карту для указанного пользователя.
     * <br>
     * Номер карты шифруется перед сохранением, баланс инициализируется
     * значением из запроса, срок действия устанавливается на 3 года вперёд
     * (если не указан явно).
     *
     * @param username имя владельца карты
     * @param request  параметры создания карты (номер, начальный баланс, срок действия)
     * @return {@link CardResponse} с данными созданной карты (номер маскирован)
     * @throws com.example.bankcards.exception.UnauthorizedAccessException если пользователь не найден
     * @throws RuntimeException                                            если не удалось зашифровать номер карты
     */
    public CardResponse createCard(String username, CreateCardRequest request) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }

        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedAccessException("User not found: " + username));

        Card card = new Card();
        card.setOwner(owner);
        card.setBalance(request.getInitialBalance() != null ? request.getInitialBalance() : BigDecimal.ZERO);

        try {
            card.setCardNumberEncrypted(encryptionService.encrypt(request.getCardNumber()));
        } catch (Exception e) {
            logger.error("Failed to encrypt card number for user {}", username, e);
            throw new RuntimeException("Failed to encrypt card number", e);
        }

        card.setExpiryDate(
                request.getExpiryDate() != null
                        ? request.getExpiryDate()
                        : LocalDate.now().plusYears(3)
        );
        card.setStatus(CardStatus.ACTIVE);

        Card savedCard = cardRepository.save(card);
        logger.info("Created card {} for user {}", savedCard.getId(), username);

        return mapToResponse(savedCard);
    }

    /**
     * Возвращает данные карты по идентификатору.
     * <br>
     * Перед возвратом проверяется:
     * <ul>
     *   <li>Принадлежность карты пользователю</li>
     *   <li>Срок действия карты (автоматическая пометка как EXPIRED)</li>
     * </ul>
     *
     * @param username имя запрашивающего пользователя
     * @param cardId   идентификатор карты
     * @return {@link CardResponse} с данными карты (номер маскирован)
     * @throws CardNotFoundException       если карта не найдена или не принадлежит пользователю
     * @throws UnauthorizedAccessException если пользователь не найден
     */
    @Transactional(readOnly = true)
    public CardResponse getCardById(String username, Long cardId) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }

        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedAccessException("User not found: " + username));

        Card card = cardRepository.findByIdAndOwner(cardId, owner)
                .orElseThrow(() -> new CardNotFoundException("Card not found or access denied: " + cardId));

        checkAndUpdateExpiration(card);

        return mapToResponse(card);
    }

    /**
     * Проверяет и обновляет статус карты при получении.
     * <br>
     * Если срок действия карты истёк и статус ещё не {@code EXPIRED},
     * карта автоматически помечается как истёкшая и сохраняется в БД.
     *
     * @param card карта для проверки
     */
    private void checkAndUpdateExpiration(Card card) {
        if (card.getStatus() == CardStatus.EXPIRED) {
            return;
        }

        if (card.getExpiryDate() != null && card.getExpiryDate().isBefore(LocalDate.now())) {
            card.setStatus(CardStatus.EXPIRED);
            cardRepository.save(card);
            logger.info("Card {} marked as EXPIRED", card.getId());
        }
    }

    /**
     * Возвращает список всех карт пользователя с пагинацией.
     * <br>
     * Номер каждой карты в ответе маскируется для безопасности.
     *
     * @param username имя владельца карт
     * @param pageable параметры пагинации (страница, размер, сортировка)
     * @return {@link Page} с {@link CardResponse} картами пользователя
     * @throws UnauthorizedAccessException если пользователь не найден
     */
    @Transactional(readOnly = true)
    public Page<CardResponse> getAllCardsByOwner(String username, Pageable pageable) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }

        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedAccessException("User not found: " + username));

        return cardRepository.findAllByOwner(owner, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Выполняет перевод средств между двумя картами одного пользователя.
     * <br>
     * Перед переводом проверяется:
     * <ul>
     *   <li>Наличие достаточного баланса на карте-отправителе</li>
     *   <li>Принадлежность обеих карт пользователю</li>
     *   <li>Активный статус карт (опционально, зависит от бизнес-логики)</li>
     * </ul>
     *
     * @param username имя владельца карт
     * @param request  параметры перевода (отправитель, получатель, сумма)
     * @throws CardNotFoundException       если одна из карт не найдена или не принадлежит пользователю
     * @throws InsufficientFundsException  если недостаточно средств на карте-отправителе
     * @throws UnauthorizedAccessException если пользователь не найден
     * @throws IllegalArgumentException    если сумма перевода не положительная
     */
    public void transfer(String username, TransferRequest request) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }

        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedAccessException("User not found: " + username));

        Card fromCard = cardRepository.findByIdAndOwner(request.getFromCardId(), owner)
                .orElseThrow(() -> new CardNotFoundException("Source card not found or access denied"));

        Card toCard = cardRepository.findByIdAndOwner(request.getToCardId(), owner)
                .orElseThrow(() -> new CardNotFoundException("Target card not found or access denied"));

        // Опционально: проверить статус карт
        if (fromCard.getStatus() != CardStatus.ACTIVE) {
            throw new IllegalStateException("Source card is not active: " + fromCard.getStatus());
        }

        if (fromCard.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds on card " + fromCard.getId() +
                            ". Available: " + fromCard.getBalance() +
                            ", Required: " + request.getAmount()
            );
        }

        fromCard.setBalance(fromCard.getBalance().subtract(request.getAmount()));
        toCard.setBalance(toCard.getBalance().add(request.getAmount()));

        // Сохранение не требуется явно благодаря @Transactional на классе
        logger.info("Transferred {} from card {} to card {} for user {}",
                request.getAmount(), fromCard.getId(), toCard.getId(), username);
    }

    /**
     * Блокирует карту по идентификатору.
     * <br>
     * Заблокированная карта не может использоваться для операций
     * (переводов, платежей и т.д.).
     *
     * @param username имя владельца карты
     * @param cardId   идентификатор карты для блокировки
     * @throws CardNotFoundException       если карта не найдена или не принадлежит пользователю
     * @throws UnauthorizedAccessException если пользователь не найден
     */
    public void blockCard(String username, Long cardId) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }

        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedAccessException("User not found: " + username));

        Card card = cardRepository.findByIdAndOwner(cardId, owner)
                .orElseThrow(() -> new CardNotFoundException("Card not found or access denied: " + cardId));

        if (card.getStatus() == CardStatus.BLOCKED) {
            logger.debug("Card {} is already blocked", cardId);
            return;
        }

        card.setStatus(CardStatus.BLOCKED);
        logger.info("Blocked card {} for user {}", cardId, username);
    }

    /**
     * Разблокирует ранее заблокированную карту.
     *
     * @param username имя владельца карты
     * @param cardId   идентификатор карты для разблокировки
     * @throws CardNotFoundException       если карта не найдена или не принадлежит пользователю
     * @throws UnauthorizedAccessException если пользователь не найден
     * @throws IllegalStateException       если карта не была заблокирована
     */
    public void unblockCard(String username, Long cardId) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }

        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedAccessException("User not found: " + username));

        Card card = cardRepository.findByIdAndOwner(cardId, owner)
                .orElseThrow(() -> new CardNotFoundException("Card not found or access denied: " + cardId));

        if (card.getStatus() != CardStatus.BLOCKED) {
            throw new IllegalStateException("Card is not blocked: " + card.getStatus());
        }

        card.setStatus(CardStatus.ACTIVE);
        logger.info("Unblocked card {} for user {}", cardId, username);
    }

    /**
     * Возвращает текущий баланс карты.
     *
     * @param username имя владельца карты
     * @param cardId   идентификатор карты
     * @return текущий баланс карты
     * @throws CardNotFoundException       если карта не найдена или не принадлежит пользователю
     * @throws UnauthorizedAccessException если пользователь не найден
     */
    @Transactional(readOnly = true)
    public BigDecimal getBalance(String username, Long cardId) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }

        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedAccessException("User not found: " + username));

        Card card = cardRepository.findByIdAndOwner(cardId, owner)
                .orElseThrow(() -> new CardNotFoundException("Card not found or access denied: " + cardId));

        return card.getBalance();
    }

    // ==================== ADMIN METHODS ====================

    /**
     * Удаляет карту по идентификатору (только для администраторов).
     * <br>
     * Не проверяет принадлежность карты — админ может удалять любые карты.
     *
     * @param cardId идентификатор карты для удаления
     * @throws CardNotFoundException если карта не найдена
     */
    public void deleteCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException("Card not found: " + cardId));

        cardRepository.delete(card);
        logger.info("Deleted card {} by admin", cardId);
    }

    /**
     * Возвращает все карты системы с пагинацией (только для администраторов).
     *
     * @param pageable параметры пагинации
     * @return {@link Page} с {@link CardResponse} всех карт системы
     */
    @Transactional(readOnly = true)
    public Page<CardResponse> getAllCards(Pageable pageable) {
        return cardRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    /**
     * Активирует карту по идентификатору (только для администраторов).
     * <p>
     * Может активировать заблокированную или истёкшую карту.
     * </p>
     *
     * @param cardId идентификатор карты для активации
     * @throws CardNotFoundException если карта не найдена
     */
    public void activateCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException("Card not found: " + cardId));

        if (card.getStatus() == CardStatus.ACTIVE) {
            logger.debug("Card {} is already active", cardId);
            return;
        }

        card.setStatus(CardStatus.ACTIVE);
        logger.info("Activated card {} by admin", cardId);
    }

    // ==================== PRIVATE HELPERS ====================

    /**
     * Маппит сущность {@link Card} в DTO {@link CardResponse}.
     * <br>
     * Номер карты маскируется через {@link CardMasker} для безопасности.
     *
     * @param card сущность карты
     * @return DTO с публичными данными карты
     */
    private CardResponse mapToResponse(Card card) {
        CardResponse response = new CardResponse();
        response.setId(card.getId());
        response.setMaskedCardNumber(cardMasker.mask(card.getCardNumberEncrypted()));
        response.setBalance(card.getBalance());
        response.setStatus(card.getStatus());
        response.setOwnerUsername(card.getOwner().getUsername());
        return response;
    }

}