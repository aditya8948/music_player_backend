package com.musekit.backend.service;

import com.musekit.backend.dto.PaginatedSongsResponse;
import com.musekit.backend.model.Song;
import com.musekit.backend.repository.SongRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SongServiceTest {

    @Mock
    private SongRepository songRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private SongSearchService songSearchService;

    @InjectMocks
    private SongService songService;

    private Song sampleSong;

    @BeforeEach
    void setUp() {
        sampleSong = Song.builder()
                .id("song-1")
                .title("Midnight Horizon")
                .artist("Arijit & The Asterays")
                .album("Neon Skies")
                .genre("Bollywood")
                .duration(240)
                .releaseYear(2024)
                .audioUrl("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3")
                .coverImageUrl("https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=900&q=80")
                .build();
    }

    @Test
    void testGetPaginatedSongs_BrowseWithoutQuery() {
        when(mongoTemplate.count(any(Query.class), eq(Song.class))).thenReturn(8L);
        when(mongoTemplate.find(any(Query.class), eq(Song.class))).thenReturn(List.of(sampleSong));

        PaginatedSongsResponse response = songService.getPaginatedSongs(1, 8, null, "Bollywood", null, null);

        assertNotNull(response);
        assertEquals(1, response.getCurrentPage());
        assertEquals(8, response.getLimit());
        assertEquals(8L, response.getTotalSongs());
        assertEquals(1, response.getTotalPages());
        assertFalse(response.isHasNext());
        assertFalse(response.isHasPrevious());
        assertEquals(1, response.getSongs().size());
        assertEquals("Midnight Horizon", response.getSongs().get(0).getTitle());
    }

    @Test
    void testGetPaginatedSongs_SearchWithElasticsearch() {
        PaginatedSongsResponse esResponse = PaginatedSongsResponse.builder()
                .songs(List.of(sampleSong))
                .currentPage(1)
                .totalPages(1)
                .totalSongs(1)
                .limit(8)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        when(songSearchService.searchSongs(1, 8, "Midnight", "Bollywood", null, null))
                .thenReturn(esResponse);

        PaginatedSongsResponse response = songService.getPaginatedSongs(1, 8, "Midnight", "Bollywood", null, null);

        assertNotNull(response);
        assertEquals(1, response.getSongs().size());
        assertEquals("Midnight Horizon", response.getSongs().get(0).getTitle());
        verify(songSearchService).searchSongs(1, 8, "Midnight", "Bollywood", null, null);
    }

    @Test
    void testGetSongById_Success() {
        when(songRepository.findById("song-1")).thenReturn(Optional.of(sampleSong));

        Song found = songService.getSongById("song-1");

        assertNotNull(found);
        assertEquals("Midnight Horizon", found.getTitle());
    }

    @Test
    void testUploadSong_Success() throws IOException {
        MockMultipartFile audioFile = new MockMultipartFile(
                "audioFile", "test-song.mp3", "audio/mpeg", "mock audio data".getBytes()
        );
        MockMultipartFile coverFile = new MockMultipartFile(
                "coverImage", "test-cover.jpg", "image/jpeg", "mock image data".getBytes()
        );

        when(songRepository.save(any(Song.class))).thenReturn(sampleSong);

        Song uploaded = songService.uploadSong(
                "Midnight Horizon", "Arijit", "Neon Skies", "Bollywood", 240, 2024, audioFile, coverFile
        );

        assertNotNull(uploaded);
        verify(songRepository).save(any(Song.class));
    }
}
