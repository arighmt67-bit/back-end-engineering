package com.arirahmat.helpdesk.controller;

import com.arirahmat.helpdesk.dto.AuthResponse;
import com.arirahmat.helpdesk.dto.LoginRequest;
import com.arirahmat.helpdesk.dto.RegisterRequest;
import com.arirahmat.helpdesk.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "1. Authentication", description = "Registrasi dan login untuk memperoleh token JWT")
@SecurityRequirements
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Daftar akun baru",
               description = "Membuat user dengan role ROLE_USER dan langsung mengembalikan token JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registrasi berhasil"),
            @ApiResponse(responseCode = "400", description = "Payload tidak valid", content = @io.swagger.v3.oas.annotations.media.Content),
            @ApiResponse(responseCode = "409", description = "Email sudah terdaftar", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Login dan dapatkan token JWT",
               description = "Salin nilai accessToken, lalu tempel lewat tombol Authorize di Swagger UI.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login berhasil"),
            @ApiResponse(responseCode = "401", description = "Email atau password salah", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
