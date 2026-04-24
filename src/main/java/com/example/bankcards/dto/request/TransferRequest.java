package com.example.bankcards.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransferRequest {

    @NotNull(message = "From card ID is required")
    private Long fromCardId;

    @NotNull(message = "To card ID is required")
    private Long toCardId;

    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

}