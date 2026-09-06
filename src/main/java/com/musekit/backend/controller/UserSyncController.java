package com.musekit.backend.controller;

import com.musekit.backend.model.User;
import com.musekit.backend.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * User synchronization controller.
 *
 * After a user logs in via ZITADEL for the first time, the frontend calls this
 * endpoint to ensure the user exists in MongoDB. This replaces the old signup flow.
 *
 * The user's identity (email, name) is extracted from the validated ZITADEL JWT token
 * — no request body needed. The token has already been validated by Spring Security's
 * OAuth2 Resource Server before this method is called.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile synchronization with ZITADEL identity provider")
@Slf4j
public class UserSyncController {

    private final UserRepository userRepository;

    @Operation(
            summary = "Sync User Profile",
            description = "Synchronizes the authenticated user's ZITADEL profile to MongoDB. " +
                    "Creates a new user record if one doesn't exist, or returns the existing profile. " +
                    "Called by the frontend after each successful ZITADEL login."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User profile synced successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = User.class))
            ),
            @ApiResponse(responseCode = "401", description = "Missing or invalid ZITADEL JWT token")
    })
    @PostMapping("/sync")
    public ResponseEntity<User> syncUser(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-User-Email", required = false) String headerEmail,
            @RequestHeader(value = "X-User-Name", required = false) String headerName,
            @RequestBody(required = false) Map<String, String> body) {
        // Extract user info from ZITADEL JWT claims
        String email = extractEmail(jwt);
        String name = extractName(jwt);
        String zitadelUserId = jwt.getSubject(); // ZITADEL's unique user ID

        // Check headers from API Gateway if JWT claims lacked email
        if ((email == null || email.equals(zitadelUserId)) && headerEmail != null && headerEmail.contains("@")) {
            email = headerEmail.toLowerCase().trim();
        }
        if ((name == null || name.equals(zitadelUserId)) && headerName != null && !headerName.isBlank()) {
            name = headerName.trim();
        }

        // If still missing or defaulted to subject ID, fallback to body provided by frontend
        if (body != null) {
            if ((email == null || email.equals(zitadelUserId)) && body.get("email") != null && !body.get("email").isBlank()) {
                email = body.get("email").toLowerCase().trim();
            }
            if ((name == null || name.equals(zitadelUserId)) && body.get("name") != null && !body.get("name").isBlank()) {
                name = body.get("name").trim();
            }
        }

        log.info("User sync requested — email: {}, name: {}, zitadelId: {}", email, name, zitadelUserId);

        final String finalEmail = email;
        final String finalName = name;

        // Find existing user by email, or create a new one
        User user = userRepository.findByEmail(finalEmail)
                .map(existingUser -> {
                    // Update name if it changed in ZITADEL
                    if (finalName != null && !finalName.equals(existingUser.getName())) {
                        existingUser.setName(finalName);
                        userRepository.save(existingUser);
                        log.info("Updated user name for: {}", finalEmail);
                    }
                    return existingUser;
                })
                .orElseGet(() -> {
                    // First-time login — create user in MongoDB
                    User newUser = User.builder()
                            .email(finalEmail)
                            .name(finalName != null ? finalName : finalEmail.split("@")[0])
                            .build();
                    User saved = userRepository.save(newUser);
                    log.info("Created new user from ZITADEL profile: {} (mongoId: {})", finalEmail, saved.getId());
                    return saved;
                });

        return ResponseEntity.ok(user);
    }

    @Operation(
            summary = "Get Current User Profile",
            description = "Returns the authenticated user's profile from MongoDB."
    )
    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        String email = extractEmail(jwt);

        return userRepository.findByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Extracts the user's email from ZITADEL JWT claims.
     * ZITADEL stores email in the "email" claim (OIDC standard)
     * or as part of "urn:zitadel:iam:user:metadata" claims.
     */
    private String extractEmail(Jwt jwt) {
        // Standard OIDC email claim
        String email = jwt.getClaimAsString("email");
        if (email != null) {
            return email.toLowerCase().trim();
        }

        // Fallback: preferred_username (often the email in ZITADEL)
        String preferred = jwt.getClaimAsString("preferred_username");
        if (preferred != null && preferred.contains("@")) {
            return preferred.toLowerCase().trim();
        }

        // Last resort: subject (ZITADEL user ID)
        return jwt.getSubject();
    }

    /**
     * Extracts the user's display name from ZITADEL JWT claims.
     */
    private String extractName(Jwt jwt) {
        // Standard OIDC name claim
        String name = jwt.getClaimAsString("name");
        if (name != null && !name.isBlank()) {
            return name.trim();
        }

        // Fallback: given_name + family_name
        String given = jwt.getClaimAsString("given_name");
        String family = jwt.getClaimAsString("family_name");
        if (given != null || family != null) {
            return ((given != null ? given : "") + " " + (family != null ? family : "")).trim();
        }

        return null;
    }
}
