package com.kurumsal.wallet_api.wallet.service;

import com.kurumsal.wallet_api.audit.domain.AuditStatus;
import com.kurumsal.wallet_api.audit.service.AuditService;
import com.kurumsal.wallet_api.infrastructure.exception.ExternalBankUnavailableException;
import com.kurumsal.wallet_api.infrastructure.exception.InsufficientBalanceException;
import com.kurumsal.wallet_api.infrastructure.exception.WalletNotFoundException;
import com.kurumsal.wallet_api.infrastructure.external.ExternalBankService;
import com.kurumsal.wallet_api.transaction.domain.Transaction;
import com.kurumsal.wallet_api.transaction.domain.TransactionStatus;
import com.kurumsal.wallet_api.transaction.domain.TransactionType;
import com.kurumsal.wallet_api.transaction.dto.TransactionResponse;
import com.kurumsal.wallet_api.transaction.event.TransactionEvent;
import com.kurumsal.wallet_api.transaction.event.TransactionEventPublisher;
import com.kurumsal.wallet_api.transaction.repository.TransactionRepository;
import com.kurumsal.wallet_api.transaction.service.CompensationService;
import com.kurumsal.wallet_api.transaction.service.IdempotencyService;
import com.kurumsal.wallet_api.wallet.cache.WalletCacheService;
import com.kurumsal.wallet_api.wallet.domain.Wallet;
import com.kurumsal.wallet_api.wallet.dto.WalletResponse;
import com.kurumsal.wallet_api.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final AuditService auditService;
    private final IdempotencyService idempotencyService;
    private final WalletCacheService walletCacheService;
    private final ExternalBankService externalBankService;
    private final CompensationService compensationService;
    private final TransactionEventPublisher transactionEventPublisher;

    @Transactional(readOnly = true)
    public WalletResponse getBalance(Long walletId) {
        Optional<BigDecimal> cached = walletCacheService.getBalance(walletId);
        if (cached.isPresent()) {
            Wallet wallet = walletRepository.findById(walletId)
                    .orElseThrow(() -> new WalletNotFoundException(walletId));
            return new WalletResponse(wallet.getId(), wallet.getUser().getId(), cached.get(), wallet.getUpdatedAt());
        }
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException(walletId));
        walletCacheService.cacheBalance(walletId, wallet.getBalance());
        return WalletResponse.from(wallet);
    }

    @Transactional
    public TransactionResponse deposit(Long walletId, BigDecimal amount,
                                       String idempotencyKey, String ipAddress) {
        if (idempotencyKey != null) {
            Optional<TransactionResponse> cached = idempotencyService.get(idempotencyKey, TransactionResponse.class);
            if (cached.isPresent()) return cached.get();
        }

        Wallet wallet = walletRepository.findByIdWithPessimisticLock(walletId)
                .orElseThrow(() -> new WalletNotFoundException(walletId));

        wallet.credit(amount);
        walletRepository.save(wallet);
        walletCacheService.evict(walletId);

        Transaction tx = Transaction.builder()
                .toWallet(wallet)
                .amount(amount)
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.SUCCESS)
                .idempotencyKey(idempotencyKey)
                .completedAt(LocalDateTime.now())
                .build();
        transactionRepository.save(tx);

        auditService.log(wallet, wallet.getUser().getId(), tx.getId(),
                "DEPOSIT", amount, AuditStatus.SUCCESS, ipAddress, null);

        TransactionResponse response = TransactionResponse.from(tx);
        if (idempotencyKey != null) idempotencyService.save(idempotencyKey, response);

        transactionEventPublisher.publish(new TransactionEvent(
                tx.getId(), null, walletId, wallet.getUser().getId(), amount,
                TransactionType.DEPOSIT, ipAddress, LocalDateTime.now()));

        return response;
    }

    /**
     * noRollbackFor: on external-settlement failure the wallet debit is compensated (credited
     * back) and the transaction row is marked FAILED within this same transaction, then the
     * exception is thrown to inform the caller. Those compensating writes must commit — not the
     * default Spring behavior of rolling back the whole method on a RuntimeException — so they
     * are excluded from rollback here. (The compensation itself must stay in *this* transaction
     * rather than a REQUIRES_NEW one: a separate transaction trying to update the same wallet
     * row would block behind the pessimistic lock this transaction is still holding, deadlocking
     * against itself.)
     */
    @Transactional(noRollbackFor = ExternalBankUnavailableException.class)
    public TransactionResponse withdraw(Long walletId, BigDecimal amount,
                                        String idempotencyKey, String ipAddress) {
        if (idempotencyKey != null) {
            Optional<TransactionResponse> cached = idempotencyService.get(idempotencyKey, TransactionResponse.class);
            if (cached.isPresent()) return cached.get();
        }

        Wallet wallet = walletRepository.findByIdWithPessimisticLock(walletId)
                .orElseThrow(() -> new WalletNotFoundException(walletId));

        try {
            wallet.debit(amount);
        } catch (IllegalStateException e) {
            auditService.log(wallet, wallet.getUser().getId(), null,
                    "WITHDRAW", amount, AuditStatus.FAILED, ipAddress, "Insufficient balance");
            throw new InsufficientBalanceException();
        }

        walletRepository.save(wallet);
        walletCacheService.evict(walletId);

        // fromWallet = source, toWallet = null (money leaves the system; V2 migration allows this)
        Transaction tx = Transaction.builder()
                .fromWallet(wallet)
                .amount(amount)
                .type(TransactionType.WITHDRAW)
                .status(TransactionStatus.SUCCESS)
                .idempotencyKey(idempotencyKey)
                .completedAt(LocalDateTime.now())
                .build();
        transactionRepository.save(tx);

        try {
            externalBankService.settleWithdrawal(walletId, amount);
        } catch (ExternalBankUnavailableException e) {
            compensationService.reverseWithdrawal(wallet, tx, amount, ipAddress,
                    "External bank settlement failed — withdrawal reversed");
            throw e;
        }

        auditService.log(wallet, wallet.getUser().getId(), tx.getId(),
                "WITHDRAW", amount, AuditStatus.SUCCESS, ipAddress, null);

        TransactionResponse response = TransactionResponse.from(tx);
        if (idempotencyKey != null) idempotencyService.save(idempotencyKey, response);

        transactionEventPublisher.publish(new TransactionEvent(
                tx.getId(), walletId, null, wallet.getUser().getId(), amount,
                TransactionType.WITHDRAW, ipAddress, LocalDateTime.now()));

        return response;
    }
}
