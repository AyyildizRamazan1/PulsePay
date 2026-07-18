package com.kurumsal.wallet_api.transaction.service;

public record FraudCheckResult(boolean suspicious, String reason) {

    public static FraudCheckResult suspicious(String reason) {
        return new FraudCheckResult(true, reason);
    }

    public static FraudCheckResult notSuspicious() {
        return new FraudCheckResult(false, null);
    }
}
