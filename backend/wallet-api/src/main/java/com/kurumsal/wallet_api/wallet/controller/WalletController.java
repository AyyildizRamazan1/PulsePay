package com.kurumsal.wallet_api.wallet.controller;

import com.kurumsal.wallet_api.infrastructure.exception.ErrorResponse;
import com.kurumsal.wallet_api.transaction.dto.TransactionResponse;
import com.kurumsal.wallet_api.transaction.dto.TransferRequest;
import com.kurumsal.wallet_api.transaction.service.TransactionService;
import com.kurumsal.wallet_api.wallet.dto.DepositRequest;
import com.kurumsal.wallet_api.wallet.dto.WalletResponse;
import com.kurumsal.wallet_api.wallet.dto.WithdrawRequest;
import com.kurumsal.wallet_api.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Wallets", description = "Cüzdan bakiye sorgulama, para yatırma/çekme ve transfer işlemleri")
@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final TransactionService transactionService;

    @Operation(
            summary = "Cüzdan bakiyesi sorgula",
            description = "Verilen cüzdan ID'sine ait güncel bakiyeyi döner. Sonuç Redis üzerinden cache'lenir."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bakiye bilgisi döndü",
                    content = @Content(schema = @Schema(implementation = WalletResponse.class))),
            @ApiResponse(responseCode = "404", description = "Cüzdan bulunamadı",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{walletId}/balance")
    public ResponseEntity<WalletResponse> getBalance(
            @Parameter(description = "Cüzdan ID", example = "1") @PathVariable Long walletId) {
        return ResponseEntity.ok(walletService.getBalance(walletId));
    }

    @Operation(
            summary = "Cüzdana para yatır",
            description = "Belirtilen tutarı cüzdana ekler. Aynı Idempotency-Key ile tekrar gönderilen istekler " +
                    "işlemi tekrarlamaz; önceki sonuç aynen döner."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Para yatırma işlemi başarılı",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Geçersiz tutar (validasyon hatası)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Cüzdan bulunamadı",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{walletId}/deposit")
    public ResponseEntity<TransactionResponse> deposit(
            @Parameter(description = "Cüzdan ID", example = "1") @PathVariable Long walletId,
            @Valid @RequestBody DepositRequest request,
            @Parameter(description = "İsteğin yeniden işlenmesini önlemek için benzersiz anahtar (opsiyonel)")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest servletRequest) {
        TransactionResponse response = walletService.deposit(
                walletId, request.amount(), idempotencyKey, servletRequest.getRemoteAddr());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Cüzdandan para çek",
            description = "Belirtilen tutarı cüzdandan düşer. Bakiye yetersizse 422 döner. Aynı Idempotency-Key " +
                    "ile tekrar gönderilen istekler işlemi tekrarlamaz."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Para çekme işlemi başarılı",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Geçersiz tutar (validasyon hatası)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Cüzdan bulunamadı",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Yetersiz bakiye",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{walletId}/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(
            @Parameter(description = "Cüzdan ID", example = "1") @PathVariable Long walletId,
            @Valid @RequestBody WithdrawRequest request,
            @Parameter(description = "İsteğin yeniden işlenmesini önlemek için benzersiz anahtar (opsiyonel)")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest servletRequest) {
        TransactionResponse response = walletService.withdraw(
                walletId, request.amount(), idempotencyKey, servletRequest.getRemoteAddr());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Başka bir cüzdana transfer yap",
            description = "Belirtilen tutarı kaynak cüzdandan (path'teki walletId) hedef cüzdana (istek " +
                    "gövdesindeki toWalletId) aktarır. Kilitler kilitlenme (deadlock) riskini önlemek için " +
                    "ID sırasına göre alınır."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transfer başarılı",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Geçersiz istek (validasyon hatası)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Kaynak veya hedef cüzdan bulunamadı",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Yetersiz bakiye",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{walletId}/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            @Parameter(description = "Kaynak cüzdan ID", example = "1") @PathVariable Long walletId,
            @Valid @RequestBody TransferRequest request,
            @Parameter(description = "İsteğin yeniden işlenmesini önlemek için benzersiz anahtar (opsiyonel)")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest servletRequest) {
        TransactionResponse response = transactionService.transfer(
                walletId, request, idempotencyKey, servletRequest.getRemoteAddr());
        return ResponseEntity.ok(response);
    }
}
