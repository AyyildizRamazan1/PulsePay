package com.kurumsal.wallet_api.transaction.controller;

import com.kurumsal.wallet_api.infrastructure.exception.ErrorResponse;
import com.kurumsal.wallet_api.transaction.dto.TransactionResponse;
import com.kurumsal.wallet_api.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Transactions", description = "İşlem geçmişi sorgulama")
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @Operation(
            summary = "Cüzdana ait işlem geçmişini listele",
            description = "Belirtilen cüzdana ait tüm işlemleri (deposit/withdraw/transfer), en yeniden en " +
                    "eskiye sıralı ve sayfalı şekilde döner."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "İşlem listesi döndü",
                    content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "400", description = "Geçersiz istek parametresi",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> getTransactions(
            @Parameter(description = "Cüzdan ID", example = "1") @RequestParam Long walletId,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(transactionService.getTransactions(walletId, pageable));
    }
}
