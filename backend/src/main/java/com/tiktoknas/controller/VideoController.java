package com.tiktoknas.controller;

import com.tiktoknas.model.PlayHistory;
import com.tiktoknas.model.Video;
import com.tiktoknas.service.HlsService;
import com.tiktoknas.service.VideoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;
    private final HlsService hlsService;

    @GetMapping("/videos")
    public List<Video> listVideos() {
        return videoService.getAllVideos();
    }

    @GetMapping("/video/{id}")
    public void streamVideo(@PathVariable Long id,
                            HttpServletRequest request,
                            HttpServletResponse response) throws IOException {
        videoService.streamVideo(id, request, response);
    }

    @GetMapping("/video/{id}/hls")
    public ResponseEntity<Map<String, String>> getHlsUrl(@PathVariable Long id) {
        String url = hlsService.getOrGenerateHls(id);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @GetMapping("/covers/{filename:.+}")
    public void serveCover(@PathVariable String filename,
                           HttpServletResponse response) throws IOException {
        videoService.serveCover(filename, response);
    }

    @PostMapping("/history/{videoId}")
    public ResponseEntity<Void> updateHistory(@PathVariable Long videoId,
                                              @RequestBody Map<String, Long> body) {
        videoService.updateHistory(videoId, body.getOrDefault("progress", 0L));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/history")
    public List<PlayHistory> getHistory() {
        return videoService.getHistory();
    }

    @PostMapping("/scan")
    public ResponseEntity<Map<String, String>> triggerScan() {
        videoService.triggerScan();
        return ResponseEntity.ok(Map.of("status", "scan started"));
    }
}
