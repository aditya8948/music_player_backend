package com.musekit.backend.repository;

import com.musekit.backend.model.SongIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SongElasticsearchRepository extends ElasticsearchRepository<SongIndex, String> {
}
