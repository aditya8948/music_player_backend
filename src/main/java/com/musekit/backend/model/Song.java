package com.musekit.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "songs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Song {
    @Id
    private String id;
    private String title;
    private String artist;
    private String album;
    private String genre;
    private Integer duration;
    private Integer releaseYear;
    private String coverImageUrl;
    private String audioUrl;
}
