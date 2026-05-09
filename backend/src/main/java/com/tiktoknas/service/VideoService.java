package com.tiktoknas.service;

import com.tiktoknas.model.PlayHistory;
import com.tiktoknas.model.Video;
import com.tiktoknas.repository.PlayHistoryRepository;
import com.tiktoknas.repository.VideoRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoService {

    private final VideoRepository videoRepository;
    private final PlayHistoryRepository historyRepository;
    private final VideoScanService scanService;
    private final SmbVideoScanService smbVideoScanService;

    @Value("${app.cover-dir:./data/covers}")
    private String coverDir;

    public List<Video> getAllVideos() {
        return videoRepository.findAll(
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
    }

    public void streamVideo(Long id, HttpServletRequest request, HttpServletResponse response) throws IOException {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found: " + id));

        File file = new File(video.getPath());
        if (!file.exists()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        long fileSize = file.length();
        String rangeHeader = request.getHeader("Range");
        long start = 0;
        long end = fileSize - 1;

        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            String[] parts = rangeHeader.substring(6).split("-");
            start = Long.parseLong(parts[0]);
            if (parts.length > 1 && !parts[1].isEmpty()) {
                end = Long.parseLong(parts[1]);
            }
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        } else {
            response.setStatus(HttpServletResponse.SC_OK);
        }

        long contentLength = end - start + 1;

        // ETag based on file identity — enables browser to cache Range responses
        String etag = '"' + Long.toHexString(file.lastModified()) + '-' + Long.toHexString(fileSize) + '"';
        String ifNoneMatch = request.getHeader("If-None-Match");
        if (rangeHeader == null && etag.equals(ifNoneMatch)) {
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }

        response.setHeader("Content-Type", resolveContentType(video.getPath()));
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("Content-Length", String.valueOf(contentLength));
        response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileSize);
        response.setHeader("ETag", etag);
        response.setHeader("Cache-Control", "public, max-age=3600");

        // SMB path: stream directly from NAS
        if (smbVideoScanService.isSmbPath(video.getPath())) {
            try (OutputStream out = response.getOutputStream()) {
                smbVideoScanService.streamFromSmb(video.getPath(), start, end, out);
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException("SMB stream error", e);
            }
            return;
        }

        try (RandomAccessFile raf = new RandomAccessFile(file, "r");
             OutputStream out = response.getOutputStream()) {
            raf.seek(start);
            byte[] buffer = new byte[65536];
            long remaining = contentLength;
            int bytesRead;
            while (remaining > 0) {
                int toRead = (int) Math.min(buffer.length, remaining);
                bytesRead = raf.read(buffer, 0, toRead);
                if (bytesRead == -1) break;
                out.write(buffer, 0, bytesRead);
                remaining -= bytesRead;
            }
        }
    }

    public void serveCover(String filename, HttpServletResponse response) throws IOException {
        File file = new File(coverDir, filename);
        if (!file.exists()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        response.setHeader("Content-Type", "image/jpeg");
        response.setHeader("Content-Length", String.valueOf(file.length()));
        response.setHeader("Cache-Control", "public, max-age=86400");

        try (FileInputStream fis = new FileInputStream(file);
             OutputStream out = response.getOutputStream()) {
            byte[] buffer = new byte[16384];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
    }

    public void updateHistory(Long videoId, Long progress) {
        PlayHistory history = historyRepository.findByVideoId(videoId)
                .orElseGet(() -> {
                    PlayHistory h = new PlayHistory();
                    h.setVideoId(videoId);
                    return h;
                });
        history.setProgress(progress);
        history.setUpdatedAt(LocalDateTime.now());
        historyRepository.save(history);
    }

    public List<PlayHistory> getHistory() {
        return historyRepository.findAllByOrderByUpdatedAtDesc();
    }

    public void triggerScan() {
        scanService.rescan();
    }

    private String resolveContentType(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".mkv")) return "video/x-matroska";
        if (lower.endsWith(".mov")) return "video/quicktime";
        if (lower.endsWith(".avi")) return "video/x-msvideo";
        if (lower.endsWith(".webm")) return "video/webm";
        return "video/mp4";
    }
}
