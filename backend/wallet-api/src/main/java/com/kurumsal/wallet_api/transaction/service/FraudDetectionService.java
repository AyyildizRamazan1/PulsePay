package com.kurumsal.wallet_api.transaction.service;

import com.kurumsal.wallet_api.transaction.domain.TransactionStatus;
import com.kurumsal.wallet_api.transaction.repository.TransactionRepository;
import com.kurumsal.wallet_api.user.domain.User;
import com.kurumsal.wallet_api.user.repository.UserRepository;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Runs the fraud heuristics from the design doc against an already-completed outgoing
 * transaction (WITHDRAW/TRANSFER). Only the two rules backed by data actually present in
 * this schema are implemented — "transfer to a country never used before" is skipped since
 * neither {@code User} nor {@code Transaction} track a country/location.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Value("${app.fraud-detection.max-transfers-per-hour}")
    private int maxTransfersPerHour;

    @Value("${app.fraud-detection.high-value-threshold}")
    private BigDecimal highValueThreshold;

    @Value("${app.fraud-detection.new-account-days}")
    private int newAccountDays;

    @Observed(name = "fraud-detection.evaluate", contextualName = "fraud-detection-evaluate")
    public FraudCheckResult evaluate(Long walletId, Long userId, BigDecimal amount) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long recentCount = transactionRepository.countByWalletAndStatusSince(
                walletId, TransactionStatus.SUCCESS, oneHourAgo);
        if (recentCount >= maxTransfersPerHour) {
            return FraudCheckResult.suspicious(
                    "Son 1 saatte " + recentCount + " işlem yapıldı (eşik: " + maxTransfersPerHour + ")");
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user != null
                && user.getCreatedAt().isAfter(LocalDateTime.now().minusDays(newAccountDays))
                && amount.compareTo(highValueThreshold) > 0) {
            return FraudCheckResult.suspicious(
                    "Yeni hesap (< " + newAccountDays + " gün) ve yüksek tutarlı işlem: " + amount);
        }

        return FraudCheckResult.notSuspicious();
    }
}
