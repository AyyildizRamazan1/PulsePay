package com.kurumsal.wallet_api.infrastructure.exception;

import org.springframework.http.HttpStatus;

public class ExternalBankUnavailableException extends AppException {

    public ExternalBankUnavailableException() {
        super("External bank settlement unavailable, please retry later",
                HttpStatus.SERVICE_UNAVAILABLE, "EXTERNAL_BANK_UNAVAILABLE");
    }
}
