package com.tiktoknas.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tiktoknas.config.SmbProperties;
import com.tiktoknas.model.Video;
import com.tiktoknas.repository.VideoRepository;
import jcifs.CIFSContext;
import jcifs.config.PropertyConfiguration;
import jcifs.context.BaseContext;
import jcifs.smb.NtlmPasswordAuthenticator;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * SMB 原生扫描服务：不依赖 OS 挂载，直接通过 CIFS 协议读取 NAS 文件。
 * 当 app.smb.enabled=true 时替代 VideoScanService 使用。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SmbVideoScanService {

    private final VideoRepository videoRepository;
    private final SmbProperties smb;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.cover-dir:./data/covers}")
    private String coverDir;

    private static final List<String> SUPPORTED_EXT =
            Arrays.asList(".mp4", ".mkv", ".mov", ".avi", ".webm");

    public void scan() {
        if (!smb.isEnabled()) {
            log.info("SMB scan disabled, skipping");
            return;
        }

        String smbUrl = buildSmbUrl("");
        log.info("Starting SMB scan: {}", smbUrl);

        try {
            CIFSContext ctx = buildContext();
            try (SmbFile root = new SmbFile(smbUrl, ctx)) {
                scanDirectory(root, ctx);
            }
            log.info("SMB scan complete. Total videos: {}", videoRepository.count());
        } catch (Exception e) {
            log.error("SMB scan failed: {}", e.getMessage(), e);
        }
    }

    private void scanDirectory(SmbFile dir, CIFSContext ctx) throws Exception {
        SmbFile[] files = dir.listFiles();
        if (files == null) return;

        for (SmbFile file : files) {
            String name = file.getName();
            if (file.isDirectory()) {
                scanDirectory(file, ctx);
            } else if (isVideoFile(name)) {
                processVideo(file, ctx);
            }
        }
    }

    private boolean isVideoFile(String name) {
        String lower = name.toLowerCase();
        return SUPPORTED_EXT.stream().anyMatch(lower::endsWith);
    }

    private void processVideo(SmbFile smbFile, CIFSContext ctx) {
        // Use SMB URL as the unique path key
        String smbPath = smbFile.getCanonicalPath();

        if (videoRepository.findByPath(smbPath).isPresent()) return;

        try {
            Video video = new Video();
            video.setTitle(stripExtension(smbFile.getName()));
            video.setPath(smbPath);
            video.setSize(smbFile.length());
            video.setCreatedAt(LocalDateTime.now());

            // Cache file locally for ffprobe metadata extraction
            Path tempFile = cacheLocally(smbFile);
            if (tempFile != null) {
                extractMetadata(video, tempFile.toFile());
            }

            video = videoRepository.save(video);

            if (tempFile != null) {
                generateCover(video, tempFile.toFile());
                videoRepository.save(video);
                Files.deleteIfExists(tempFile);
            }

            log.info("Indexed SMB video: {}", smbFile.getName());
        } catch (Exception e) {
            log.error("Failed to index SMB video {}: {}", smbPath, e.getMessage());
        }
    }

    /**
     * Downloads the first ~10MB of the file locally for ffprobe analysis.
     * For large videos we only need a small sample to get codec/resolution info.
     */
    private Path cacheLocally(SmbFile smbFile) {
        try {
            Path temp = Files.createTempFile("tiktoknas_", "_" + smbFile.getName());
            long maxBytes = 10L * 1024 * 1024; // 10MB for metadata

            try (SmbFileInputStream in = new SmbFileInputStream(smbFile);
                 FileOutputStream out = new FileOutputStream(temp.toFile())) {
                byte[] buf = new byte[65536];
                long written = 0;
                int read;
                while (written < maxBytes && (read = in.read(buf)) != -1) {
                    out.write(buf, 0, read);
                    written += read;
                }
            }
            return temp;
        } catch (Exception e) {
            log.warn("Could not cache SMB file for metadata: {}", e.getMessage());
            return null;
        }
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
                if (dur != null) video.setDuration((long) Double.parseDouble(dur));
            }
            for (JsonNode stream : root.path("streams")) {
                if ("video".equals(stream.path("codec_type").asText())) {
                    video.setCodec(stream.path("codec_name").asText(null));
                    int w = stream.path("width").asInt(0);
                    int h = stream.path("height").asInt(0);
                    if (w > 0 && h > 0) video.setResolution(w + "x" + h);
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("ffprobe failed: {}", e.getMessage());
        }
    }

    private void generateCover(Video video, File file) {
        try {
            File dir = new File(coverDir);
            dir.mkdirs();
            File coverFile = new File(dir, video.getId() + ".jpg");

            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-y", "-ss", "00:00:03",
                    "-i", file.getAbsolutePath(),
                    "-vframes", "1", "-q:v", "3",
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
            log.warn("Cover generation failed: {}", e.getMessage());
        }
    }

    /**
     * Stream a video file from NAS directly to HTTP response (used instead of local file streaming).
     */
    public void streamFromSmb(String smbPath, long start, long end,
                               OutputStream outputStream) throws Exception {
        CIFSContext ctx = buildContext();
        try (SmbFile smbFile = new SmbFile(smbPath, ctx);
             SmbFileInputStream in = new SmbFileInputStream(smbFile)) {
            in.skip(start);
            byte[] buf = new byte[65536];
            long remaining = end - start + 1;
            int read;
            while (remaining > 0 && (read = in.read(buf, 0, (int) Math.min(buf.length, remaining))) != -1) {
                outputStream.write(buf, 0, read);
                remaining -= read;
            }
        }
    }

    public boolean isSmbPath(String path) {
        return path != null && path.startsWith("smb://");
    }

    private String buildSmbUrl(String subPath) {
        StringBuilder url = new StringBuilder("smb://");
        url.append(smb.getHost()).append("/").append(smb.getShare()).append("/");
        if (smb.getPath() != null && !smb.getPath().isEmpty()) {
            url.append(smb.getPath()).append("/");
        }
        if (subPath != null && !subPath.isEmpty()) {
            url.append(subPath);
        }
        return url.toString();
    }

    private CIFSContext buildContext() throws Exception {
        Properties props = new Properties();
        props.setProperty("jcifs.smb.client.minVersion", "SMB202");
        props.setProperty("jcifs.smb.client.maxVersion", "SMB300");

        CIFSContext base = new BaseContext(new PropertyConfiguration(props));

        if (smb.getUsername() != null && !smb.getUsername().isEmpty()) {
            NtlmPasswordAuthenticator auth = new NtlmPasswordAuthenticator(
                    smb.getDomain(), smb.getUsername(), smb.getPassword());
            return base.withCredentials(auth);
        }
        return base;
    }

    private String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }
}
