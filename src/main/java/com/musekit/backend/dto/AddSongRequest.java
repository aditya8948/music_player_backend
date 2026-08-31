package com.musekit.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for adding a song to a playlist")
public class AddSongRequest {

    @NotBlank(message = "Song ID is required")
    @Schema(description = "ID of the song to add", example = "bolly-diltodke")
    private String songId;
}
