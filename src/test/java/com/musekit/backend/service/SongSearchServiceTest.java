package com.musekit.backend.service;

import com.musekit.backend.dto.PaginatedSongsResponse;
import com.musekit.backend.model.Song;
import com.musekit.backend.model.SongIndex;
import com.musekit.backend.repository.SongElasticsearchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SongSearchServiceTest {

    @Mock
    private SongElasticsearchRepository songElasticsearchRepository;

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @InjectMocks
    private SongSearchService songSearchService;

    private Song sampleSong;

    @BeforeEach
    void setUp() {
        sampleSong = Song.builder()
                .id("bolly-diltodke")
                .title("Dil Tod Ke")
                .artist("B Praak, Rochak Kohli")
                .album("Dil Tod Ke")
                .genre("Romantic")
                .duration(284)
                .releaseYear(2020)
                .coverImageUrl("/default_cover.jpg")
                .audioUrl("/uploads/songs/bolly-diltodke.mp3")
                .build();
    }

    @Test
    void testIndexSong_Success() {
        songSearchService.indexSong(sampleSong);
        verify(songElasticsearchRepository).save(any(SongIndex.class));
    }

    @Test
    void testDeleteSong_Success() {
        songSearchService.deleteSong("bolly-diltodke");
        verify(songElasticsearchRepository).deleteById("bolly-diltodke");
    }

    @Test
    void testBulkIndex_Success() {
        songSearchService.bulkIndex(List.of(sampleSong));
        verify(songElasticsearchRepository).saveAll(anyList());
    }

    @Test
    void testCount_Success() {
        when(songElasticsearchRepository.count()).thenReturn(18L);
        long count = songSearchService.count();
        assertEquals(18L, count);
        verify(songElasticsearchRepository).count();
    }

    @Test
    void testSearchSongs_Success() {
        @SuppressWarnings("unchecked")
        SearchHits<SongIndex> mockSearchHits = mock(SearchHits.class);
        @SuppressWarnings("unchecked")
        SearchHit<SongIndex> mockHit = mock(SearchHit.class);

        SongIndex index = SongIndex.fromSong(sampleSong);
        when(mockHit.getContent()).thenReturn(index);
        when(mockSearchHits.getTotalHits()).thenReturn(1L);
        when(mockSearchHits.getSearchHits()).thenReturn(List.of(mockHit));

        when(elasticsearchOperations.search(any(Query.class), eq(SongIndex.class)))
                .thenReturn(mockSearchHits);

        PaginatedSongsResponse response = songSearchService.searchSongs(1, 8, "dil", "Romantic", null, null);

        assertNotNull(response);
        assertEquals(1, response.getCurrentPage());
        assertEquals(8, response.getLimit());
        assertEquals(1L, response.getTotalSongs());
        assertEquals(1, response.getSongs().size());
        assertEquals("Dil Tod Ke", response.getSongs().get(0).getTitle());
    }
}
