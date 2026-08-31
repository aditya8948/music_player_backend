package com.musekit.backend.dto;

import com.musekit.backend.model.Song;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Paginated response containing song items and pagination metadata")
public class PaginatedSongsResponse {

    @Schema(description = "List of songs in the current page")
    private List<Song> songs;

    @Schema(description = "Current page number (1-indexed)", example = "1")
    private int currentPage;

    @Schema(description = "Total number of pages available", example = "3")
    private int totalPages;

    @Schema(description = "Total count of songs matching the filter", example = "24")
    private long totalSongs;

    @Schema(description = "Number of items per page", example = "8")
    private int limit;

    @Schema(description = "Indicates whether there is a next page", example = "true")
    private boolean hasNext;

    @Schema(description = "Indicates whether there is a previous page", example = "false")
    private boolean hasPrevious;
}
