package com.musekit.backend.service;

import com.musekit.backend.dto.CreatePlaylistRequest;
import com.musekit.backend.exception.AppException;
import com.musekit.backend.model.Playlist;
import com.musekit.backend.repository.PlaylistRepository;
import com.musekit.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaylistServiceTest {

    @Mock
    private PlaylistRepository playlistRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PlaylistService playlistService;

    private Playlist samplePlaylist;

    @BeforeEach
    void setUp() {
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
    void testGetAllPlaylists_WithoutUser() {
        when(playlistRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(samplePlaylist));

        List<Playlist> result = playlistService.getAllPlaylists(null);

        assertEquals(1, result.size());
        assertEquals("My Favorites", result.get(0).getName());
    }

    @Test
    void testGetAllPlaylists_WithUser_AutoCreatesFavorites() {
        when(playlistRepository.findByUserId("usr-1")).thenReturn(new ArrayList<>());
        when(playlistRepository.save(any(Playlist.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<Playlist> result = playlistService.getAllPlaylists("usr-1");

        assertFalse(result.isEmpty());
        assertEquals("Favorites", result.get(0).getName());
        assertEquals("usr-1", result.get(0).getUserId());
        verify(playlistRepository).save(any(Playlist.class));
    }

    @Test
    void testGetPlaylistById_Success() {
        when(playlistRepository.findById("pl-123")).thenReturn(Optional.of(samplePlaylist));

        Playlist result = playlistService.getPlaylistById("pl-123");

        assertNotNull(result);
        assertEquals("My Favorites", result.getName());
    }

    @Test
    void testGetPlaylistById_NotFound() {
        when(playlistRepository.findById("non-existent")).thenReturn(Optional.empty());

        assertThrows(AppException.UserNotFoundException.class, () -> playlistService.getPlaylistById("non-existent"));
    }

    @Test
    void testCreatePlaylist() {
        CreatePlaylistRequest request = CreatePlaylistRequest.builder()
                .name("Workout Vibes")
                .userId("usr-1")
                .coverImageUrl("https://images.unsplash.com/custom.jpg")
                .songIds(List.of("bolly-3"))
                .build();

        when(playlistRepository.save(any(Playlist.class))).thenReturn(samplePlaylist);

        Playlist created = playlistService.createPlaylist(request);

        assertNotNull(created);
        verify(playlistRepository).save(any(Playlist.class));
    }

    @Test
    void testAddSongToPlaylist_Success() {
        when(playlistRepository.findById("pl-123")).thenReturn(Optional.of(samplePlaylist));
        when(playlistRepository.save(any(Playlist.class))).thenReturn(samplePlaylist);

        Playlist updated = playlistService.addSongToPlaylist("pl-123", "bolly-3");

        assertNotNull(updated);
        assertTrue(samplePlaylist.getSongIds().contains("bolly-3"));
        verify(playlistRepository).save(samplePlaylist);
    }

    @Test
    void testRemoveSongFromPlaylist_Success() {
        when(playlistRepository.findById("pl-123")).thenReturn(Optional.of(samplePlaylist));
        when(playlistRepository.save(any(Playlist.class))).thenReturn(samplePlaylist);

        Playlist updated = playlistService.removeSongFromPlaylist("pl-123", "bolly-1");

        assertNotNull(updated);
        assertFalse(samplePlaylist.getSongIds().contains("bolly-1"));
        verify(playlistRepository).save(samplePlaylist);
    }

    @Test
    void testDeletePlaylist_Success() {
        when(playlistRepository.existsById("pl-123")).thenReturn(true);

        playlistService.deletePlaylist("pl-123");

        verify(playlistRepository).deleteById("pl-123");
    }
}
