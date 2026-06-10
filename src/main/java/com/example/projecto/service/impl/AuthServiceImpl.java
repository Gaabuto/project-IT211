package com.example.projecto.service.impl;


import com.example.projecto.model.dto.request.*;
import com.example.projecto.model.dto.response.AuthResponse;
import com.example.projecto.model.entity.RoleEnum;
import org.springframework.beans.factory.annotation.Value;
import com.example.projecto.model.entity.User;
import com.example.projecto.exception.DuplicateResourceException;
import com.example.projecto.exception.InvalidStateException;
import com.example.projecto.exception.ResourceNotFoundException;
import com.example.projecto.repository.UserRepository;
import com.example.projecto.service.AuthService;
import com.example.projecto.security.jwt.JwtUtil;
import com.example.projecto.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;
    // Thêm field:
    private final RedisTemplate<String, String> redisTemplate;
    private final JavaMailSender mailSender;

    @Value("${app.reset-token.expiration}")
    private long resetTokenExpiration;

    private static final String RESET_PREFIX = "reset:";

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        // Không báo lỗi nếu email không tồn tại (tránh user enumeration)
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            redisTemplate.opsForValue().set(
                    RESET_PREFIX + token,
                    user.getUsername(),
                    Duration.ofMillis(resetTokenExpiration)
            );
            sendResetEmail(user.getEmail(), token);
            log.info("Password reset token generated for user '{}'", user.getUsername());
        });
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String key = RESET_PREFIX + request.getToken();
        String username = redisTemplate.opsForValue().get(key);

        if (username == null) {
            throw new InvalidStateException("Reset token is invalid or has expired");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        redisTemplate.delete(key); // Xóa token sau khi dùng
        log.info("Password reset successfully for user '{}'", username);
    }

    private void sendResetEmail(String toEmail, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Password Reset Request");
        message.setText("Use the following token to reset your password:\n\n" + token
                + "\n\nThis token expires in 15 minutes.");
        mailSender.send(message);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        // Spring Security sẽ ném BadCredentialsException hoặc DisabledException nếu thất bại
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String accessToken  = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        log.info("User '{}' logged in successfully", user.getUsername());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (jwtUtil.isTokenExpired(refreshToken)) {
            throw new InvalidStateException("Refresh token has expired, please login again");
        }
        if (tokenBlacklistService.isBlacklisted(refreshToken)) {
            throw new InvalidStateException("Refresh token has been revoked");
        }

        String username = jwtUtil.extractUsername(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        String newAccessToken = jwtUtil.generateAccessToken(userDetails);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken) // giữ nguyên refresh token
                .username(username)
                .build();
    }

    @Override
    @Transactional
    public void logout(String token) {
        Date expiresAt = jwtUtil.extractExpiration(token);
        long ttl = expiresAt.getTime() - System.currentTimeMillis();
        if (ttl > 0) {
            tokenBlacklistService.blacklist(token, ttl);
        }
        log.info("Token blacklisted in Redis successfully");
    }

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .fullName(request.getFullName())
                .role(RoleEnum.STUDENT)
                .isActive(true)
                .build();

        userRepository.save(user);
        log.info("New student registered: {}", user.getUsername());
    }

    @Override
    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new InvalidStateException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for user '{}'", username);
    }
}