package com.kurumsal.wallet_api.infrastructure.exception;

import org.springframework.http.HttpStatusCode;

public class AppException extends RuntimeException {

    private final HttpStatusCode status;
    private final String errorCode;

    public AppException(String message, HttpStatusCode status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatusCode getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
