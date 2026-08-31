package com.musekit.backend.config;

import com.musekit.backend.model.Playlist;
import com.musekit.backend.model.User;
import com.musekit.backend.repository.PlaylistRepository;
import com.musekit.backend.repository.SongRepository;
import com.musekit.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final SongRepository songRepository;
    private final PlaylistRepository playlistRepository;
    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        // 1. Delete legacy dummy / soundhelix tones from SongRepository
        songRepository.findAll().stream()
                .filter(s -> s.getAudioUrl() != null && (s.getAudioUrl().contains("soundhelix") || s.getAudioUrl().contains("sample.mp3") || s.getId().matches("bolly-[0-9]+")))
                .forEach(s -> {
                    songRepository.deleteById(s.getId());
                    log.info("Deleted dummy tone song: {} ({})", s.getTitle(), s.getId());
                });

        // 2. Delete legacy unassigned global playlists (where userId is null or dummy IDs)
        playlistRepository.findAll().stream()
                .filter(p -> p.getUserId() == null || p.getId().equals("playlist-favorites") || p.getId().startsWith("playlist-"))
                .forEach(p -> {
                    playlistRepository.deleteById(p.getId());
                    log.info("Cleaned legacy unassigned playlist: {}", p.getId());
                });

        // 3. Ensure every registered user has their own dedicated 'Favorites' playlist document
        List<User> users = userRepository.findAll();
        for (User user : users) {
            String userKey = user.getEmail() != null ? user.getEmail().toLowerCase().trim() : user.getId();
            boolean hasFav = playlistRepository.findByUserId(userKey).stream()
                    .anyMatch(p -> "Favorites".equalsIgnoreCase(p.getName()));

            if (!hasFav) {
                String favId = "fav-" + userKey.replaceAll("[^a-zA-Z0-9]", "_");
                Playlist userFavorites = Playlist.builder()
                        .id(favId)
                        .name("Favorites")
                        .userId(userKey)
                        .coverImageUrl("https://images.unsplash.com/photo-1518609878373-06d740f60d8b?auto=format&fit=crop&w=900&q=80")
                        .songIds(new ArrayList<>())
                        .createdAt(Instant.now().toString())
                        .build();

                playlistRepository.save(userFavorites);
                log.info("Created dedicated Favorites playlist for user: {} (id: {})", userKey, favId);
            }
        }

        log.info("DataSeeder completed. Total real songs: {}, Total users: {}, Total user-bound playlists: {}",
                songRepository.count(), userRepository.count(), playlistRepository.count());
    }
}
