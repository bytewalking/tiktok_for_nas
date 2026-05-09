package com.tiktoknas.service;

import com.tiktoknas.model.Video;
import com.tiktoknas.repository.VideoRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class HlsService {

    private final VideoRepository videoRepository;

    @Value("${app.hls-dir:./data/hls}")
    private String hlsBaseDir;

    /**
     * Returns HLS playlist URL if ready, otherwise starts background generation
     * and returns the direct stream URL as fallback.
     */
    public String getOrGenerateHls(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found: " + videoId));

        Path hlsDir = Paths.get(hlsBaseDir, videoId.toString());
        Path m3u8 = hlsDir.resolve("index.m3u8");

        if (m3u8.toFile().exists()) {
            if (!Boolean.TRUE.equals(video.getHlsReady())) {
                video.setHlsReady(true);
                videoRepository.save(video);
            }
            return "/hls/" + videoId + "/index.m3u8";
        }

        // Trigger background generation
        generateHlsAsync(video, hlsDir);

        // Return direct stream as fallback
        return "/api/video/" + videoId;
    }

    @Async("hlsTaskExecutor")
    public void generateHlsAsync(Video video, Path hlsDir) {
        try {
            Files.createDirectories(hlsDir);
            log.info("Generating HLS for video {}: {}", video.getId(), video.getTitle());

            String segmentPattern = hlsDir.resolve("segment_%04d.ts").toString();
            String m3u8Path = hlsDir.resolve("index.m3u8").toString();

            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-y",
                    "-i", video.getPath(),
                    "-c:v", "copy",
                    "-c:a", "aac",
                    "-start_number", "0",
                    "-hls_time", "10",
                    "-hls_list_size", "0",
                    "-hls_segment_filename", segmentPattern,
                    "-f", "hls",
                    m3u8Path
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            boolean finished = process.waitFor(30, TimeUnit.MINUTES);

            if (finished && process.exitValue() == 0) {
                video.setHlsReady(true);
                videoRepository.save(video);
                log.info("HLS ready for video {}", video.getId());
            } else {
                log.error("HLS generation failed for video {}: {}", video.getId(), output);
            }
        } catch (Exception e) {
            log.error("HLS generation error for video {}", video.getId(), e);
        }
    }

    public void serveHlsFile(Long videoId, String filename, HttpServletResponse response) throws IOException {
        // Security: prevent path traversal
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        Path filePath = Paths.get(hlsBaseDir, videoId.toString(), filename);
        File file = filePath.toFile();

        if (!file.exists()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String contentType = filename.endsWith(".m3u8")
                ? "application/vnd.apple.mpegurl"
                : "video/mp2t";

        response.setHeader("Content-Type", contentType);
        response.setHeader("Content-Length", String.valueOf(file.length()));
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Access-Control-Allow-Origin", "*");

        try (FileInputStream fis = new FileInputStream(file);
             OutputStream out = response.getOutputStream()) {
            byte[] buffer = new byte[65536];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
    }
}
