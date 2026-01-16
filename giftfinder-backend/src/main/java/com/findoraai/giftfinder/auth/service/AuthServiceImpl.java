package com.findoraai.giftfinder.auth.service;

import com.findoraai.giftfinder.auth.dto.AuthResponse;
import com.findoraai.giftfinder.auth.dto.LoginRequest;
import com.findoraai.giftfinder.auth.dto.SignupRequest;
import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.auth.model.Role;
import com.findoraai.giftfinder.auth.repository.UserRepository;
import com.findoraai.giftfinder.config.exception.DuplicateEmailException;
import com.findoraai.giftfinder.config.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        log.info("Signup attempt for email: {}, name: {}", request.email(), request.name());

        if (userRepository.existsByEmail(request.email())) {
            log.warn("Signup failed - email already exists: {}", request.email());
            throw new DuplicateEmailException("Email already registered", request.email());
        }

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .build();

        userRepository.save(user);
        log.info("User registered successfully - email: {}, name: {}", user.getEmail(), user.getName());

        String token = jwtService.generateToken(user.getEmail());

        return new AuthResponse(token, user.getName(), user.getEmail());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.email());

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    log.warn("Login failed - user not found: {}", request.email());
                    return new BadCredentialsException("Invalid credentials");
                });

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            log.warn("Login failed - invalid password for email: {}", request.email());
            throw new BadCredentialsException("Invalid credentials");
        }

        log.info("User logged in successfully - email: {}", user.getEmail());
        String token = jwtService.generateToken(user.getEmail());

        return new AuthResponse(token, user.getName(), user.getEmail());
    }
}
