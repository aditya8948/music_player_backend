package com.musekit.backend.controller;

import com.musekit.backend.dto.PaginatedSongsResponse;
import com.musekit.backend.exception.GlobalExceptionHandler;
import com.musekit.backend.model.Song;
import com.musekit.backend.service.SongService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SongControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SongService songService;

    @InjectMocks
    private SongController songController;

    private Song sampleSong;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(songController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sampleSong = Song.builder()
                .id("song-1")
                .title("Chrome Hearts")
                .artist("The Asterays")
                .album("Electric Rooms")
                .genre("Electronic")
                .duration(224)
                .releaseYear(2023)
                .audioUrl("https://example.com/audio/chrome-hearts.mp3")
                .coverImageUrl("https://images.unsplash.com/cover.jpg")
                .build();
    }

    @Test
    void testGetSongs_Success() throws Exception {
        PaginatedSongsResponse response = PaginatedSongsResponse.builder()
                .songs(List.of(sampleSong))
                .currentPage(1)
                .totalPages(3)
                .totalSongs(24)
                .limit(8)
                .hasNext(true)
                .hasPrevious(false)
                .build();

        when(songService.getPaginatedSongs(eq(1), eq(8), eq("Chrome"), eq("Electronic"), any(), any()))
                .thenReturn(response);

        mockMvc.perform(get("/api/songs")
                        .param("page", "1")
                        .param("limit", "8")
                        .param("query", "Chrome")
                        .param("genre", "Electronic")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.totalSongs").value(24))
                .andExpect(jsonPath("$.songs[0].title").value("Chrome Hearts"));
    }

    @Test
    void testGetSongById_Success() throws Exception {
        when(songService.getSongById("song-1")).thenReturn(sampleSong);

        mockMvc.perform(get("/api/songs/song-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("song-1"))
                .andExpect(jsonPath("$.title").value("Chrome Hearts"));
    }

    @Test
    void testUploadSong_Success() throws Exception {
        MockMultipartFile audioFile = new MockMultipartFile(
                "audioFile", "song.mp3", "audio/mpeg", "audio content".getBytes()
        );
        MockMultipartFile coverFile = new MockMultipartFile(
                "coverImage", "cover.jpg", "image/jpeg", "image content".getBytes()
        );

        when(songService.uploadSong(eq("Chrome Hearts"), eq("The Asterays"), eq("Electric Rooms"), eq("Electronic"), eq(224), eq(2023), any(), any()))
                .thenReturn(sampleSong);

        mockMvc.perform(multipart("/api/songs/upload")
                        .file(audioFile)
                        .file(coverFile)
                        .param("title", "Chrome Hearts")
                        .param("artist", "The Asterays")
                        .param("album", "Electric Rooms")
                        .param("genre", "Electronic")
                        .param("duration", "224")
                        .param("releaseYear", "2023"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("song-1"))
                .andExpect(jsonPath("$.title").value("Chrome Hearts"));
    }
}
