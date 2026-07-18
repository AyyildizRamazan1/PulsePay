package com.kurumsal.wallet_api.infrastructure.external;

import com.kurumsal.wallet_api.infrastructure.exception.ExternalBankUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.observation.annotation.Observed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Stub for the external bank rail a withdrawal ultimately settles against. There is no real
 * bank integration behind this — it simulates network latency and a configurable failure rate
 * ({@code app.external-bank.failure-rate}) purely to exercise the resilience4j circuit
 * breaker/retry pipeline ("externalBankService" instance in application.yml) and the
 * compensating-rollback path in {@link com.kurumsal.wallet_api.wallet.service.CompensationService}.
 */
@Slf4j
@Service
public class ExternalBankService {

    @Value("${app.external-bank.failure-rate:0.15}")
    private double failureRate;

    @CircuitBreaker(name = "externalBankService", fallbackMethod = "settleFallback")
    @Retry(name = "externalBankService")
    @Observed(name = "external-bank.settle", contextualName = "external-bank-settlement")
    public void settleWithdrawal(Long walletId, BigDecimal amount) {
        simulateNetworkCall();
        log.info("External bank settlement confirmed: wallet={} amount={}", walletId, amount);
    }

    @SuppressWarnings("unused")
    private void settleFallback(Long walletId, BigDecimal amount, Throwable t) {
        log.error("External bank settlement failed after retries: wallet={} amount={} cause={}",
                walletId, amount, t.getMessage());
        throw new ExternalBankUnavailableException();
    }

    private void simulateNetworkCall() {
        try {
            Thread.sleep(10 + ThreadLocalRandom.current().nextInt(40));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (ThreadLocalRandom.current().nextDouble() < failureRate) {
            throw new ExternalBankTimeoutException();
        }
    }
}
