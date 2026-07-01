package com.kurumsal.wallet_api.transaction.dto;

import com.kurumsal.wallet_api.transaction.domain.Transaction;
import com.kurumsal.wallet_api.transaction.domain.TransactionStatus;
import com.kurumsal.wallet_api.transaction.domain.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        Long fromWalletId,
        Long toWalletId,
        BigDecimal amount,
        TransactionType type,
        TransactionStatus status,
        String idempotencyKey,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
    public static TransactionResponse from(Transaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getFromWallet() != null ? tx.getFromWallet().getId() : null,
                tx.getToWallet() != null ? tx.getToWallet().getId() : null,
                tx.getAmount(),
                tx.getType(),
                tx.getStatus(),
                tx.getIdempotencyKey(),
                tx.getCreatedAt(),
                tx.getCompletedAt()
        );
    }
}
