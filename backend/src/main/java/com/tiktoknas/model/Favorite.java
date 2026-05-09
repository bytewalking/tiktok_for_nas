package com.tiktoknas.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "favorite")
@Data
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "video_id", unique = true)
    private Long videoId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
