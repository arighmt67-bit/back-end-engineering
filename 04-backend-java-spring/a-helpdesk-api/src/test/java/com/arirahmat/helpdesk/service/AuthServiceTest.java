package com.arirahmat.helpdesk.service;

import com.arirahmat.helpdesk.dto.AuthResponse;
import com.arirahmat.helpdesk.dto.LoginRequest;
import com.arirahmat.helpdesk.dto.RegisterRequest;
import com.arirahmat.helpdesk.entity.Role;
import com.arirahmat.helpdesk.entity.User;
import com.arirahmat.helpdesk.exception.BusinessRuleException;
import com.arirahmat.helpdesk.repository.UserRepository;
import com.arirahmat.helpdesk.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test AuthService dengan Mockito — repository, encoder, dan JwtService di-mock
 * supaya tidak perlu database maupun Spring context.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Register menyimpan user baru dengan role USER dan password ter-hash")
    void register_menyimpan_user_dengan_password_terhash() {
        var req = new RegisterRequest("Aconk@Example.com ", "password123", "  Ari Rahmat  ");
        when(userRepository.existsByEmail("aconk@example.com ".trim())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hash");
        when(jwtService.generateToken(anyString(), anyString())).thenReturn("token-abc");
        when(jwtService.expiresInSeconds()).thenReturn(1800L);

        AuthResponse res = authService.register(req);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getEmail()).isEqualTo("aconk@example.com");
        assertThat(saved.getFullName()).isEqualTo("Ari Rahmat");
        assertThat(saved.getRole()).isEqualTo(Role.ROLE_USER);
        assertThat(saved.getPasswordHash())
                .isEqualTo("$2a$10$hash")
                .isNotEqualTo("password123");

        assertThat(res.accessToken()).isEqualTo("token-abc");
        assertThat(res.tokenType()).isEqualTo("Bearer");
        assertThat(res.expiresInSeconds()).isEqualTo(1800L);
    }

    @Test
    @DisplayName("Register dengan email yang sudah terdaftar ditolak dan tidak menyimpan apa pun")
    void register_email_duplikat_ditolak() {
        var req = new RegisterRequest("aconk@example.com", "password123", "Ari Rahmat");
        when(userRepository.existsByEmail("aconk@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("aconk@example.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Login dengan kredensial benar mengembalikan token berisi email & role user")
    void login_kredensial_benar_menerbitkan_token() {
        User user = User.builder()
                .id(1L)
                .email("aconk@example.com")
                .passwordHash("$2a$10$hash")
                .fullName("Ari Rahmat")
                .role(Role.ROLE_AGENT)
                .build();

        when(userRepository.findByEmail("aconk@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "$2a$10$hash")).thenReturn(true);
        when(jwtService.generateToken("aconk@example.com", "ROLE_AGENT")).thenReturn("token-agent");
        when(jwtService.expiresInSeconds()).thenReturn(1800L);

        AuthResponse res = authService.login(new LoginRequest("  ACONK@example.com ", "password123"));

        assertThat(res.accessToken()).isEqualTo("token-agent");
        verify(jwtService).generateToken("aconk@example.com", "ROLE_AGENT");
    }

    @Test
    @DisplayName("Login dengan password salah ditolak dan tidak menerbitkan token")
    void login_password_salah_ditolak() {
        User user = User.builder()
                .email("aconk@example.com")
                .passwordHash("$2a$10$hash")
                .role(Role.ROLE_USER)
                .build();

        when(userRepository.findByEmail("aconk@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("salah", "$2a$10$hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("aconk@example.com", "salah")))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtService, never()).generateToken(anyString(), anyString());
    }

    @Test
    @DisplayName("Login email tak terdaftar memberi pesan sama dengan password salah (anti user enumeration)")
    void login_email_tidak_terdaftar_ditolak_dengan_pesan_generik() {
        when(userRepository.findByEmail("hantu@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("hantu@example.com", "password123")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Kredensial tidak valid");
    }
}
