package com.example.projecto.service;

import com.example.projecto.model.dto.request.ChangePasswordRequest;
import com.example.projecto.model.dto.request.LoginRequest;
import com.example.projecto.model.dto.request.RefreshTokenRequest;
import com.example.projecto.model.dto.request.RegisterRequest;
import com.example.projecto.model.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    AuthResponse refresh(RefreshTokenRequest request);
    void logout(String token);
    void register(RegisterRequest request);
    void changePassword(String username, ChangePasswordRequest request);
}