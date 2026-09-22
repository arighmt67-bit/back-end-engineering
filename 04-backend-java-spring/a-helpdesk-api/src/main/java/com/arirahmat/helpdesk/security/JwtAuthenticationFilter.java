package com.arirahmat.helpdesk.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(PREFIX.length()).trim();
        jwtService.parse(token).ifPresent(claims -> {
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                setAuthentication(claims, request);
            }
        });

        chain.doFilter(request, response);
    }

    /**
     * Secara default OncePerRequestFilter melewati ERROR dispatch, sehingga SecurityContext
     * kosong saat container mem-forward ke /error. Akibatnya 403 (sudah login, role kurang)
     * berubah menjadi 401 karena /error dianggap request anonim.
     * Mengembalikan false membuat token tetap diproses pada ERROR dispatch.
     */
    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    private void setAuthentication(Claims claims, HttpServletRequest request) {
        String email = claims.getSubject();
        String role = claims.get("role", String.class);
        var authorities = List.of(new SimpleGrantedAuthority(role));
        var auth = new UsernamePasswordAuthenticationToken(email, null, authorities);
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
