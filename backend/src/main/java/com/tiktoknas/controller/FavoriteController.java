package com.tiktoknas.controller;

import com.tiktoknas.model.Favorite;
import com.tiktoknas.model.Video;
import com.tiktoknas.repository.FavoriteRepository;
import com.tiktoknas.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteRepository favoriteRepository;
    private final VideoRepository videoRepository;

    /** All favorited video IDs — used by the feed to mark hearts */
    @GetMapping("/ids")
    public List<Long> getFavoriteIds() {
        return favoriteRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(Favorite::getVideoId)
                .collect(Collectors.toList());
    }

    /** Full video objects for the favorites page */
    @GetMapping
    public List<Video> getFavoriteVideos() {
        List<Long> ids = favoriteRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(Favorite::getVideoId)
                .collect(Collectors.toList());
        // Preserve order from favorites table
        List<Video> videos = videoRepository.findAllById(ids);
        videos.sort((a, b) -> ids.indexOf(a.getId()) - ids.indexOf(b.getId()));
        return videos;
    }

    /** Toggle: add if not favorited, remove if already favorited */
    @PostMapping("/{videoId}")
    public ResponseEntity<Map<String, Object>> toggle(@PathVariable Long videoId) {
        Optional<Favorite> existing = favoriteRepository.findByVideoId(videoId);
        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            return ResponseEntity.ok(Map.of("favorited", false, "videoId", videoId));
        } else {
            Favorite fav = new Favorite();
            fav.setVideoId(videoId);
            fav.setCreatedAt(LocalDateTime.now());
            favoriteRepository.save(fav);
            return ResponseEntity.ok(Map.of("favorited", true, "videoId", videoId));
        }
    }
}
