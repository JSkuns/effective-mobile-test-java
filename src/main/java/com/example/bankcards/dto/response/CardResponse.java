package com.example.bankcards.dto.response;

import com.example.bankcards.entity.CardStatus;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CardResponse {

    private Long id;
    private String maskedCardNumber; // **** **** **** 1234
    private BigDecimal balance;
    private CardStatus status;
    private String ownerUsername;

}