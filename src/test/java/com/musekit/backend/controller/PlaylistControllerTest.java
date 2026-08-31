package com.musekit.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.musekit.backend.dto.AddSongRequest;
import com.musekit.backend.dto.CreatePlaylistRequest;
import com.musekit.backend.exception.GlobalExceptionHandler;
import com.musekit.backend.model.Playlist;
import com.musekit.backend.service.PlaylistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PlaylistControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PlaylistService playlistService;

    @InjectMocks
    private PlaylistController playlistController;

    private Playlist samplePlaylist;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(playlistController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        samplePlaylist = Playlist.builder()
                .id("pl-123")
                .name("My Favorites")
                .userId("usr-1")
                .coverImageUrl("https://images.unsplash.com/cover.jpg")
                .songIds(new ArrayList<>(List.of("bolly-1", "bolly-2")))
                .createdAt("2026-08-30T12:00:00Z")
                .build();
    }

    @Test
    void testGetAllPlaylists_Success() throws Exception {
        when(playlistService.getAllPlaylists(null)).thenReturn(List.of(samplePlaylist));

        mockMvc.perform(get("/api/playlists")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("pl-123"))
                .andExpect(jsonPath("$[0].name").value("My Favorites"));
    }

    @Test
    void testGetPlaylistById_Success() throws Exception {
        when(playlistService.getPlaylistById("pl-123")).thenReturn(samplePlaylist);

        mockMvc.perform(get("/api/playlists/pl-123")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("pl-123"))
                .andExpect(jsonPath("$.name").value("My Favorites"));
    }

    @Test
    void testCreatePlaylist_Success() throws Exception {
        CreatePlaylistRequest request = CreatePlaylistRequest.builder()
                .name("Workout Vibes")
                .userId("usr-1")
                .build();

        when(playlistService.createPlaylist(any(CreatePlaylistRequest.class))).thenReturn(samplePlaylist);

        mockMvc.perform(post("/api/playlists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("pl-123"));
    }

    @Test
    void testAddSongToPlaylist_Success() throws Exception {
        AddSongRequest request = AddSongRequest.builder()
                .songId("bolly-3")
                .build();

        when(playlistService.addSongToPlaylist(eq("pl-123"), eq("bolly-3"))).thenReturn(samplePlaylist);

        mockMvc.perform(post("/api/playlists/pl-123/songs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("pl-123"));
    }

    @Test
    void testRemoveSongFromPlaylist_Success() throws Exception {
        when(playlistService.removeSongFromPlaylist(eq("pl-123"), eq("bolly-1"))).thenReturn(samplePlaylist);

        mockMvc.perform(delete("/api/playlists/pl-123/songs/bolly-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("pl-123"));
    }

    @Test
    void testDeletePlaylist_Success() throws Exception {
        mockMvc.perform(delete("/api/playlists/pl-123")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
