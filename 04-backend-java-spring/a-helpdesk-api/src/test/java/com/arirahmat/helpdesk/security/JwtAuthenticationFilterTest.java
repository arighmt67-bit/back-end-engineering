package com.arirahmat.helpdesk.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit test filter JWT memakai MockHttpServletRequest — tanpa menyalakan server.
 * Yang diuji: kapan SecurityContext terisi, dan memastikan chain SELALU dilanjutkan.
 */
class JwtAuthenticationFilterTest {

    private static final String SECRET = "rahasia-uji-minimal-32-karakter-untuk-hs256-aman";

    private final JwtService jwtService =
            new JwtService(new JwtProperties(SECRET, Duration.ofMinutes(30), "helpdesk-api"));
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private Authentication runFilter(String headerValue) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (headerValue != null) {
            request.addHeader("Authorization", headerValue);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Test
    @DisplayName("Token valid mengisi SecurityContext dengan email dan authority sesuai role")
    void token_valid_mengisi_security_context() throws Exception {
        String token = jwtService.generateToken("agent@example.com", "ROLE_AGENT");

        Authentication auth = runFilter("Bearer " + token);

        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("agent@example.com");
        assertThat(auth.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_AGENT");
    }

    @Test
    @DisplayName("Tanpa header Authorization, request diteruskan sebagai anonim")
    void tanpa_header_tetap_lanjut_tanpa_autentikasi() throws Exception {
        assertThat(runFilter(null)).isNull();
    }

    @Test
    @DisplayName("Header dengan skema selain Bearer diabaikan")
    void header_bukan_bearer_diabaikan() throws Exception {
        assertThat(runFilter("Basic YWJjOjEyMw==")).isNull();
    }

    @Test
    @DisplayName("Token tidak valid tidak mengisi SecurityContext, tapi chain tetap jalan")
    void token_tidak_valid_tidak_mengautentikasi() throws Exception {
        assertThat(runFilter("Bearer token.palsu.banget")).isNull();
    }
}
