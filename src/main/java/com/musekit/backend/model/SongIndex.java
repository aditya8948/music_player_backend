package com.musekit.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "songs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SongIndex {

    @Id
    private String id;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Text)
    private String artist;

    @Field(type = FieldType.Text)
    private String album;

    @Field(type = FieldType.Keyword)
    private String genre;

    @Field(type = FieldType.Integer)
    private Integer duration;

    @Field(type = FieldType.Integer)
    private Integer releaseYear;

    @Field(type = FieldType.Keyword, index = false)
    private String coverImageUrl;

    @Field(type = FieldType.Keyword, index = false)
    private String audioUrl;

    public static SongIndex fromSong(Song song) {
        if (song == null) return null;
        return SongIndex.builder()
                .id(song.getId())
                .title(song.getTitle())
                .artist(song.getArtist())
                .album(song.getAlbum())
                .genre(song.getGenre())
                .duration(song.getDuration())
                .releaseYear(song.getReleaseYear())
                .coverImageUrl(song.getCoverImageUrl())
                .audioUrl(song.getAudioUrl())
                .build();
    }

    public Song toSong() {
        return Song.builder()
                .id(this.id)
                .title(this.title)
                .artist(this.artist)
                .album(this.album)
                .genre(this.genre)
                .duration(this.duration)
                .releaseYear(this.releaseYear)
                .coverImageUrl(this.coverImageUrl)
                .audioUrl(this.audioUrl)
                .build();
    }
}
