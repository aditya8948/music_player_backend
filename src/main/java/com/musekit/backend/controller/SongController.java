package com.musekit.backend.controller;

import com.musekit.backend.dto.PaginatedSongsResponse;
import com.musekit.backend.model.Song;
import com.musekit.backend.service.SongService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/songs")
@RequiredArgsConstructor
@Tag(name = "Songs", description = "Endpoints for song browsing, searching, streaming, uploading, and downloading")
public class SongController {

    private final SongService songService;

    @Operation(
            summary = "Get Paginated Songs & Search",
            description = "Fetches a paginated list of songs. Supports basic keyword search across Title, Artist, Album, Genre, as well as granular multi-field filters."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved paginated songs list",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaginatedSongsResponse.class))
            )
    })
    @GetMapping
    public ResponseEntity<PaginatedSongsResponse> getSongs(
            @Parameter(description = "Page number (1-indexed)", example = "1")
            @RequestParam(value = "page", defaultValue = "1") int page,

            @Parameter(description = "Number of items per page (e.g. 8 for dashboard)", example = "8")
            @RequestParam(value = "limit", defaultValue = "8") int limit,

            @Parameter(description = "Basic search query (matches title, artist, album, genre)", example = "Chrome")
            @RequestParam(value = "query", required = false) String query,

            @Parameter(description = "Filter by genre (e.g. Electronic, Jazz, Pop, Indie)", example = "Electronic")
            @RequestParam(value = "genre", required = false) String genre,

            @Parameter(description = "Filter by artist name", example = "The Asterays")
            @RequestParam(value = "artist", required = false) String artist,

            @Parameter(description = "Filter by album name", example = "Electric Rooms")
            @RequestParam(value = "album", required = false) String album
    ) {
        PaginatedSongsResponse response = songService.getPaginatedSongs(page, limit, query, genre, artist, album);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get Song by ID",
            description = "Retrieves complete metadata and streaming URL for a specific song."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Song found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Song.class))
            ),
            @ApiResponse(responseCode = "404", description = "Song not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Song> getSongById(@PathVariable String id) {
        Song song = songService.getSongById(id);
        return ResponseEntity.ok(song);
    }

    @Operation(
            summary = "Get All Genres",
            description = "Returns list of all distinct music genres present in the catalog."
    )
    @GetMapping("/genres")
    public ResponseEntity<List<String>> getAllGenres() {
        return ResponseEntity.ok(songService.getAllGenres());
    }

    @Operation(
            summary = "Upload Song (Multipart Form)",
            description = "Uploads a new song with an MP3 audio file and cover image."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Song uploaded and created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Song.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid file or metadata")
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Song> uploadSong(
            @RequestParam("title") String title,
            @RequestParam("artist") String artist,
            @RequestParam(value = "album", required = false) String album,
            @RequestParam(value = "genre", required = false) String genre,
            @RequestParam(value = "duration", required = false) Integer duration,
            @RequestParam(value = "releaseYear", required = false) Integer releaseYear,
            @RequestPart(value = "audioFile", required = false) MultipartFile audioFile,
            @RequestPart(value = "coverImage", required = false) MultipartFile coverImage
    ) throws IOException {
        Song createdSong = songService.uploadSong(title, artist, album, genre, duration, releaseYear, audioFile, coverImage);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdSong);
    }

    @Operation(
            summary = "Download Song Audio",
            description = "Downloads the audio file of a song as an attachment."
    )
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadSong(@PathVariable String id) throws IOException {
        Song song = songService.getSongById(id);
        Resource audioResource = songService.getAudioResource(id);

        String filename = song.getTitle().replaceAll("[^a-zA-Z0-9.-]", "_") + ".mp3";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(audioResource);
    }
}
