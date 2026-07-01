package com.kurumsal.wallet_api.infrastructure.exception;

import org.springframework.http.HttpStatus;

public class WalletNotFoundException extends AppException {

    public WalletNotFoundException(Long id) {
        super("Wallet not found: " + id, HttpStatus.NOT_FOUND, "WALLET_NOT_FOUND");
    }
}
