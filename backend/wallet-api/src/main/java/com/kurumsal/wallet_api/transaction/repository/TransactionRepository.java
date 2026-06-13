package com.kurumsal.wallet_api.transaction.repository;

import com.kurumsal.wallet_api.transaction.domain.Transaction;
import com.kurumsal.wallet_api.transaction.domain.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    Page<Transaction> findByFromWalletIdOrToWalletIdOrderByCreatedAtDesc(
            Long fromWalletId, Long toWalletId, Pageable pageable);

    @Query("""
            SELECT COUNT(t) FROM Transaction t
            WHERE t.fromWallet.id = :walletId
              AND t.status = :status
              AND t.createdAt >= :since
            """)
    long countByWalletAndStatusSince(
            @Param("walletId") Long walletId,
            @Param("status") TransactionStatus status,
            @Param("since") LocalDateTime since);
}