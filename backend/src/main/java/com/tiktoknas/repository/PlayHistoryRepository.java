package com.tiktoknas.repository;

import com.tiktoknas.model.PlayHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayHistoryRepository extends JpaRepository<PlayHistory, Long> {
    Optional<PlayHistory> findByVideoId(Long videoId);
    List<PlayHistory> findAllByOrderByUpdatedAtDesc();
}
