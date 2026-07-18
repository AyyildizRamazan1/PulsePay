package com.kurumsal.wallet_api.infrastructure.external;

/**
 * Internal signal used only to simulate a transient external-bank failure inside
 * {@link ExternalBankService}, so resilience4j's retry/circuit-breaker can act on it.
 * Never escapes to a controller — the {@code settleFallback} method translates the final
 * failure into {@link com.kurumsal.wallet_api.infrastructure.exception.ExternalBankUnavailableException}.
 */
class ExternalBankTimeoutException extends RuntimeException {

    ExternalBankTimeoutException() {
        super("Simulated external bank timeout");
    }
}
