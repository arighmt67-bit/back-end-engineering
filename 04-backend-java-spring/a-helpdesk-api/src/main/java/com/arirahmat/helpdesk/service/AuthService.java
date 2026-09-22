package com.arirahmat.helpdesk.service;

import com.arirahmat.helpdesk.dto.AuthResponse;
import com.arirahmat.helpdesk.dto.LoginRequest;
import com.arirahmat.helpdesk.dto.RegisterRequest;
import com.arirahmat.helpdesk.entity.Role;
import com.arirahmat.helpdesk.entity.User;
import com.arirahmat.helpdesk.exception.BusinessRuleException;
import com.arirahmat.helpdesk.repository.UserRepository;
import com.arirahmat.helpdesk.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new BusinessRuleException("Email sudah terdaftar: " + email);
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(req.password()))
                .fullName(req.fullName().trim())
                .role(Role.ROLE_USER)
                .build();

        userRepository.save(user);
        return issueToken(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email().toLowerCase().trim())
                .orElseThrow(() -> new BadCredentialsException("Kredensial tidak valid"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Kredensial tidak valid");
        }
        return issueToken(user);
    }

    private AuthResponse issueToken(User user) {
        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        return AuthResponse.bearer(token, jwtService.expiresInSeconds());
    }
}
