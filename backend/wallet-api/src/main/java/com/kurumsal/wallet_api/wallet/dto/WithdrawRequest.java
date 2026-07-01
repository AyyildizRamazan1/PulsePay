package com.kurumsal.wallet_api.wallet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record WithdrawRequest(
        @NotNull @DecimalMin(value = "0.01", message = "Amount must be at least 0.01") BigDecimal amount
) {}
