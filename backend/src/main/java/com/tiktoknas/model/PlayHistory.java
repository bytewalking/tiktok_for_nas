package com.tiktoknas.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "play_history")
@Data
public class PlayHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "video_id")
    private Long videoId;

    private Long progress;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
