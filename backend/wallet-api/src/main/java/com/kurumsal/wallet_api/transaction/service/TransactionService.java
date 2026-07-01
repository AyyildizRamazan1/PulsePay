package com.kurumsal.wallet_api.transaction.service;

import com.kurumsal.wallet_api.audit.domain.AuditStatus;
import com.kurumsal.wallet_api.audit.service.AuditService;
import com.kurumsal.wallet_api.infrastructure.exception.InsufficientBalanceException;
import com.kurumsal.wallet_api.infrastructure.exception.WalletNotFoundException;
import com.kurumsal.wallet_api.transaction.domain.Transaction;
import com.kurumsal.wallet_api.transaction.domain.TransactionStatus;
import com.kurumsal.wallet_api.transaction.domain.TransactionType;
import com.kurumsal.wallet_api.transaction.dto.TransactionResponse;
import com.kurumsal.wallet_api.transaction.dto.TransferRequest;
import com.kurumsal.wallet_api.transaction.repository.TransactionRepository;
import com.kurumsal.wallet_api.wallet.cache.WalletCacheService;
import com.kurumsal.wallet_api.wallet.domain.Wallet;
import com.kurumsal.wallet_api.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final AuditService auditService;
    private final IdempotencyService idempotencyService;
    private final WalletCacheService walletCacheService;

    @Transactional
    public TransactionResponse transfer(Long fromWalletId, TransferRequest request,
                                        String idempotencyKey, String ipAddress) {
        if (idempotencyKey != null) {
            Optional<TransactionResponse> cached = idempotencyService.get(idempotencyKey, TransactionResponse.class);
            if (cached.isPresent()) return cached.get();
        }

        Long toWalletId = request.toWalletId();

        // Lock wallets in consistent ID order to prevent deadlocks
        Long firstId  = Math.min(fromWalletId, toWalletId);
        Long secondId = Math.max(fromWalletId, toWalletId);

        Wallet first  = walletRepository.findByIdWithPessimisticLock(firstId)
                .orElseThrow(() -> new WalletNotFoundException(firstId));
        Wallet second = walletRepository.findByIdWithPessimisticLock(secondId)
                .orElseThrow(() -> new WalletNotFoundException(secondId));

        Wallet fromWallet = firstId.equals(fromWalletId) ? first : second;
        Wallet toWallet   = firstId.equals(toWalletId)   ? first : second;

        try {
            fromWallet.debit(request.amount());
        } catch (IllegalStateException e) {
            auditService.log(fromWallet, fromWallet.getUser().getId(), null,
                    "TRANSFER_OUT", request.amount(), AuditStatus.FAILED, ipAddress, "Insufficient balance");
            throw new InsufficientBalanceException();
        }

        toWallet.credit(request.amount());

        walletRepository.save(fromWallet);
        walletRepository.save(toWallet);
        walletCacheService.evict(fromWalletId);
        walletCacheService.evict(toWalletId);

        Transaction tx = Transaction.builder()
                .fromWallet(fromWallet)
                .toWallet(toWallet)
                .amount(request.amount())
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.SUCCESS)
                .idempotencyKey(idempotencyKey)
                .completedAt(LocalDateTime.now())
                .build();
        transactionRepository.save(tx);

        auditService.log(fromWallet, fromWallet.getUser().getId(), tx.getId(),
                "TRANSFER_OUT", request.amount(), AuditStatus.SUCCESS, ipAddress, null);
        auditService.log(toWallet, toWallet.getUser().getId(), tx.getId(),
                "TRANSFER_IN", request.amount(), AuditStatus.SUCCESS, ipAddress, null);

        log.info("Transfer id={} fromWallet={} toWallet={} amount={}",
                tx.getId(), fromWalletId, toWalletId, request.amount());

        TransactionResponse response = TransactionResponse.from(tx);
        if (idempotencyKey != null) idempotencyService.save(idempotencyKey, response);
        return response;
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactions(Long walletId, Pageable pageable) {
        return transactionRepository
                .findByFromWalletIdOrToWalletIdOrderByCreatedAtDesc(walletId, walletId, pageable)
                .map(TransactionResponse::from);
    }
}
