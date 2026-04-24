package com.example.bankcards.controller;

import com.example.bankcards.dto.request.CreateCardRequest;
import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.service.CardService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Юнит-тесты для {@link CardController} без загрузки Spring контекста.
 */
@ExtendWith(MockitoExtension.class)
class CardControllerUnitTest {

    @Mock private CardService cardService;

    @InjectMocks private CardController cardController;

    private UserDetails testUser;
    private CardResponse mockCardResponse;

    @BeforeEach
    void setUp() {
        // Создаём тестового пользователя с ролью USER
        testUser = new User(
                "testuser",
                "",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        // Создаём мок-ответ карты
        mockCardResponse = mock(CardResponse.class);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // ==================== CREATE CARD TESTS ====================

    @Test
    @DisplayName("POST /cards → 200 OK с созданной картой")
    void createCard_withValidRequest_shouldReturnCreatedCard() {
        // Given
        CreateCardRequest request = new CreateCardRequest();
        when(cardService.createCard(eq("testuser"), any(CreateCardRequest.class)))
                .thenReturn(mockCardResponse);
        setAuthentication(testUser);

        // When
        ResponseEntity<CardResponse> response = cardController.createCard(testUser, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(mockCardResponse);
        verify(cardService).createCard("testuser", request);
    }

    @Test
    @DisplayName("POST /cards с ошибкой сервиса → пробрасывает исключение")
    void createCard_withServiceError_shouldPropagateException() {
        // Given
        CreateCardRequest request = new CreateCardRequest();
        when(cardService.createCard(eq("testuser"), any(CreateCardRequest.class)))
                .thenThrow(new RuntimeException("Creation failed"));
        setAuthentication(testUser);

        // When & Then
        assertThatThrownBy(() -> cardController.createCard(testUser, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Creation failed");
    }

    // ==================== GET CARD TESTS ====================

    @Test
    @DisplayName("GET /cards/{id} → 200 OK с данными карты")
    void getCard_withValidId_shouldReturnCard() {
        // Given
        Long cardId = 1L;
        when(cardService.getCardById("testuser", cardId)).thenReturn(mockCardResponse);
        setAuthentication(testUser);

        // When
        ResponseEntity<CardResponse> response = cardController.getCard(testUser, cardId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(mockCardResponse);
        verify(cardService).getCardById("testuser", cardId);
    }

    @Test
    @DisplayName("GET /cards/{id} с несуществующей картой → пробрасывает исключение")
    void getCard_withNotFoundId_shouldPropagateException() {
        // Given
        Long cardId = 999L;
        when(cardService.getCardById("testuser", cardId))
                .thenThrow(new com.example.bankcards.exception.CardNotFoundException(cardId.toString()));
        setAuthentication(testUser);

        // When & Then
        assertThatThrownBy(() -> cardController.getCard(testUser, cardId))
                .isInstanceOf(com.example.bankcards.exception.CardNotFoundException.class);
    }

    // ==================== GET ALL CARDS TESTS ====================

    @Test
    @DisplayName("GET /cards → 200 OK со списком карт с пагинацией")
    void getAllCards_shouldReturnPaginatedCards() {
        // Given
        PageRequest pageable = PageRequest.of(0, 10);
        Page<CardResponse> page = new PageImpl<>(List.of(mockCardResponse), pageable, 1);
        when(cardService.getAllCardsByOwner("testuser", pageable)).thenReturn(page);
        setAuthentication(testUser);

        // When
        ResponseEntity<Page<CardResponse>> response = cardController.getAllCards(testUser, pageable);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        assertThat(response.getBody().getTotalElements()).isEqualTo(1);
        verify(cardService).getAllCardsByOwner("testuser", pageable);
    }

    // ==================== TRANSFER TESTS ====================

    @Test
    @DisplayName("POST /cards/transfer → 204 No Content")
    void transfer_withValidRequest_shouldReturnNoContent() {
        // Given
        TransferRequest request = new TransferRequest();
        doNothing().when(cardService).transfer(eq("testuser"), any(TransferRequest.class));
        setAuthentication(testUser);

        // When
        ResponseEntity<Void> response = cardController.transfer(testUser, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(cardService).transfer("testuser", request);
    }

    @Test
    @DisplayName("POST /cards/transfer с недостатком средств → пробрасывает исключение")
    void transfer_withInsufficientFunds_shouldPropagateException() {
        // Given
        TransferRequest request = new TransferRequest();
        doThrow(new com.example.bankcards.exception.InsufficientFundsException("Not enough money"))
                .when(cardService).transfer(eq("testuser"), any(TransferRequest.class));
        setAuthentication(testUser);

        // When & Then
        assertThatThrownBy(() -> cardController.transfer(testUser, request))
                .isInstanceOf(com.example.bankcards.exception.InsufficientFundsException.class);
    }

    // ==================== BLOCK/UNBLOCK TESTS ====================

    @Test
    @DisplayName("PATCH /cards/{id}/block → 204 No Content")
    void blockCard_withValidId_shouldReturnNoContent() {
        // Given
        Long cardId = 1L;
        doNothing().when(cardService).blockCard("testuser", cardId);
        setAuthentication(testUser);

        // When
        ResponseEntity<Void> response = cardController.blockCard(testUser, cardId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(cardService).blockCard("testuser", cardId);
    }

    @Test
    @DisplayName("PATCH /cards/{id}/unblock → 204 No Content")
    void unblockCard_withValidId_shouldReturnNoContent() {
        // Given
        Long cardId = 1L;
        doNothing().when(cardService).unblockCard("testuser", cardId);
        setAuthentication(testUser);

        // When
        ResponseEntity<Void> response = cardController.unblockCard(testUser, cardId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(cardService).unblockCard("testuser", cardId);
    }

    // ==================== GET BALANCE TESTS ====================

    @Test
    @DisplayName("GET /cards/{id}/balance → 200 OK с балансом")
    void getBalance_withValidId_shouldReturnBalance() {
        // Given
        Long cardId = 1L;
        BigDecimal expectedBalance = BigDecimal.valueOf(2500.50);
        when(cardService.getBalance("testuser", cardId)).thenReturn(expectedBalance);
        setAuthentication(testUser);

        // When
        ResponseEntity<BigDecimal> response = cardController.getBalance(testUser, cardId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedBalance);
        verify(cardService).getBalance("testuser", cardId);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Устанавливает мок-аутентификацию в SecurityContextHolder.
     * Это нужно, чтобы @AuthenticationPrincipal в контроллере получил пользователя.
     */
    private void setAuthentication(UserDetails userDetails) {
        var auth = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

}