package com.tiktoknas.repository;

import com.tiktoknas.model.PlayHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PlayHistoryRepository extends JpaRepository<PlayHistory, Long> {
    Optional<PlayHistory> findByVideoId(Long videoId);
    List<PlayHistory> findAllByOrderByUpdatedAtDesc();

    @Query(value = "SELECT DATE(updated_at) AS day, SUM(progress) AS total_secs, COUNT(*) AS watch_count " +
                   "FROM play_history GROUP BY DATE(updated_at) ORDER BY day DESC LIMIT 365",
           nativeQuery = true)
    List<Object[]> findDailyAggregates();
}
