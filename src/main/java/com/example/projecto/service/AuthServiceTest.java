package com.example.projecto.service;

import com.example.projecto.exception.DuplicateResourceException;
import com.example.projecto.exception.InvalidStateException;
import com.example.projecto.model.dto.request.ChangePasswordRequest;
import com.example.projecto.model.dto.request.LoginRequest;
import com.example.projecto.model.dto.request.RegisterRequest;
import com.example.projecto.model.dto.response.AuthResponse;
import com.example.projecto.model.entity.RoleEnum;
import com.example.projecto.model.entity.User;
import com.example.projecto.repository.TokenBlacklistRepository;
import com.example.projecto.repository.UserRepository;
import com.example.projecto.security.jwt.JwtUtil;
import com.example.projecto.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock AuthenticationManager authenticationManager;
    @Mock UserRepository userRepository;
    @Mock TokenBlacklistRepository tokenBlacklistRepository;
    @Mock JwtUtil jwtUtil;
    @Mock PasswordEncoder passwordEncoder;
    @Mock UserDetailsService userDetailsService;

    @InjectMocks AuthServiceImpl authService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .username("student01")
                .passwordHash("$2a$12$encodedPassword")
                .email("student@edu.com")
                .fullName("Test Student")
                .role(RoleEnum.STUDENT)
                .isActive(true)
                .build();
    }

    // Test 1: Đăng nhập thành công
    @Test
    void login_success_returnsTokens() {
        LoginRequest request = new LoginRequest();
        request.setUsername("student01");
        request.setPassword("password123");

        when(userRepository.findByUsername("student01")).thenReturn(Optional.of(mockUser));
        when(jwtUtil.generateAccessToken(mockUser)).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(mockUser)).thenReturn("refresh-token");

        AuthResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getUsername()).isEqualTo("student01");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    // Test 2: Đăng nhập sai mật khẩu
    @Test
    void login_wrongPassword_throwsBadCredentials() {
        LoginRequest request = new LoginRequest();
        request.setUsername("student01");
        request.setPassword("wrongpassword");

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    // Test 3: Đăng ký thành công
    @Test
    void register_success_savesUser() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("password123");
        request.setEmail("new@edu.com");
        request.setFullName("New User");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@edu.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPass");

        authService.register(request);

        verify(userRepository).save(argThat(u ->
                u.getUsername().equals("newuser") &&
                        u.getRole() == RoleEnum.STUDENT
        ));
    }

    // Test 4: Đăng ký username đã tồn tại
    @Test
    void register_duplicateUsername_throwsException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("student01");
        request.setPassword("password123");
        request.setEmail("other@edu.com");
        request.setFullName("Other");

        when(userRepository.existsByUsername("student01")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username already exists");
    }

    // Test 5: Đổi mật khẩu sai mật khẩu cũ
    @Test
    void changePassword_wrongCurrentPassword_throwsException() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrongOld");
        request.setNewPassword("newPassword123");

        when(userRepository.findByUsername("student01")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("wrongOld", mockUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword("student01", request))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("Current password is incorrect");
    }
}