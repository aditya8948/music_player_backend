package com.musekit.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.musekit.backend.model.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard authentication response")
public class AuthResponse {

    @Schema(description = "Indicates whether the operation was successful", example = "true")
    private boolean success;

    @Schema(description = "Descriptive response message", example = "Logged in successfully")
    private String message;

    @Schema(description = "User details (returned upon successful login)")
    private User user;

    @Schema(description = "Signed JWT token for authenticating subsequent requests", example = "eyJhbGciOiJIUzI1NiIsIn...")
    private String token;

    public static AuthResponse success(String message) {
        return AuthResponse.builder()
                .success(true)
                .message(message)
                .build();
    }

    public static AuthResponse success(String message, User user) {
        return AuthResponse.builder()
                .success(true)
                .message(message)
                .user(user)
                .build();
    }

    public static AuthResponse success(String message, User user, String token) {
        return AuthResponse.builder()
                .success(true)
                .message(message)
                .user(user)
                .token(token)
                .build();
    }

    public static AuthResponse error(String message) {
        return AuthResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}
