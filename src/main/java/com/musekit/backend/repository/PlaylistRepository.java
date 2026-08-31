package com.musekit.backend.repository;

import com.musekit.backend.model.Playlist;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlaylistRepository extends MongoRepository<Playlist, String> {
    List<Playlist> findByUserId(String userId);
    List<Playlist> findAllByOrderByCreatedAtDesc();
}
