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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Playlists", description = "Endpoints for creating and managing custom playlists and adding/removing tracks")
public class PlaylistController {

    private final PlaylistService playlistService;

    @Operation(
            summary = "Get All Playlists",
            description = "Retrieves all custom playlists. Optionally filter by user ID."
    )
    @GetMapping
    public ResponseEntity<List<Playlist>> getAllPlaylists(
            @Parameter(description = "Optional user ID to filter playlists", example = "usr-123")
            @RequestParam(value = "userId", required = false) String userId
    ) {
        List<Playlist> playlists = playlistService.getAllPlaylists(userId);
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
            description = "Creates a new playlist with name, optional cover image, and initial song list."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Playlist created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload")
    })
    @PostMapping
    public ResponseEntity<Playlist> createPlaylist(@Valid @RequestBody CreatePlaylistRequest request) {
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
}
