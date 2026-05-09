package com.tiktoknas.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "video")
@Data
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(unique = true)
    private String path;

    private Long duration;

    private Long size;

    private String codec;

    private String resolution;

    private String cover;

    @Column(name = "hls_ready")
    private Boolean hlsReady = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
