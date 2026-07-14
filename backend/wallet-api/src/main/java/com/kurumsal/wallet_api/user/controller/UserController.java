package com.kurumsal.wallet_api.user.controller;

import com.kurumsal.wallet_api.infrastructure.exception.ErrorResponse;
import com.kurumsal.wallet_api.user.dto.CreateUserRequest;
import com.kurumsal.wallet_api.user.dto.UserResponse;
import com.kurumsal.wallet_api.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Tag(name = "Users", description = "Kullanıcı kaydı ve sorgulama işlemleri")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "Yeni kullanıcı oluştur",
            description = "Yeni bir kullanıcı kaydı oluşturur ve kullanıcıya otomatik olarak sıfır bakiyeli bir cüzdan atar."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Kullanıcı başarıyla oluşturuldu",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Geçersiz istek gövdesi (validasyon hatası)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "E-posta adresi zaten kullanımda",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.created(URI.create("/api/v1/users/" + response.id())).body(response);
    }

    @Operation(
            summary = "Kullanıcı bilgisi getir",
            description = "Verilen kullanıcı ID'sine ait kullanıcı detaylarını döner."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Kullanıcı bulundu",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "404", description = "Kullanıcı bulunamadı",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(
            @Parameter(description = "Kullanıcı ID", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }
}
