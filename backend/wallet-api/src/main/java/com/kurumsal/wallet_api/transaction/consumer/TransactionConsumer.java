package com.kurumsal.wallet_api.transaction.consumer;

import com.kurumsal.wallet_api.audit.domain.AuditStatus;
import com.kurumsal.wallet_api.audit.service.AuditService;
import com.kurumsal.wallet_api.config.RabbitMQConfig;
import com.kurumsal.wallet_api.transaction.domain.Transaction;
import com.kurumsal.wallet_api.transaction.domain.TransactionStatus;
import com.kurumsal.wallet_api.transaction.domain.TransactionType;
import com.kurumsal.wallet_api.transaction.event.TransactionEvent;
import com.kurumsal.wallet_api.transaction.repository.TransactionRepository;
import com.kurumsal.wallet_api.transaction.service.FraudCheckResult;
import com.kurumsal.wallet_api.transaction.service.FraudDetectionService;
import com.kurumsal.wallet_api.wallet.domain.Wallet;
import com.kurumsal.wallet_api.wallet.repository.WalletRepository;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Async, post-commit side-processing for completed transactions (§5.5 of the design doc):
 * scores outgoing money movement (WITHDRAW/TRANSFER) for fraud and flags it for review.
 * Deposits are skipped — incoming money isn't the fraud vector the heuristics target, and
 * {@link TransactionRepository#countByWalletAndStatusSince} only tracks outgoing volume.
 * Uncaught exceptions here are NACKed and routed to {@code transaction.dlq} per the
 * dead-letter binding already declared in {@link RabbitMQConfig}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionConsumer {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final FraudDetectionService fraudDetectionService;
    private final AuditService auditService;

    @RabbitListener(queues = RabbitMQConfig.TRANSACTION_QUEUE)
    @Observed(name = "transaction.consume", contextualName = "transaction-event-consume")
    @Transactional
    public void onTransactionEvent(TransactionEvent event) {
        log.info("Processing transaction event txId={} type={}", event.transactionId(), event.type());

        if (event.type() == TransactionType.DEPOSIT) {
            return;
        }

        FraudCheckResult result = fraudDetectionService.evaluate(
                event.fromWalletId(), event.userId(), event.amount());
        if (!result.suspicious()) {
            return;
        }

        Transaction tx = transactionRepository.findById(event.transactionId())
                .orElseThrow(() -> new IllegalStateException("Transaction not found: " + event.transactionId()));

        // Don't override a withdrawal that was already reversed by the compensation path.
        if (tx.getStatus() != TransactionStatus.SUCCESS) {
            return;
        }
        tx.setStatus(TransactionStatus.PENDING_REVIEW);
        transactionRepository.save(tx);

        Wallet wallet = walletRepository.findById(event.fromWalletId())
                .orElseThrow(() -> new IllegalStateException("Wallet not found: " + event.fromWalletId()));
        auditService.log(wallet, event.userId(), tx.getId(),
                "FRAUD_FLAGGED", event.amount(), AuditStatus.FAILED, event.ipAddress(), result.reason());

        log.warn("İşlem incelemeye alındı (PENDING_REVIEW): txId={} reason={} — SMS/Email uyarısı simüle edildi",
                tx.getId(), result.reason());
    }
}
