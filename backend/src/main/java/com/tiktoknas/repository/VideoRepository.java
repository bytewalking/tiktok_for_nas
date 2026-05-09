package com.tiktoknas.repository;

import com.tiktoknas.model.Video;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VideoRepository extends JpaRepository<Video, Long> {
    Optional<Video> findByPath(String path);
}
