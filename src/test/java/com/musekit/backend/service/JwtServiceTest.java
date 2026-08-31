package com.musekit.backend.service;

import com.musekit.backend.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // 256-bit base64 secret key
        ReflectionTestUtils.setField(jwtService, "secretKey", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 86400000L); // 24 hours

        sampleUser = User.builder()
                .id("usr-12345")
                .name("Aditya Pandey")
                .email("aditya@example.com")
                .build();
    }

    @Test
    void testGenerateToken_And_ExtractClaims() {
        String token = jwtService.generateToken(sampleUser);

        assertNotNull(token);
        assertFalse(token.isEmpty());

        String extractedUserId = jwtService.extractUserId(token);
        String extractedEmail = jwtService.extractEmail(token);

        assertEquals("usr-12345", extractedUserId);
        assertEquals("aditya@example.com", extractedEmail);
        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    void testInvalidToken_ReturnsFalse() {
        assertFalse(jwtService.isTokenValid("invalid.jwt.token"));
    }
}
