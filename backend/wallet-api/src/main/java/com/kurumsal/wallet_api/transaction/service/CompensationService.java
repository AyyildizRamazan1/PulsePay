package com.kurumsal.wallet_api.transaction.service;

import com.kurumsal.wallet_api.audit.domain.AuditStatus;
import com.kurumsal.wallet_api.audit.service.AuditService;
import com.kurumsal.wallet_api.transaction.domain.Transaction;
import com.kurumsal.wallet_api.transaction.domain.TransactionStatus;
import com.kurumsal.wallet_api.transaction.repository.TransactionRepository;
import com.kurumsal.wallet_api.wallet.cache.WalletCacheService;
import com.kurumsal.wallet_api.wallet.domain.Wallet;
import com.kurumsal.wallet_api.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Reverses a wallet debit whose external settlement ultimately failed (circuit breaker/retry
 * exhausted). Called from within the caller's existing transaction — see
 * {@link com.kurumsal.wallet_api.wallet.service.WalletService#withdraw} — so the compensating
 * credit and the FAILED status commit atomically alongside it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompensationService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final WalletCacheService walletCacheService;
    private final AuditService auditService;

    public void reverseWithdrawal(Wallet wallet, Transaction tx, BigDecimal amount, String ipAddress, String reason) {
        wallet.credit(amount);
        walletRepository.save(wallet);
        walletCacheService.evict(wallet.getId());

        tx.setStatus(TransactionStatus.FAILED);
        tx.setErrorMessage(reason);
        transactionRepository.save(tx);

        auditService.log(wallet, wallet.getUser().getId(), tx.getId(),
                "WITHDRAW_COMPENSATED", amount, AuditStatus.FAILED, ipAddress, reason);

        log.warn("Compensated withdrawal txId={} wallet={} amount={} reason={}",
                tx.getId(), wallet.getId(), amount, reason);
    }
}
