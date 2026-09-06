package com.musekit.backend.config;

import com.musekit.backend.model.Song;
import com.musekit.backend.repository.SongRepository;
import com.musekit.backend.service.SongSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchDataSyncRunner implements ApplicationRunner {

    private final SongRepository songRepository;
    private final SongSearchService songSearchService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Checking Elasticsearch data synchronization status...");
        try {
            long esCount = songSearchService.count();
            long mongoCount = songRepository.count();

            log.info("MongoDB song count: {}, Elasticsearch indexed song count: {}", mongoCount, esCount);

            if (mongoCount > 0 && esCount < mongoCount) {
                log.info("Syncing {} songs from MongoDB to Elasticsearch...", mongoCount);
                List<Song> allSongs = songRepository.findAll();
                songSearchService.bulkIndex(allSongs);
                log.info("Elasticsearch data synchronization complete! Indexed {} songs.", allSongs.size());
            } else {
                log.info("Elasticsearch is already in sync with MongoDB (or MongoDB is empty).");
            }
        } catch (Exception e) {
            log.warn("Could not synchronize MongoDB to Elasticsearch on startup (Elasticsearch may be offline): {}", e.getMessage());
        }
    }
}
