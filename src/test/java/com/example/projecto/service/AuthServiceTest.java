package com.example.projecto.service;

import com.example.projecto.exception.DuplicateResourceException;
import com.example.projecto.exception.InvalidStateException;
import com.example.projecto.model.dto.request.ChangePasswordRequest;
import com.example.projecto.model.dto.request.LoginRequest;
import com.example.projecto.model.dto.request.RegisterRequest;
import com.example.projecto.model.dto.response.AuthResponse;
import com.example.projecto.model.entity.RoleEnum;
import com.example.projecto.model.entity.User;
import com.example.projecto.repository.UserRepository;
import com.example.projecto.security.jwt.JwtUtil;
import com.example.projecto.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private AuthServiceImpl authService;

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

    @Test
    @DisplayName("AuthService - Login thành công trả về token")
    void testLogin_Success() {
        LoginRequest request = new LoginRequest();
        request.setUsername("student01");
        request.setPassword("password123");

        Mockito.when(userRepository.findByUsername("student01")).thenReturn(Optional.of(mockUser));
        Mockito.when(jwtUtil.generateAccessToken(mockUser)).thenReturn("access-token");
        Mockito.when(jwtUtil.generateRefreshToken(mockUser)).thenReturn("refresh-token");

        AuthResponse result = authService.login(request);

        assertNotNull(result);
        assertEquals("access-token", result.getAccessToken());
        assertEquals("refresh-token", result.getRefreshToken());
        assertEquals("student01", result.getUsername());
        assertEquals("STUDENT", result.getRole());

        Mockito.verify(authenticationManager, Mockito.times(1))
                .authenticate(Mockito.any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("AuthService - Login sai password ném BadCredentialsException")
    void testLogin_WrongPassword_ThrowsBadCredentials() {
        LoginRequest request = new LoginRequest();
        request.setUsername("student01");
        request.setPassword("wrongpassword");

        Mockito.doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(Mockito.any());

        assertThrows(BadCredentialsException.class,
                () -> authService.login(request));
    }

    @Test
    @DisplayName("AuthService - Register thành công lưu user mới")
    void testRegister_Success() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("password123");
        request.setEmail("new@edu.com");
        request.setFullName("New User");

        Mockito.when(userRepository.existsByUsername("newuser")).thenReturn(false);
        Mockito.when(userRepository.existsByEmail("new@edu.com")).thenReturn(false);
        Mockito.when(passwordEncoder.encode("password123")).thenReturn("encodedPass");

        authService.register(request);

        Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.argThat(u ->
                u.getUsername().equals("newuser") &&
                        u.getRole() == RoleEnum.STUDENT &&
                        u.getIsActive()
        ));
    }

    @Test
    @DisplayName("AuthService - Register username đã tồn tại ném DuplicateResourceException")
    void testRegister_DuplicateUsername_ThrowsException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("student01");
        request.setPassword("password123");
        request.setEmail("other@edu.com");
        request.setFullName("Other");

        Mockito.when(userRepository.existsByUsername("student01")).thenReturn(true);

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class,
                () -> authService.register(request));

        assertTrue(ex.getMessage().contains("Username already exists"));
    }

    @Test
    @DisplayName("AuthService - Đổi mật khẩu sai mật khẩu cũ ném InvalidStateException")
    void testChangePassword_WrongCurrentPassword_ThrowsException() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrongOld");
        request.setNewPassword("newPassword123");

        Mockito.when(userRepository.findByUsername("student01")).thenReturn(Optional.of(mockUser));
        Mockito.when(passwordEncoder.matches("wrongOld", mockUser.getPasswordHash())).thenReturn(false);

        InvalidStateException ex = assertThrows(InvalidStateException.class,
                () -> authService.changePassword("student01", request));

        assertEquals("Current password is incorrect", ex.getMessage());
    }
}