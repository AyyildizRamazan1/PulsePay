package com.kurumsal.wallet_api.transaction.event;

import com.kurumsal.wallet_api.transaction.domain.TransactionType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionEvent(
        Long transactionId,
        Long fromWalletId,
        Long toWalletId,
        Long userId,
        BigDecimal amount,
        TransactionType type,
        String ipAddress,
        LocalDateTime occurredAt
) implements Serializable {
}
