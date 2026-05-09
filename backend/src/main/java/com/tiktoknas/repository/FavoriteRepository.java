package com.tiktoknas.repository;

import com.tiktoknas.model.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    Optional<Favorite> findByVideoId(Long videoId);
    boolean existsByVideoId(Long videoId);
    List<Favorite> findAllByOrderByCreatedAtDesc();
}
