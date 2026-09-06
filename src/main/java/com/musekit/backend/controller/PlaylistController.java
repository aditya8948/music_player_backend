package com.musekit.backend.controller;

import com.musekit.backend.dto.AddSongRequest;
import com.musekit.backend.dto.CreatePlaylistRequest;
import com.musekit.backend.model.Playlist;
import com.musekit.backend.service.PlaylistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Playlist management controller.
 *
 * All playlist operations are scoped to the authenticated user.
 * The user's identity is extracted from:
 * 1. ZITADEL JWT token (direct or via Gateway)
 * 2. X-User-Email header injected by the API Gateway
 * 3. Client-supplied userId parameter as fallback
 */
@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
@Tag(name = "Playlists", description = "Endpoints for creating and managing custom playlists and adding/removing tracks")
@Slf4j
public class PlaylistController {

    private final PlaylistService playlistService;

    @Operation(
            summary = "Get All Playlists",
            description = "Retrieves all playlists for the authenticated user."
    )
    @GetMapping
    public ResponseEntity<List<Playlist>> getAllPlaylists(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-User-Email", required = false) String headerEmail,
            @RequestParam(value = "userId", required = false) String paramUserId
    ) {
        String userEmail = resolveUserEmail(jwt, headerEmail, paramUserId);
        List<Playlist> playlists = playlistService.getAllPlaylists(userEmail);
        return ResponseEntity.ok(playlists);
    }

    @Operation(
            summary = "Get Playlist by ID",
            description = "Retrieves a specific playlist including its metadata and song ID list."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Playlist found"),
            @ApiResponse(responseCode = "404", description = "Playlist not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Playlist> getPlaylistById(@PathVariable String id) {
        Playlist playlist = playlistService.getPlaylistById(id);
        return ResponseEntity.ok(playlist);
    }

    @Operation(
            summary = "Create Playlist",
            description = "Creates a new playlist for the authenticated user. The userId is automatically set from the JWT token or Gateway header."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Playlist created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload")
    })
    @PostMapping
    public ResponseEntity<Playlist> createPlaylist(
            @Valid @RequestBody CreatePlaylistRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "X-User-Email", required = false) String headerEmail
    ) {
        String userEmail = resolveUserEmail(jwt, headerEmail, request.getUserId());
        request.setUserId(userEmail);

        Playlist created = playlistService.createPlaylist(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(
            summary = "Add Song to Playlist",
            description = "Appends a song ID to an existing playlist if not already present."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Song added to playlist"),
            @ApiResponse(responseCode = "404", description = "Playlist not found")
    })
    @PostMapping("/{id}/songs")
    public ResponseEntity<Playlist> addSongToPlaylist(
            @PathVariable String id,
            @Valid @RequestBody AddSongRequest request
    ) {
        Playlist updated = playlistService.addSongToPlaylist(id, request.getSongId());
        return ResponseEntity.ok(updated);
    }

    @Operation(
            summary = "Remove Song from Playlist",
            description = "Removes a song ID from a playlist."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Song removed from playlist"),
            @ApiResponse(responseCode = "404", description = "Playlist not found")
    })
    @DeleteMapping("/{id}/songs/{songId}")
    public ResponseEntity<Playlist> removeSongFromPlaylist(
            @PathVariable String id,
            @PathVariable String songId
    ) {
        Playlist updated = playlistService.removeSongFromPlaylist(id, songId);
        return ResponseEntity.ok(updated);
    }

    @Operation(
            summary = "Delete Playlist",
            description = "Permanently deletes a playlist by its ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Playlist deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Playlist not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deletePlaylist(@PathVariable String id) {
        playlistService.deletePlaylist(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Playlist deleted successfully"));
    }

    /**
     * Resolves the user's email across JWT claims, Gateway headers, and fallback params.
     */
    private String resolveUserEmail(Jwt jwt, String headerEmail, String fallbackUserId) {
        String email = null;
        if (jwt != null) {
            email = extractEmail(jwt);
            String sub = jwt.getSubject();
            if (email != null && sub != null && email.equals(sub)) {
                // If it fell back to subject ID, clear it so header or param is prioritized
                email = null;
            }
        }

        if ((email == null || !email.contains("@")) && StringUtils.hasText(headerEmail) && headerEmail.contains("@")) {
            email = headerEmail.toLowerCase().trim();
        }

        if ((email == null || !email.contains("@")) && StringUtils.hasText(fallbackUserId) && fallbackUserId.contains("@")) {
            email = fallbackUserId.toLowerCase().trim();
        }

        if (email == null && jwt != null) {
            email = jwt.getSubject();
        }

        log.debug("Resolved playlist user email: {} (header: {}, fallback: {})", email, headerEmail, fallbackUserId);
        return email;
    }

    /**
     * Extracts the user's email from ZITADEL JWT claims.
     * Falls back to preferred_username or subject if email claim is missing.
     */
    private String extractEmail(Jwt jwt) {
        if (jwt == null) return null;

        String email = jwt.getClaimAsString("email");
        if (email != null) return email.toLowerCase().trim();

        String preferred = jwt.getClaimAsString("preferred_username");
        if (preferred != null && preferred.contains("@")) return preferred.toLowerCase().trim();

        return jwt.getSubject();
    }
}
