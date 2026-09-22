package com.arirahmat.helpdesk.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Properti JWT.
 * <p>expiration ditulis sebagai Duration (contoh: 30m, 1h) sehingga satuannya eksplisit.
 * Menyimpan angka telanjang seperti "1800" rawan ditafsirkan sebagai milidetik oleh
 * library lain dan menghasilkan token yang langsung kedaluwarsa.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration expiration, String issuer) {
}
