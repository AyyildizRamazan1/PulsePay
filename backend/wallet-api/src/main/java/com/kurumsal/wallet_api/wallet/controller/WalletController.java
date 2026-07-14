package com.kurumsal.wallet_api.wallet.controller;

import com.kurumsal.wallet_api.transaction.dto.TransactionResponse;
import com.kurumsal.wallet_api.transaction.dto.TransferRequest;
import com.kurumsal.wallet_api.transaction.service.TransactionService;
import com.kurumsal.wallet_api.wallet.dto.DepositRequest;
import com.kurumsal.wallet_api.wallet.dto.WalletResponse;
import com.kurumsal.wallet_api.wallet.dto.WithdrawRequest;
import com.kurumsal.wallet_api.wallet.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final TransactionService transactionService;

    @GetMapping("/{walletId}/balance")
    public ResponseEntity<WalletResponse> getBalance(@PathVariable Long walletId) {
        return ResponseEntity.ok(walletService.getBalance(walletId));
    }

    @PostMapping("/{walletId}/deposit")
    public ResponseEntity<TransactionResponse> deposit(
            @PathVariable Long walletId,
            @Valid @RequestBody DepositRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest servletRequest) {
        TransactionResponse response = walletService.deposit(
                walletId, request.amount(), idempotencyKey, servletRequest.getRemoteAddr());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{walletId}/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(
            @PathVariable Long walletId,
            @Valid @RequestBody WithdrawRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest servletRequest) {
        TransactionResponse response = walletService.withdraw(
                walletId, request.amount(), idempotencyKey, servletRequest.getRemoteAddr());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{walletId}/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            @PathVariable Long walletId,
            @Valid @RequestBody TransferRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest servletRequest) {
        TransactionResponse response = transactionService.transfer(
                walletId, request, idempotencyKey, servletRequest.getRemoteAddr());
        return ResponseEntity.ok(response);
    }
}
