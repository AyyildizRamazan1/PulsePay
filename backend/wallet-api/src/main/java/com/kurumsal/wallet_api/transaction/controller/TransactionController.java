package com.kurumsal.wallet_api.transaction.controller;

import com.kurumsal.wallet_api.transaction.dto.TransactionResponse;
import com.kurumsal.wallet_api.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> getTransactions(
            @RequestParam Long walletId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(transactionService.getTransactions(walletId, pageable));
    }
}
