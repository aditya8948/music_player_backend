package com.musekit.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for creating a new playlist")
public class CreatePlaylistRequest {

    @NotBlank(message = "Playlist name is required")
    @Schema(description = "Name of the playlist", example = "Bollywood Hits")
    private String name;

    @Schema(description = "Optional cover image URL for the playlist", example = "https://images.unsplash.com/...")
    private String coverImageUrl;

    @Schema(description = "Optional user ID who owns the playlist", example = "usr-12345")
    private String userId;

    @Schema(description = "Initial list of song IDs to include in the playlist")
    private List<String> songIds;
}
