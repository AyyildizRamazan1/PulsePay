package com.kurumsal.wallet_api.audit.service;

import com.kurumsal.wallet_api.audit.domain.AuditLog;
import com.kurumsal.wallet_api.audit.domain.AuditStatus;
import com.kurumsal.wallet_api.audit.repository.AuditLogRepository;
import com.kurumsal.wallet_api.wallet.domain.Wallet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * REQUIRES_NEW: audit log is committed independently so failures are always recorded,
     * even when the calling transaction rolls back.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Wallet wallet, Long userId, Long transactionId,
                    String action, BigDecimal amount,
                    AuditStatus status, String ipAddress, String errorMessage) {
        try {
            AuditLog entry = AuditLog.builder()
                    .wallet(wallet)
                    .userId(userId)
                    .transactionId(transactionId)
                    .action(action)
                    .amount(amount)
                    .status(status)
                    .ipAddress(ipAddress)
                    .errorMessage(errorMessage)
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to save audit log — action={} walletId={}", action,
                    wallet != null ? wallet.getId() : null, e);
        }
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getByWallet(Long walletId, Pageable pageable) {
        return auditLogRepository.findByWalletIdOrderByTimestampDesc(walletId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getByUser(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserIdOrderByTimestampDesc(userId, pageable);
    }
}
