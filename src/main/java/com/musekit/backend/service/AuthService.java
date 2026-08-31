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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final PlaylistService playlistService;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Common endpoint: Generates and dispatches a 6-digit OTP to the given email address.
     *
     * @param request SendOtpRequest containing recipient email
     * @return AuthResponse confirmation
     */
    public AuthResponse sendOtp(SendOtpRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        // Generate secure 6-digit OTP
        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));

        // Delete any existing OTPs for this email and save new one
        otpVerificationRepository.deleteByEmail(email);

        OtpVerification verification = OtpVerification.builder()
                .email(email)
                .otp(otp)
                .createdAt(new Date())
                .build();
        otpVerificationRepository.save(verification);

        // Send OTP email via Gmail SMTP
        emailService.sendOtpEmail(email, otp);

        log.info("Dispatched OTP to: {}", email);
        return AuthResponse.success("OTP sent successfully to " + email);
    }

    /**
     * Signup endpoint: Verifies OTP, checks email uniqueness, registers user,
     * and automatically initializes their default 'Favorites' playlist.
     *
     * @param request SignupRequest containing name, email, and otp
     * @return AuthResponse with success message
     */
    public AuthResponse signup(SignupRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        String name = request.getName().trim();
        String otp = request.getOtp().trim();

        // 1. Check if user already exists
        if (userRepository.findByEmail(email).isPresent()) {
            throw new AppException.EmailAlreadyExistsException("Email already registered, please login");
        }

        // 2. Validate and consume OTP
        validateAndConsumeOtp(email, otp);

        // 3. Create and save new User in MongoDB
        User newUser = User.builder()
                .name(name)
                .email(email)
                .build();
        User savedUser = userRepository.save(newUser);
        log.info("User successfully registered: {}", email);

        // 4. Auto-initialize the default 'Favorites' playlist for this new user
        try {
            playlistService.getAllPlaylists(email);
            log.info("Auto-initialized default 'Favorites' playlist for: {}", email);
        } catch (Exception e) {
            log.warn("Could not auto-initialize playlist for {}: {}", email, e.getMessage());
        }

        return AuthResponse.success("Account created successfully! Please login.", savedUser);
    }

    /**
     * Login endpoint: Verifies user exists, validates OTP, generates signed JWT token, and returns user details.
     *
     * @param request LoginRequest containing email and otp
     * @return AuthResponse with user profile details and JWT token
     */
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        String otp = request.getOtp().trim();

        // 1. Verify user exists
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException.UserNotFoundException("User not found, please create an account"));

        // 2. Validate and consume OTP
        validateAndConsumeOtp(email, otp);

        // 3. Generate signed 24-hour JWT token
        String token = jwtService.generateToken(user);

        // 4. Ensure user's default 'Favorites' playlist is ready
        try {
            playlistService.getAllPlaylists(email);
        } catch (Exception e) {
            log.warn("Could not verify playlist for {}: {}", email, e.getMessage());
        }

        log.info("User successfully logged in with JWT token: {}", email);
        return AuthResponse.success("Logged in successfully", user, token);
    }

    /**
     * Helper method to validate OTP existence, 5-minute expiry, matching, and consume it.
     */
    private void validateAndConsumeOtp(String email, String otp) {
        OtpVerification verification = otpVerificationRepository.findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new AppException.InvalidOtpException("Invalid or expired OTP"));

        // Check 5-minute validity window
        long ageInMillis = System.currentTimeMillis() - verification.getCreatedAt().getTime();
        if (ageInMillis > 5 * 60 * 1000) {
            otpVerificationRepository.deleteByEmail(email);
            throw new AppException.InvalidOtpException("OTP has expired. Please request a new one.");
        }

        // Verify OTP matches
        if (!verification.getOtp().equals(otp)) {
            throw new AppException.InvalidOtpException("Invalid OTP");
        }

        // Delete consumed OTP to prevent reuse
        otpVerificationRepository.deleteByEmail(email);
    }
}
