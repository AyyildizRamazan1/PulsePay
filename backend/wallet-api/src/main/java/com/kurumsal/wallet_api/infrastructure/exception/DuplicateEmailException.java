package com.kurumsal.wallet_api.infrastructure.exception;

import org.springframework.http.HttpStatus;

public class DuplicateEmailException extends AppException {

    public DuplicateEmailException(String email) {
        super("Email already in use: " + email, HttpStatus.CONFLICT, "USER_EMAIL_DUPLICATE");
    }
}
