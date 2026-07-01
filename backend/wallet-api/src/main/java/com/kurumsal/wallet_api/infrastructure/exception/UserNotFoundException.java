package com.kurumsal.wallet_api.infrastructure.exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends AppException {

    public UserNotFoundException(Long id) {
        super("User not found: " + id, HttpStatus.NOT_FOUND, "USER_NOT_FOUND");
    }

    public UserNotFoundException(String email) {
        super("User not found: " + email, HttpStatus.NOT_FOUND, "USER_NOT_FOUND");
    }
}
