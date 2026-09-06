package com.musekit.backend.service;

import com.musekit.backend.dto.PaginatedSongsResponse;
import com.musekit.backend.model.Song;
import com.musekit.backend.model.SongIndex;
import com.musekit.backend.repository.SongElasticsearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SongSearchService {

    private final SongElasticsearchRepository songElasticsearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    public void indexSong(Song song) {
        try {
            SongIndex songIndex = SongIndex.fromSong(song);
            songElasticsearchRepository.save(songIndex);
            log.info("Indexed song in Elasticsearch: id={}, title='{}'", song.getId(), song.getTitle());
        } catch (Exception e) {
            log.error("Failed to index song in Elasticsearch: id={}, error={}", song.getId(), e.getMessage());
        }
    }

    public void deleteSong(String songId) {
        try {
            songElasticsearchRepository.deleteById(songId);
            log.info("Deleted song from Elasticsearch: id={}", songId);
        } catch (Exception e) {
            log.error("Failed to delete song from Elasticsearch: id={}, error={}", songId, e.getMessage());
        }
    }

    public void bulkIndex(List<Song> songs) {
        if (songs == null || songs.isEmpty()) return;
        try {
            List<SongIndex> indices = songs.stream()
                    .map(SongIndex::fromSong)
                    .collect(Collectors.toList());
            songElasticsearchRepository.saveAll(indices);
            log.info("Bulk indexed {} songs into Elasticsearch", indices.size());
        } catch (Exception e) {
            log.error("Failed to bulk index songs into Elasticsearch: error={}", e.getMessage());
        }
    }

    public long count() {
        try {
            return songElasticsearchRepository.count();
        } catch (Exception e) {
            log.warn("Could not retrieve Elasticsearch song count: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Search songs in Elasticsearch with fuzzy multi-match across title, artist, album, genre,
     * plus optional exact filters.
     */
    public PaginatedSongsResponse searchSongs(
            int page,
            int limit,
            String query,
            String genre,
            String artist,
            String album
    ) {
        int validPage = Math.max(1, page);
        int validLimit = Math.max(1, limit);

        try {
            var nativeQueryBuilder = NativeQuery.builder()
                    .withPageable(PageRequest.of(validPage - 1, validLimit));

            nativeQueryBuilder.withQuery(q -> q.bool(b -> {
                // High-precision search combining Search-as-you-type (bool_prefix) and Fuzzy typo-tolerance (AUTO)
                if (StringUtils.hasText(query)) {
                    String cleanQuery = query.trim();
                    b.must(m -> m.bool(subBool -> subBool
                            .should(s -> s.multiMatch(mm -> mm
                                    .query(cleanQuery)
                                    .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BoolPrefix)
                                    .fields("title^4", "artist^3", "album^2", "genre")
                            ))
                            .should(s -> s.multiMatch(mm -> mm
                                    .query(cleanQuery)
                                    .fields("title^4", "artist^3", "album^2", "genre")
                                    .fuzziness("AUTO")
                            ))
                            .minimumShouldMatch("1")
                    ));
                }

                // Granular filters
                if (StringUtils.hasText(genre)) {
                    b.filter(f -> f.term(t -> t.field("genre").value(genre.trim())));
                }
                if (StringUtils.hasText(artist)) {
                    b.must(m -> m.match(ma -> ma.field("artist").query(artist.trim())));
                }
                if (StringUtils.hasText(album)) {
                    b.must(m -> m.match(ma -> ma.field("album").query(album.trim())));
                }

                // If no criteria specified, match all
                if (!StringUtils.hasText(query) && !StringUtils.hasText(genre) &&
                        !StringUtils.hasText(artist) && !StringUtils.hasText(album)) {
                    b.must(m -> m.matchAll(ma -> ma));
                }

                return b;
            }));

            SearchHits<SongIndex> searchHits = elasticsearchOperations.search(nativeQueryBuilder.build(), SongIndex.class);

            long totalHits = searchHits.getTotalHits();
            int totalPages = Math.max(1, (int) Math.ceil((double) totalHits / validLimit));

            List<Song> songs = searchHits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .map(SongIndex::toSong)
                    .collect(Collectors.toList());

            return PaginatedSongsResponse.builder()
                    .songs(songs)
                    .currentPage(validPage)
                    .totalPages(totalPages)
                    .totalSongs(totalHits)
                    .limit(validLimit)
                    .hasNext(validPage < totalPages)
                    .hasPrevious(validPage > 1)
                    .build();

        } catch (Exception e) {
            log.error("Elasticsearch query failed: {}", e.getMessage(), e);
            throw e;
        }
    }
}
