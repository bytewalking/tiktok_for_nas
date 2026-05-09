package com.tiktoknas.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tiktoknas.config.SmbProperties;
import com.tiktoknas.model.Video;
import com.tiktoknas.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoScanService implements ApplicationRunner {

    private final VideoRepository videoRepository;
    private final SmbProperties smbProperties;
    private final SmbVideoScanService smbVideoScanService;
    private final SettingsService settingsService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.video-dir:/videos}")
    private String defaultVideoDir;

    @Value("${app.cover-dir:./data/covers}")
    private String coverDir;

    private static final List<String> SUPPORTED_EXT =
            Arrays.asList(".mp4", ".mkv", ".mov", ".avi", ".webm");

    @Override
    public void run(ApplicationArguments args) {
        if (smbProperties.isEnabled()) {
            smbVideoScanService.scan();
        } else {
            String dir = settingsService.getVideoDir();
            log.info("Starting video scan: {}", dir);
            scanDirectory(new File(dir));
            log.info("Video scan complete. Total: {}", videoRepository.count());
        }
    }

    public void rescan() {
        if (smbProperties.isEnabled()) {
            smbVideoScanService.scan();
        } else {
            String dir = settingsService.getVideoDir();
            log.info("Rescanning: {}", dir);
            scanDirectory(new File(dir));
            log.info("Rescan complete. Total: {}", videoRepository.count());
        }
    }

    private void scanDirectory(File dir) {
        if (!dir.exists() || !dir.isDirectory()) {
            log.warn("Video directory not found: {}", dir.getAbsolutePath());
            return;
        }
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file);
            } else if (isVideoFile(file)) {
                processVideo(file);
            }
        }
    }

    private boolean isVideoFile(File file) {
        String name = file.getName().toLowerCase();
        return SUPPORTED_EXT.stream().anyMatch(name::endsWith);
    }

    private void processVideo(File file) {
        String path = file.getAbsolutePath();
        if (videoRepository.findByPath(path).isPresent()) return;

        try {
            Video video = new Video();
            video.setTitle(stripExtension(file.getName()));
            video.setPath(path);
            video.setSize(file.length());
            video.setCreatedAt(LocalDateTime.now());

            extractMetadata(video, file);

            // Save to get ID first
            video = videoRepository.save(video);

            generateCover(video, file);
            videoRepository.save(video);

            log.info("Indexed: {}", file.getName());
        } catch (Exception e) {
            log.error("Failed to index: {}", path, e);
        }
    }

    private String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    private void extractMetadata(Video video, File file) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "ffprobe", "-v", "quiet",
                    "-print_format", "json",
                    "-show_streams", "-show_format",
                    file.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);

            JsonNode root = objectMapper.readTree(output);

            JsonNode format = root.path("format");
            if (!format.isMissingNode()) {
                String dur = format.path("duration").asText(null);
                if (dur != null) {
                    video.setDuration((long) Double.parseDouble(dur));
                }
            }

            JsonNode streams = root.path("streams");
            for (JsonNode stream : streams) {
                if ("video".equals(stream.path("codec_type").asText())) {
                    video.setCodec(stream.path("codec_name").asText(null));
                    int w = stream.path("width").asInt(0);
                    int h = stream.path("height").asInt(0);
                    if (w > 0 && h > 0) {
                        video.setResolution(w + "x" + h);
                    }
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("ffprobe failed for {}: {}", file.getName(), e.getMessage());
        }
    }

    private void generateCover(Video video, File file) {
        try {
            File coverDirFile = new File(coverDir);
            coverDirFile.mkdirs();
            File coverFile = new File(coverDirFile, video.getId() + ".jpg");

            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-y",
                    "-ss", "00:00:03",
                    "-i", file.getAbsolutePath(),
                    "-vframes", "1",
                    "-q:v", "3",
                    "-vf", "scale=480:-1",
                    coverFile.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            process.getInputStream().readAllBytes();
            process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);

            if (coverFile.exists()) {
                video.setCover("/api/covers/" + video.getId() + ".jpg");
            }
        } catch (Exception e) {
            log.warn("Cover generation failed for {}: {}", file.getName(), e.getMessage());
        }
    }
}
