package com.musekit.backend.service;

import com.musekit.backend.dto.PaginatedSongsResponse;
import com.musekit.backend.exception.AppException;
import com.musekit.backend.model.Song;
import com.musekit.backend.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SongService {

    private final SongRepository songRepository;
    private final MongoTemplate mongoTemplate;

    private static final String SONGS_SUBDIR = "uploads/songs";
    private static final String COVERS_SUBDIR = "uploads/covers";
    public static final String DEFAULT_COVER_IMAGE = "/default_cover.jpg";

    /**
     * Unified Song Query:
     * - Returns Local MongoDB songs with pagination, search, and genre/artist/album filters.
     */
    public PaginatedSongsResponse getPaginatedSongs(
            int page,
            int limit,
            String query,
            String genre,
            String artist,
            String album
    ) {
        int validPage = Math.max(1, page);
        int validLimit = Math.max(1, limit);

        Query mongoQuery = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        if (StringUtils.hasText(query)) {
            String regex = "(?i)" + query.trim();
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("title").regex(regex),
                    Criteria.where("artist").regex(regex),
                    Criteria.where("album").regex(regex),
                    Criteria.where("genre").regex(regex)
            ));
        }

        if (StringUtils.hasText(genre)) {
            criteriaList.add(Criteria.where("genre").regex("(?i)^" + genre.trim() + "$"));
        }
        if (StringUtils.hasText(artist)) {
            criteriaList.add(Criteria.where("artist").regex("(?i)" + artist.trim()));
        }
        if (StringUtils.hasText(album)) {
            criteriaList.add(Criteria.where("album").regex("(?i)" + album.trim()));
        }

        if (!criteriaList.isEmpty()) {
            mongoQuery.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        long totalSongs = mongoTemplate.count(mongoQuery, Song.class);

        mongoQuery.skip((long) (validPage - 1) * validLimit)
                .limit(validLimit)
                .with(Sort.by(Sort.Direction.ASC, "title"));

        List<Song> songs = mongoTemplate.find(mongoQuery, Song.class);
        int totalPages = Math.max(1, (int) Math.ceil((double) totalSongs / validLimit));

        return PaginatedSongsResponse.builder()
                .songs(songs)
                .currentPage(validPage)
                .totalPages(totalPages)
                .totalSongs(totalSongs)
                .limit(validLimit)
                .hasNext(validPage < totalPages)
                .hasPrevious(validPage > 1)
                .build();
    }

    /**
     * Retrieves a song by its unique identifier.
     */
    public Song getSongById(String id) {
        return songRepository.findById(id)
                .orElseThrow(() -> new AppException.UserNotFoundException("Song not found with id: " + id));
    }

    /**
     * Uploads a new custom song with audio file and cover image to local server and saves to MongoDB.
     */
    public Song uploadSong(
            String title,
            String artist,
            String album,
            String genre,
            Integer duration,
            Integer releaseYear,
            MultipartFile audioFile,
            MultipartFile coverImage
    ) throws IOException {
        Files.createDirectories(Paths.get(SONGS_SUBDIR));
        Files.createDirectories(Paths.get(COVERS_SUBDIR));

        String audioUrl = "/uploads/songs/bolly-diltodke.mp3";
        if (audioFile != null && !audioFile.isEmpty()) {
            String originalFilename = StringUtils.cleanPath(audioFile.getOriginalFilename() != null ? audioFile.getOriginalFilename() : "audio.mp3");
            String uniqueAudioName = UUID.randomUUID() + "_" + originalFilename;
            Path audioTarget = Paths.get(SONGS_SUBDIR).resolve(uniqueAudioName);
            Files.copy(audioFile.getInputStream(), audioTarget, StandardCopyOption.REPLACE_EXISTING);
            audioUrl = "/uploads/songs/" + uniqueAudioName;
        }

        String coverImageUrl = DEFAULT_COVER_IMAGE;
        if (coverImage != null && !coverImage.isEmpty()) {
            String originalImageName = StringUtils.cleanPath(coverImage.getOriginalFilename() != null ? coverImage.getOriginalFilename() : "cover.jpg");
            String uniqueCoverName = UUID.randomUUID() + "_" + originalImageName;
            Path coverTarget = Paths.get(COVERS_SUBDIR).resolve(uniqueCoverName);
            Files.copy(coverImage.getInputStream(), coverTarget, StandardCopyOption.REPLACE_EXISTING);
            coverImageUrl = "/uploads/covers/" + uniqueCoverName;
        }

        Song newSong = Song.builder()
                .title(title.trim())
                .artist(artist.trim())
                .album(StringUtils.hasText(album) ? album.trim() : "Single")
                .genre(StringUtils.hasText(genre) ? genre.trim() : "Bollywood")
                .duration(duration != null && duration > 0 ? duration : 200)
                .releaseYear(releaseYear != null ? releaseYear : java.time.Year.now().getValue())
                .audioUrl(audioUrl)
                .coverImageUrl(coverImageUrl)
                .build();

        Song savedSong = songRepository.save(newSong);
        log.info("Song uploaded successfully: {} by {}", savedSong.getTitle(), savedSong.getArtist());
        return savedSong;
    }

    /**
     * Prepares the audio file Resource for downloading.
     */
    public Resource getAudioResource(String id) throws MalformedURLException {
        Song song = getSongById(id);
        String audioUrl = song.getAudioUrl();

        if (audioUrl != null && audioUrl.startsWith("/uploads/")) {
            Path filePath = Paths.get(audioUrl.substring(1));
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
        }
        throw new AppException.InvalidOtpException("Audio file is not locally stored for download");
    }

    /**
     * Returns list of distinct genres present in the database.
     */
    public List<String> getAllGenres() {
        return mongoTemplate.query(Song.class).distinct("genre").as(String.class).all();
    }
}
