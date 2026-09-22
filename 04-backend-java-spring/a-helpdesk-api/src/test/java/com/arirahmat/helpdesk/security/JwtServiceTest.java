package com.arirahmat.helpdesk.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test murni JwtService — tanpa Spring context, jadi cepat.
 *
 * Fokus pengujian:
 *  1. Token valid bisa di-parse balik (round trip).
 *  2. Satuan expiration benar (Duration 30m -> 1800 detik, BUKAN milidetik).
 *  3. Token kedaluwarsa, signature salah, issuer salah, dan sampah -> Optional.empty().
 */
class JwtServiceTest {

    private static final String SECRET = "rahasia-uji-minimal-32-karakter-untuk-hs256-aman";
    private static final String ISSUER = "helpdesk-api";

    private final JwtProperties props =
            new JwtProperties(SECRET, Duration.ofMinutes(30), ISSUER);
    private final JwtService jwtService = new JwtService(props);

    private SecretKey keyOf(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Token yang digenerate bisa di-parse balik dengan subject & role utuh")
    void generateToken_lalu_parse_mengembalikan_claim_yang_sama() {
        String token = jwtService.generateToken("aconk@example.com", "ROLE_AGENT");

        Optional<Claims> parsed = jwtService.parse(token);

        assertThat(parsed).isPresent();
        Claims claims = parsed.orElseThrow();
        assertThat(claims.getSubject()).isEqualTo("aconk@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("ROLE_AGENT");
        assertThat(claims.getIssuer()).isEqualTo(ISSUER);
    }

    @Test
    @DisplayName("Expiration dibaca sebagai DETIK, bukan milidetik (30m = 1800 detik)")
    void expiresInSeconds_menghitung_duration_dalam_detik() {
        assertThat(jwtService.expiresInSeconds()).isEqualTo(1800L);
    }

    @Test
    @DisplayName("Selisih exp-iat pada token sesuai durasi konfigurasi, dengan toleransi 2 detik")
    void selisih_exp_dan_iat_sesuai_konfigurasi() {
        String token = jwtService.generateToken("aconk@example.com", "ROLE_USER");
        Claims claims = jwtService.parse(token).orElseThrow();

        long lifetimeSeconds =
                (claims.getExpiration().getTime() - claims.getIssuedAt().getTime()) / 1000;

        assertThat(lifetimeSeconds).isBetween(1798L, 1802L);
    }

    @Test
    @DisplayName("Token kedaluwarsa ditolak dan mengembalikan Optional.empty()")
    void token_kedaluwarsa_ditolak() {
        Instant past = Instant.now().minus(Duration.ofHours(2));
        String expired = Jwts.builder()
                .subject("aconk@example.com")
                .claim("role", "ROLE_USER")
                .issuer(ISSUER)
                .issuedAt(Date.from(past))
                .expiration(Date.from(past.plus(Duration.ofMinutes(30))))
                .signWith(keyOf(SECRET))
                .compact();

        assertThat(jwtService.parse(expired)).isEmpty();
    }

    @Test
    @DisplayName("Token yang ditandatangani secret lain ditolak")
    void token_dengan_signature_salah_ditolak() {
        String foreign = Jwts.builder()
                .subject("penyusup@example.com")
                .claim("role", "ROLE_ADMIN")
                .issuer(ISSUER)
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(Duration.ofMinutes(30))))
                .signWith(keyOf("secret-penyerang-yang-juga-32-karakter-lebih-panjang"))
                .compact();

        assertThat(jwtService.parse(foreign)).isEmpty();
    }

    @Test
    @DisplayName("Token dengan issuer berbeda ditolak")
    void token_dengan_issuer_salah_ditolak() {
        String wrongIssuer = Jwts.builder()
                .subject("aconk@example.com")
                .claim("role", "ROLE_USER")
                .issuer("layanan-lain")
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(Duration.ofMinutes(30))))
                .signWith(keyOf(SECRET))
                .compact();

        assertThat(jwtService.parse(wrongIssuer)).isEmpty();
    }

    @Test
    @DisplayName("Token berformat sampah / kosong tidak melempar exception, hanya empty")
    void token_sampah_tidak_melempar_exception() {
        assertThat(jwtService.parse("bukan.token.jwt")).isEmpty();
        assertThat(jwtService.parse("")).isEmpty();
        assertThat(jwtService.parse("aaaa")).isEmpty();
    }
}
