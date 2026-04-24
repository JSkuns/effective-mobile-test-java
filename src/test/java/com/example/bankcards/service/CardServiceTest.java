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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Юнит-тесты для {@link CardService} без загрузки Spring контекста.
 */
@ExtendWith(MockitoExtension.class)
class CardServiceUnitTest {

    @Mock private CardRepository cardRepository;
    @Mock private UserRepository userRepository;
    @Mock private EncryptionService encryptionService;
    @Mock private CardMasker cardMasker;

    @InjectMocks private CardService cardService;

    private User testUser;
    private Card testCard;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setId(1L);

        testCard = new Card();
        testCard.setId(1L);
        testCard.setOwner(testUser);
        testCard.setBalance(BigDecimal.valueOf(1000));
        testCard.setStatus(CardStatus.ACTIVE);
        testCard.setExpiryDate(LocalDate.now().plusYears(3));
        testCard.setCardNumberEncrypted("encrypted_1234567812345678");

        CardResponse mockCardResponse = new CardResponse();
        mockCardResponse.setId(1L);
        mockCardResponse.setMaskedCardNumber("**** **** **** 5678");
        mockCardResponse.setBalance(BigDecimal.valueOf(1000));
        mockCardResponse.setStatus(CardStatus.ACTIVE);
        mockCardResponse.setOwnerUsername("testuser");
    }

    // ==================== CREATE CARD TESTS ====================

    @Test
    @DisplayName("createCard с валидными данными → создаёт и возвращает карту")
    void createCard_withValidRequest_shouldCreateAndReturnCard() throws Exception {
        // Given
        CreateCardRequest request = new CreateCardRequest();
        request.setCardNumber("1234567812345678");
        request.setInitialBalance(BigDecimal.valueOf(500));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(encryptionService.encrypt("1234567812345678")).thenReturn("encrypted_1234567812345678");
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cardMasker.mask("encrypted_1234567812345678")).thenReturn("**** **** **** 5678");

        // When
        CardResponse response = cardService.createCard("testuser", request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getMaskedCardNumber()).isEqualTo("**** **** **** 5678");
        assertThat(response.getBalance()).isEqualTo(BigDecimal.valueOf(500));

        verify(userRepository).findByUsername("testuser");
        verify(encryptionService).encrypt("1234567812345678");
        verify(cardRepository).save(argThat(card ->
                card.getOwner().getUsername().equals("testuser") &&
                        card.getBalance().compareTo(BigDecimal.valueOf(500)) == 0 &&
                        card.getStatus() == CardStatus.ACTIVE
        ));
    }

    @Test
    @DisplayName("createCard с пустым username → выбрасывает IllegalArgumentException")
    void createCard_withEmptyUsername_shouldThrowException() {
        // Given & When & Then
        CreateCardRequest request = new CreateCardRequest();

        assertThatThrownBy(() -> cardService.createCard("", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username cannot be empty");

        verifyNoInteractions(userRepository, encryptionService, cardRepository);
    }

    @Test
    @DisplayName("createCard: пользователь не найден → UnauthorizedAccessException")
    void createCard_userNotFound_shouldThrowUnauthorizedAccessException() {
        // Given
        CreateCardRequest request = new CreateCardRequest();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> cardService.createCard("testuser", request))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessageContaining("User not found");

        verify(userRepository).findByUsername("testuser");
        verifyNoInteractions(encryptionService, cardRepository);
    }

    @Test
    @DisplayName("createCard: ошибка шифрования → RuntimeException")
    void createCard_encryptionFailure_shouldThrowRuntimeException() throws Exception {
        // Given
        CreateCardRequest request = new CreateCardRequest();
        request.setCardNumber("1234567812345678");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(encryptionService.encrypt("1234567812345678")).thenThrow(new RuntimeException("Encryption failed"));

        // When & Then
        assertThatThrownBy(() -> cardService.createCard("testuser", request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to encrypt card number");

        verify(encryptionService).encrypt("1234567812345678");
        verifyNoInteractions(cardRepository);
    }

    // ==================== GET CARD BY ID TESTS ====================

    @Test
    @DisplayName("getCardById с валидными данными → возвращает карту")
    void getCardById_withValidData_shouldReturnCard() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findByIdAndOwner(1L, testUser)).thenReturn(Optional.of(testCard));
        when(cardMasker.mask("encrypted_1234567812345678")).thenReturn("**** **** **** 5678");

        // When
        CardResponse response = cardService.getCardById("testuser", 1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getMaskedCardNumber()).isEqualTo("**** **** **** 5678");

        verify(cardRepository).findByIdAndOwner(1L, testUser);
    }

    @Test
    @DisplayName("getCardById: карта не найдена → CardNotFoundException")
    void getCardById_cardNotFound_shouldThrowCardNotFoundException() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findByIdAndOwner(999L, testUser)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> cardService.getCardById("testuser", 999L))
                .isInstanceOf(CardNotFoundException.class)
                .hasMessageContaining("Card not found");
    }

    @Test
    @DisplayName("getCardById: истёкшая карта → статус обновляется на EXPIRED")
    void getCardById_expiredCard_shouldUpdateStatusToExpired() {
        // Given
        Card expiredCard = new Card();
        expiredCard.setId(2L);
        expiredCard.setOwner(testUser);
        expiredCard.setExpiryDate(LocalDate.now().minusDays(1)); // Истёк вчера
        expiredCard.setStatus(CardStatus.ACTIVE); // Но статус ещё ACTIVE
        expiredCard.setCardNumberEncrypted("encrypted_expired");
        expiredCard.setBalance(BigDecimal.ZERO);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findByIdAndOwner(2L, testUser)).thenReturn(Optional.of(expiredCard));
        when(cardMasker.mask("encrypted_expired")).thenReturn("**** **** **** 0000");
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CardResponse response = cardService.getCardById("testuser", 2L);

        // Then
        assertThat(expiredCard.getStatus()).isEqualTo(CardStatus.EXPIRED);
        verify(cardRepository).save(expiredCard);
    }

    // ==================== GET ALL CARDS TESTS ====================

    @Test
    @DisplayName("getAllCardsByOwner → возвращает страницу карт пользователя")
    void getAllCardsByOwner_shouldReturnPaginatedCards() {
        // Given
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Card> cardPage = new PageImpl<>(List.of(testCard), pageable, 1);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findAllByOwner(eq(testUser), any(PageRequest.class))).thenReturn(cardPage);
        when(cardMasker.mask(any())).thenReturn("**** **** **** 5678");

        // When
        Page<CardResponse> response = cardService.getAllCardsByOwner("testuser", pageable);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getId()).isEqualTo(1L);

        verify(cardRepository).findAllByOwner(eq(testUser), eq(pageable));
    }

    // ==================== TRANSFER TESTS ====================

    @Test
    @DisplayName("transfer с валидными данными → успешно переводит средства")
    void transfer_withValidData_shouldTransferFunds() {
        // Given
        Card fromCard = new Card();
        fromCard.setId(1L);
        fromCard.setOwner(testUser);
        fromCard.setBalance(BigDecimal.valueOf(1000));
        fromCard.setStatus(CardStatus.ACTIVE);

        Card toCard = new Card();
        toCard.setId(2L);
        toCard.setOwner(testUser);
        toCard.setBalance(BigDecimal.valueOf(500));
        toCard.setStatus(CardStatus.ACTIVE);

        TransferRequest request = new TransferRequest();
        request.setFromCardId(1L);
        request.setToCardId(2L);
        request.setAmount(BigDecimal.valueOf(200));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findByIdAndOwner(1L, testUser)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdAndOwner(2L, testUser)).thenReturn(Optional.of(toCard));

        // When
        cardService.transfer("testuser", request);

        // Then
        assertThat(fromCard.getBalance()).isEqualTo(BigDecimal.valueOf(800));
        assertThat(toCard.getBalance()).isEqualTo(BigDecimal.valueOf(700));

        verify(cardRepository).findByIdAndOwner(1L, testUser);
        verify(cardRepository).findByIdAndOwner(2L, testUser);
    }

    @Test
    @DisplayName("transfer: недостаточно средств → InsufficientFundsException")
    void transfer_insufficientFunds_shouldThrowException() {
        // Given
        Card fromCard = new Card();
        fromCard.setId(1L);
        fromCard.setOwner(testUser);
        fromCard.setBalance(BigDecimal.valueOf(100));
        fromCard.setStatus(CardStatus.ACTIVE);

        Card toCard = new Card();
        toCard.setId(2L);
        toCard.setOwner(testUser);
        toCard.setBalance(BigDecimal.valueOf(500));
        toCard.setStatus(CardStatus.ACTIVE);

        TransferRequest request = new TransferRequest();
        request.setFromCardId(1L);
        request.setToCardId(2L);
        request.setAmount(BigDecimal.valueOf(200)); // Больше чем баланс

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findByIdAndOwner(1L, testUser)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdAndOwner(2L, testUser)).thenReturn(Optional.of(toCard));

        // When & Then
        assertThatThrownBy(() -> cardService.transfer("testuser", request))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Insufficient funds");
    }

    @Test
    @DisplayName("transfer: неактивная карта-отправитель → IllegalStateException")
    void transfer_inactiveSourceCard_shouldThrowException() {
        // Given
        Card fromCard = new Card();
        fromCard.setId(1L);
        fromCard.setOwner(testUser);
        fromCard.setBalance(BigDecimal.valueOf(1000));
        fromCard.setStatus(CardStatus.BLOCKED); // Заблокирована!

        Card toCard = new Card();
        toCard.setId(2L);
        toCard.setOwner(testUser);
        toCard.setBalance(BigDecimal.valueOf(500));
        toCard.setStatus(CardStatus.ACTIVE);

        TransferRequest request = new TransferRequest();
        request.setFromCardId(1L);
        request.setToCardId(2L);
        request.setAmount(BigDecimal.valueOf(100));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findByIdAndOwner(1L, testUser)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdAndOwner(2L, testUser)).thenReturn(Optional.of(toCard));

        // When & Then
        assertThatThrownBy(() -> cardService.transfer("testuser", request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Source card is not active");
    }

    @Test
    @DisplayName("transfer: отрицательная сумма → IllegalArgumentException")
    void transfer_negativeAmount_shouldThrowException() {
        // Given
        TransferRequest request = new TransferRequest();
        request.setFromCardId(1L);
        request.setToCardId(2L);
        request.setAmount(BigDecimal.valueOf(-100));

        // When & Then
        assertThatThrownBy(() -> cardService.transfer("testuser", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Transfer amount must be positive");

        verifyNoInteractions(userRepository, cardRepository);
    }

    // ==================== BLOCK/UNBLOCK TESTS ====================

    @Test
    @DisplayName("blockCard → устанавливает статус BLOCKED")
    void blockCard_shouldSetStatusToBlocked() {
        // Given
        Card card = new Card();
        card.setId(1L);
        card.setOwner(testUser);
        card.setStatus(CardStatus.ACTIVE);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findByIdAndOwner(1L, testUser)).thenReturn(Optional.of(card));

        // When
        cardService.blockCard("testuser", 1L);

        // Then
        assertThat(card.getStatus()).isEqualTo(CardStatus.BLOCKED);
    }

    @Test
    @DisplayName("blockCard: карта уже заблокирована → ничего не делает")
    void blockCard_alreadyBlocked_shouldDoNothing() {
        // Given
        Card card = new Card();
        card.setId(1L);
        card.setOwner(testUser);
        card.setStatus(CardStatus.BLOCKED);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findByIdAndOwner(1L, testUser)).thenReturn(Optional.of(card));

        // When
        cardService.blockCard("testuser", 1L);

        // Then
        assertThat(card.getStatus()).isEqualTo(CardStatus.BLOCKED);
        verify(cardRepository, never()).save(any()); // save не вызывается, т.к. статус не меняется
    }

    @Test
    @DisplayName("unblockCard → устанавливает статус ACTIVE")
    void unblockCard_shouldSetStatusToActive() {
        // Given
        Card card = new Card();
        card.setId(1L);
        card.setOwner(testUser);
        card.setStatus(CardStatus.BLOCKED);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findByIdAndOwner(1L, testUser)).thenReturn(Optional.of(card));

        // When
        cardService.unblockCard("testuser", 1L);

        // Then
        assertThat(card.getStatus()).isEqualTo(CardStatus.ACTIVE);
    }

    @Test
    @DisplayName("unblockCard: карта не заблокирована → IllegalStateException")
    void unblockCard_notBlocked_shouldThrowException() {
        // Given
        Card card = new Card();
        card.setId(1L);
        card.setOwner(testUser);
        card.setStatus(CardStatus.ACTIVE);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findByIdAndOwner(1L, testUser)).thenReturn(Optional.of(card));

        // When & Then
        assertThatThrownBy(() -> cardService.unblockCard("testuser", 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Card is not blocked");
    }

    // ==================== GET BALANCE TESTS ====================

    @Test
    @DisplayName("getBalance → возвращает баланс карты")
    void getBalance_shouldReturnCardBalance() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findByIdAndOwner(1L, testUser)).thenReturn(Optional.of(testCard));

        // When
        BigDecimal balance = cardService.getBalance("testuser", 1L);

        // Then
        assertThat(balance).isEqualTo(BigDecimal.valueOf(1000));
    }

    // ==================== ADMIN METHODS TESTS ====================

    @Test
    @DisplayName("deleteCard (admin) → удаляет карту")
    void deleteCard_admin_shouldDeleteCard() {
        // Given
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));

        // When
        cardService.deleteCard(1L);

        // Then
        verify(cardRepository).delete(testCard);
    }

    @Test
    @DisplayName("deleteCard: карта не найдена → CardNotFoundException")
    void deleteCard_notFound_shouldThrowException() {
        // Given
        when(cardRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> cardService.deleteCard(999L))
                .isInstanceOf(CardNotFoundException.class)
                .hasMessageContaining("Card not found");
    }

    @Test
    @DisplayName("getAllCards (admin) → возвращает все карты с пагинацией")
    void getAllCards_admin_shouldReturnAllCards() {
        // Given
        PageRequest pageable = PageRequest.of(0, 20);
        Page<Card> cardPage = new PageImpl<>(List.of(testCard), pageable, 1);

        when(cardRepository.findAll(pageable)).thenReturn(cardPage);
        when(cardMasker.mask(any())).thenReturn("**** **** **** 5678");

        // When
        Page<CardResponse> response = cardService.getAllCards(pageable);

        // Then
        assertThat(response.getContent()).hasSize(1);
        verify(cardRepository).findAll(pageable);
    }

    @Test
    @DisplayName("activateCard (admin) → активирует карту")
    void activateCard_admin_shouldActivateCard() {
        // Given
        Card card = new Card();
        card.setId(1L);
        card.setStatus(CardStatus.BLOCKED);

        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        // When
        cardService.activateCard(1L);

        // Then
        assertThat(card.getStatus()).isEqualTo(CardStatus.ACTIVE);
    }

    @Test
    @DisplayName("activateCard: карта уже активна → ничего не делает")
    void activateCard_alreadyActive_shouldDoNothing() {
        // Given
        Card card = new Card();
        card.setId(1L);
        card.setStatus(CardStatus.ACTIVE);

        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        // When
        cardService.activateCard(1L);

        // Then
        assertThat(card.getStatus()).isEqualTo(CardStatus.ACTIVE);
        verify(cardRepository, never()).save(any());
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("createCard: null баланс → инициализируется нулём")
    void createCard_nullBalance_shouldDefaultToZero() throws Exception {
        // Given
        CreateCardRequest request = new CreateCardRequest();
        request.setCardNumber("1234567812345678");
        // initialBalance не установлен = null

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(encryptionService.encrypt("1234567812345678")).thenReturn("encrypted");
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cardMasker.mask(any())).thenReturn("****");

        // When
        CardResponse response = cardService.createCard("testuser", request);

        // Then
        ArgumentCaptor<Card> cardCaptor = ArgumentCaptor.forClass(Card.class);
        verify(cardRepository).save(cardCaptor.capture());
        assertThat(cardCaptor.getValue().getBalance()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("checkAndUpdateExpiration: уже EXPIRED → не сохраняет")
    void checkAndUpdateExpiration_alreadyExpired_shouldNotSave() {
        // Given - проверяем через getCardById
        Card expiredCard = new Card();
        expiredCard.setId(1L);
        expiredCard.setOwner(testUser);
        expiredCard.setExpiryDate(LocalDate.now().minusDays(1));
        expiredCard.setStatus(CardStatus.EXPIRED); // Уже EXPIRED
        expiredCard.setCardNumberEncrypted("encrypted");
        expiredCard.setBalance(BigDecimal.ZERO);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(cardRepository.findByIdAndOwner(1L, testUser)).thenReturn(Optional.of(expiredCard));
        when(cardMasker.mask(any())).thenReturn("****");

        // When
        cardService.getCardById("testuser", 1L);

        // Then - save не должен вызываться, т.к. статус уже EXPIRED
        verify(cardRepository, never()).save(expiredCard);
    }

}