package com.arirahmat.helpdesk.security;

import com.arirahmat.helpdesk.dto.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * Mengembalikan 401 Unauthorized untuk request yang BELUM terautentikasi.
 * <p>
 * Tanpa entry point ini Spring Security memakai perilaku default dan membalas 403 Forbidden
 * untuk request anonim. Secara semantik itu keliru: 401 = belum login / token tidak valid,
 * 403 = sudah login tapi role-nya tidak cukup. Pembedaan ini penting bagi klien API
 * karena 401 berarti "ambil token baru", sedangkan 403 berarti "jangan diulang".
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiError body = new ApiError(
                Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "Token tidak ada atau tidak valid",
                request.getRequestURI(),
                null);

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
