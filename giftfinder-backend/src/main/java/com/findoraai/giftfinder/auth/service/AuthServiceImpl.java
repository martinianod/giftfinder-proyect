package com.findoraai.giftfinder.auth.service;

import com.findoraai.giftfinder.auth.dto.AuthResponse;
import com.findoraai.giftfinder.auth.dto.LoginRequest;
import com.findoraai.giftfinder.auth.dto.SignupRequest;
import com.findoraai.giftfinder.auth.model.User;
import com.findoraai.giftfinder.auth.model.Role;
import com.findoraai.giftfinder.auth.repository.UserRepository;
import com.findoraai.giftfinder.config.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public AuthResponse signup(SignupRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Email already registered.");
        }

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .build();

        userRepository.save(user);

        String token = jwtService.generateToken(user.getEmail());

        return new AuthResponse(token, user.getName(), user.getEmail());
    }

    @Override
    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtService.generateToken(user.getEmail());

        return new AuthResponse(token, user.getName(), user.getEmail());
    }
}
