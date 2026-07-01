package com.kurumsal.wallet_api.infrastructure.exception;

import org.springframework.http.HttpStatusCode;

public class InsufficientBalanceException extends AppException {

    public InsufficientBalanceException() {
        super("Insufficient balance", HttpStatusCode.valueOf(422), "WALLET_INSUFFICIENT_BALANCE");
    }
}
