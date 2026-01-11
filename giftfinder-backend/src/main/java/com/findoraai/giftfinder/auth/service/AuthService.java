package com.findoraai.giftfinder.auth.service;

import com.findoraai.giftfinder.auth.dto.AuthResponse;
import com.findoraai.giftfinder.auth.dto.LoginRequest;
import com.findoraai.giftfinder.auth.dto.SignupRequest;

public interface AuthService {
    AuthResponse signup(SignupRequest request);

    AuthResponse login(LoginRequest request);
}
