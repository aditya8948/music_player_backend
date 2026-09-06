package com.musekit.backend.service;

import com.musekit.backend.dto.CreatePlaylistRequest;
import com.musekit.backend.exception.AppException;
import com.musekit.backend.model.Playlist;
import com.musekit.backend.model.User;
import com.musekit.backend.repository.PlaylistRepository;
import com.musekit.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final UserRepository userRepository;
    private final Random random = new Random();

    private static final List<String> DEFAULT_COVERS = List.of(
            "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?auto=format&fit=crop&w=900&q=80",
            "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=900&q=80",
            "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=900&q=80",
            "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&w=900&q=80",
            "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?auto=format&fit=crop&w=900&q=80"
    );

    /**
     * Resolves user identifier to email (supports MongoDB ObjectId string or user email).
     */
    private String resolveUserEmail(String inputId) {
        if (!StringUtils.hasText(inputId)) {
            return null;
        }
        String trimmed = inputId.trim();
        Optional<User> byId = userRepository.findById(trimmed);
        if (byId.isPresent() && StringUtils.hasText(byId.get().getEmail())) {
            return byId.get().getEmail().trim();
        }
        return trimmed;
    }

    /**
     * Retrieves all playlists for a specific user.
     * Automatically ensures the user has a personal 'Favorites' playlist.
     *
     * @param userId User email or MongoDB user ID (Foreign Key)
     * @return List of playlists owned by this user
     */
    public List<Playlist> getAllPlaylists(String userId) {
        if (!StringUtils.hasText(userId)) {
            return playlistRepository.findAllByOrderByCreatedAtDesc();
        }

        String sanitizedUserId = resolveUserEmail(userId);
        List<Playlist> userPlaylists = playlistRepository.findByUserId(sanitizedUserId);

        // Check if this specific user already has a 'Favorites' playlist
        boolean hasFavorites = userPlaylists.stream()
                .anyMatch(p -> "Favorites".equalsIgnoreCase(p.getName()));

        if (!hasFavorites) {
            String favId = "fav-" + sanitizedUserId.replaceAll("[^a-zA-Z0-9]", "_");
            Playlist personalFavorites = Playlist.builder()
                    .id(favId)
                    .name("Favorites")
                    .userId(sanitizedUserId)
                    .coverImageUrl("https://images.unsplash.com/photo-1518609878373-06d740f60d8b?auto=format&fit=crop&w=900&q=80")
                    .songIds(new ArrayList<>())
                    .createdAt(Instant.now().toString())
                    .build();

            playlistRepository.save(personalFavorites);
            userPlaylists.add(0, personalFavorites);
            log.info("Initialized personal 'Favorites' playlist for user: {}", sanitizedUserId);
        }

        return userPlaylists;
    }

    /**
     * Retrieves a single playlist by ID.
     */
    public Playlist getPlaylistById(String id) {
        Optional<Playlist> found = playlistRepository.findById(id);
        if (found.isPresent()) {
            return found.get();
        }
        if ("playlist-favorites".equalsIgnoreCase(id) || (id != null && id.startsWith("fav-"))) {
            return playlistRepository.findAll().stream()
                    .filter(p -> "Favorites".equalsIgnoreCase(p.getName()))
                    .findFirst()
                    .orElseThrow(() -> new AppException.UserNotFoundException("Playlist not found with id: " + id));
        }
        throw new AppException.UserNotFoundException("Playlist not found with id: " + id);
    }

    /**
     * Creates a new custom playlist owned by the user.
     */
    public Playlist createPlaylist(CreatePlaylistRequest request) {
        String coverImage = request.getCoverImageUrl();
        if (!StringUtils.hasText(coverImage)) {
            coverImage = DEFAULT_COVERS.get(random.nextInt(DEFAULT_COVERS.size()));
        }

        List<String> songIds = request.getSongIds() != null ? new ArrayList<>(request.getSongIds()) : new ArrayList<>();
        String resolvedUser = resolveUserEmail(request.getUserId());

        Playlist playlist = Playlist.builder()
                .name(request.getName().trim())
                .userId(resolvedUser)
                .coverImageUrl(coverImage)
                .songIds(songIds)
                .createdAt(Instant.now().toString())
                .build();

        Playlist saved = playlistRepository.save(playlist);
        log.info("Playlist created: '{}' for user: {} (id: {})", saved.getName(), saved.getUserId(), saved.getId());
        return saved;
    }

    /**
     * Adds a song to an existing playlist (idempotent).
     */
    public Playlist addSongToPlaylist(String playlistId, String songId) {
        Playlist playlist = getPlaylistById(playlistId);
        List<String> songIds = playlist.getSongIds();
        if (songIds == null) {
            songIds = new ArrayList<>();
            playlist.setSongIds(songIds);
        }

        if (!songIds.contains(songId)) {
            songIds.add(songId);
            playlist = playlistRepository.save(playlist);
            log.info("Added song {} to playlist {}", songId, playlistId);
        }

        return playlist;
    }

    /**
     * Removes a song from an existing playlist.
     */
    public Playlist removeSongFromPlaylist(String playlistId, String songId) {
        Playlist playlist = getPlaylistById(playlistId);
        List<String> songIds = playlist.getSongIds();
        if (songIds != null && songIds.remove(songId)) {
            playlist = playlistRepository.save(playlist);
            log.info("Removed song {} from playlist {}", songId, playlistId);
        }
        return playlist;
    }

    /**
     * Deletes a playlist by its ID.
     */
    public void deletePlaylist(String playlistId) {
        if (!playlistRepository.existsById(playlistId)) {
            throw new AppException.UserNotFoundException("Playlist not found with id: " + playlistId);
        }
        playlistRepository.deleteById(playlistId);
        log.info("Deleted playlist with id: {}", playlistId);
    }
}
