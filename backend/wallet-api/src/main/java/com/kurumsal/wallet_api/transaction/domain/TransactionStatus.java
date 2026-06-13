package com.kurumsal.wallet_api.transaction.domain;

public enum TransactionStatus {
    PENDING,
    SUCCESS,
    FAILED,
    PENDING_REVIEW,
    CANCELLED
}