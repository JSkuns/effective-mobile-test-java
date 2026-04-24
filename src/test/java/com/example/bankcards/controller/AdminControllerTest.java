package com.example.bankcards.controller;

import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.UserService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Юнит-тесты для {@link AdminController} без загрузки Spring контекста.
 */
@ExtendWith(MockitoExtension.class)
class AdminControllerUnitTest {

    @Mock private CardService cardService;
    @Mock private UserService userService;

    @InjectMocks private AdminController adminController;

    private UserDetails adminUser;

    @BeforeEach
    void setUp() {
        adminUser = new User(
                "admin",
                "",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    @AfterEach  // Очищаем контекст ПОСЛЕ теста
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("deleteCard → 204 No Content")
    void deleteCard_shouldReturnNoContent() {
        Long cardId = 10L;
        doNothing().when(cardService).deleteCard(cardId);
        setAuthentication(adminUser);

        ResponseEntity<Void> response = adminController.deleteCard(cardId);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(cardService).deleteCard(cardId);
        verifyNoMoreInteractions(cardService, userService);
    }

    @Test
    @DisplayName("getAllCards → 200 OK с пагинацией")
    void getAllCards_shouldReturnPaginatedCards() {
        CardResponse mockCard = mock(CardResponse.class);
        Page<CardResponse> page = new PageImpl<>(List.of(mockCard), PageRequest.of(0, 20), 1);
        when(cardService.getAllCards(any())).thenReturn(page);
        setAuthentication(adminUser);

        ResponseEntity<Page<CardResponse>> response =
                adminController.getAllCards(PageRequest.of(0, 20));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent())
                .hasSize(1)
                .containsExactly(mockCard); // ✅ Полная проверка
        verify(cardService).getAllCards(any());
    }

    @Test
    @DisplayName("activateCard → 204 No Content")
    void activateCard_shouldReturnNoContent() {
        Long cardId = 5L;
        doNothing().when(cardService).activateCard(cardId);
        setAuthentication(adminUser);

        ResponseEntity<Void> response = adminController.activateCard(cardId);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(cardService).activateCard(cardId);
    }

    @Test
    @DisplayName("getAllUsers → 200 OK со списком")
    void getAllUsers_shouldReturnUsersList() {
        com.example.bankcards.entity.User mockUser1 = mock(com.example.bankcards.entity.User.class);
        com.example.bankcards.entity.User mockUser2 = mock(com.example.bankcards.entity.User.class);

        when(userService.getAllUsers()).thenReturn(List.of(mockUser1, mockUser2));
        setAuthentication(adminUser);

        ResponseEntity<?> response = adminController.getAllUsers();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).asList().hasSize(2);
        verify(userService).getAllUsers();
    }

    @Test
    @DisplayName("makeAdmin → 204 No Content")
    void makeAdmin_shouldReturnNoContent() {
        String username = "testuser";
        doNothing().when(userService).makeAdmin(username);
        setAuthentication(adminUser);

        ResponseEntity<Void> response = adminController.makeAdmin(username);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(userService).makeAdmin(username);
    }

    /**
     * Устанавливает мок-аутентификацию в SecurityContextHolder.
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