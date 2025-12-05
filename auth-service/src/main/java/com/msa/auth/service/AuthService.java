package com.msa.auth.service;

import com.msa.auth.domain.AuthUser;
import com.msa.auth.repository.AuthUserRepository;
import com.msa.auth.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core authentication business logic.
 *
 * Follows the pattern of returning JWT tokens on both register and login,
 * so the client can immediately make authenticated requests after registration
 * without an extra login round-trip.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthUserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public String register(String email, String password, String name) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered: " + email);
        }

        AuthUser user = AuthUser.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .name(name)
                .role(AuthUser.Role.USER)
                .build();

        AuthUser saved = userRepository.save(user);

        return jwtTokenProvider.generateToken(saved.getId(), saved.getRole().name());
    }

    @Transactional(readOnly = true)
    public String login(String email, String password) {
        AuthUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        return jwtTokenProvider.generateToken(user.getId(), user.getRole().name());
    }
}
