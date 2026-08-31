package com.musekit.backend.service;

import com.musekit.backend.dto.AuthResponse;
import com.musekit.backend.dto.LoginRequest;
import com.musekit.backend.dto.SendOtpRequest;
import com.musekit.backend.dto.SignupRequest;
import com.musekit.backend.exception.AppException;
import com.musekit.backend.model.OtpVerification;
import com.musekit.backend.model.User;
import com.musekit.backend.repository.OtpVerificationRepository;
import com.musekit.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OtpVerificationRepository otpVerificationRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private JwtService jwtService;

    @Mock
    private PlaylistService playlistService;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id("user-123")
                .name("Aditya")
                .email("test@example.com")
                .build();
    }

    @Test
    void testSendOtp_Success() {
        SendOtpRequest request = SendOtpRequest.builder()
                .email("test@example.com")
                .build();

        AuthResponse response = authService.sendOtp(request);

        assertTrue(response.isSuccess());
        verify(otpVerificationRepository).deleteByEmail("test@example.com");
        verify(otpVerificationRepository).save(any(OtpVerification.class));
        verify(emailService).sendOtpEmail(eq("test@example.com"), any(String.class));
    }

    @Test
    void testSignup_Success() {
        SignupRequest request = SignupRequest.builder()
                .name("Aditya")
                .email("test@example.com")
                .otp("123456")
                .build();

        OtpVerification storedOtp = OtpVerification.builder()
                .email("test@example.com")
                .otp("123456")
                .createdAt(new Date())
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(otpVerificationRepository.findTopByEmailOrderByCreatedAtDesc("test@example.com"))
                .thenReturn(Optional.of(storedOtp));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        AuthResponse response = authService.signup(request);

        assertTrue(response.isSuccess());
        assertEquals("Account created successfully! Please login.", response.getMessage());
        verify(otpVerificationRepository).deleteByEmail("test@example.com");
        verify(userRepository).save(any(User.class));
        verify(playlistService).getAllPlaylists("test@example.com");
    }

    @Test
    void testSignup_EmailAlreadyExists() {
        SignupRequest request = SignupRequest.builder()
                .name("Aditya")
                .email("test@example.com")
                .otp("123456")
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));

        AppException.EmailAlreadyExistsException exception = assertThrows(
                AppException.EmailAlreadyExistsException.class,
                () -> authService.signup(request)
        );

        assertEquals("Email already registered, please login", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testSignup_InvalidOtp() {
        SignupRequest request = SignupRequest.builder()
                .name("Aditya")
                .email("test@example.com")
                .otp("999999")
                .build();

        OtpVerification storedOtp = OtpVerification.builder()
                .email("test@example.com")
                .otp("123456")
                .createdAt(new Date())
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(otpVerificationRepository.findTopByEmailOrderByCreatedAtDesc("test@example.com"))
                .thenReturn(Optional.of(storedOtp));

        assertThrows(AppException.InvalidOtpException.class, () -> authService.signup(request));
    }

    @Test
    void testLogin_Success() {
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .otp("123456")
                .build();

        OtpVerification storedOtp = OtpVerification.builder()
                .email("test@example.com")
                .otp("123456")
                .createdAt(new Date())
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));
        when(otpVerificationRepository.findTopByEmailOrderByCreatedAtDesc("test@example.com"))
                .thenReturn(Optional.of(storedOtp));
        when(jwtService.generateToken(sampleUser)).thenReturn("mock.jwt.token");

        AuthResponse response = authService.login(request);

        assertTrue(response.isSuccess());
        assertNotNull(response.getUser());
        assertEquals("Aditya", response.getUser().getName());
        assertEquals("mock.jwt.token", response.getToken());
        verify(otpVerificationRepository).deleteByEmail("test@example.com");
        verify(playlistService).getAllPlaylists("test@example.com");
    }

    @Test
    void testLogin_UserNotFound() {
        LoginRequest request = LoginRequest.builder()
                .email("notfound@example.com")
                .otp("123456")
                .build();

        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        AppException.UserNotFoundException exception = assertThrows(
                AppException.UserNotFoundException.class,
                () -> authService.login(request)
        );

        assertEquals("User not found, please create an account", exception.getMessage());
    }
}
