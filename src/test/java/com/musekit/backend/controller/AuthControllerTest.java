package com.musekit.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.musekit.backend.dto.AuthResponse;
import com.musekit.backend.dto.LoginRequest;
import com.musekit.backend.dto.SendOtpRequest;
import com.musekit.backend.dto.SignupRequest;
import com.musekit.backend.exception.AppException;
import com.musekit.backend.exception.GlobalExceptionHandler;
import com.musekit.backend.model.User;
import com.musekit.backend.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testSendOtp_Success() throws Exception {
        SendOtpRequest request = SendOtpRequest.builder()
                .email("test@example.com")
                .build();

        when(authService.sendOtp(any(SendOtpRequest.class)))
                .thenReturn(AuthResponse.success("OTP sent successfully to test@example.com"));

        mockMvc.perform(post("/api/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("OTP sent successfully to test@example.com"));
    }

    @Test
    void testSignup_Success() throws Exception {
        SignupRequest request = SignupRequest.builder()
                .name("Aditya")
                .email("test@example.com")
                .otp("123456")
                .build();

        when(authService.signup(any(SignupRequest.class)))
                .thenReturn(AuthResponse.success("Account created successfully! Please login."));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Account created successfully! Please login."));
    }

    @Test
    void testSignup_EmailAlreadyExists_409() throws Exception {
        SignupRequest request = SignupRequest.builder()
                .name("Aditya")
                .email("test@example.com")
                .otp("123456")
                .build();

        when(authService.signup(any(SignupRequest.class)))
                .thenThrow(new AppException.EmailAlreadyExistsException("Email already registered, please login"));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email already registered, please login"));
    }

    @Test
    void testLogin_Success() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .otp("123456")
                .build();

        User user = User.builder().id("u1").name("Aditya").email("test@example.com").build();
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(AuthResponse.success("Logged in successfully", user));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.user.name").value("Aditya"))
                .andExpect(jsonPath("$.user.email").value("test@example.com"));
    }

    @Test
    void testLogin_UserNotFound_404() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("unknown@example.com")
                .otp("123456")
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new AppException.UserNotFoundException("User not found, please create an account"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("User not found, please create an account"));
    }

    @Test
    void testLogin_InvalidOtp_400() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .otp("123456")
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new AppException.InvalidOtpException("Invalid or expired OTP"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid or expired OTP"));
    }
}
