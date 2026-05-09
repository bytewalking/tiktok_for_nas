package com.tiktoknas.service;

import com.tiktoknas.config.SmbProperties;
import com.tiktoknas.model.AppSetting;
import com.tiktoknas.repository.AppSettingRepository;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettingsService {

    private static final String K_SMB_ENABLED  = "smb.enabled";
    private static final String K_SMB_HOST      = "smb.host";
    private static final String K_SMB_SHARE     = "smb.share";
    private static final String K_SMB_PATH      = "smb.path";
    private static final String K_SMB_DOMAIN    = "smb.domain";
    private static final String K_SMB_USERNAME  = "smb.username";
    private static final String K_SMB_PASSWORD  = "smb.password";
    private static final String K_VIDEO_DIR     = "video.dir";

    private final AppSettingRepository repo;
    private final SmbProperties smbProperties;

    @Value("${app.video-dir:/videos}")
    private String envVideoDir;

    /** Loaded at startup, overrides env vars if DB has saved values. */
    @PostConstruct
    public void init() {
        smbProperties.setEnabled(getBool(K_SMB_ENABLED,   smbProperties.isEnabled()));
        smbProperties.setHost(get(K_SMB_HOST,             orEmpty(smbProperties.getHost())));
        smbProperties.setShare(get(K_SMB_SHARE,           orEmpty(smbProperties.getShare())));
        smbProperties.setPath(get(K_SMB_PATH,             orEmpty(smbProperties.getPath())));
        smbProperties.setDomain(get(K_SMB_DOMAIN,         orEmpty(smbProperties.getDomain())));
        smbProperties.setUsername(get(K_SMB_USERNAME,     orEmpty(smbProperties.getUsername())));
        smbProperties.setPassword(get(K_SMB_PASSWORD,     orEmpty(smbProperties.getPassword())));
        log.info("Settings loaded. SMB enabled={}, videoDir={}",
                smbProperties.isEnabled(), getVideoDir());
    }

    public String getVideoDir() {
        String saved = get(K_VIDEO_DIR, "");
        return saved.isEmpty() ? envVideoDir : saved;
    }

    // ── DTO ───────────────────────────────────────────────────────────────────

    @Data
    public static class SmbSettingsDto {
        private boolean enabled;
        private String host = "";
        private String share = "videos";
        private String path = "";
        private String domain = "";
        private String username = "";
        /** Password returned as empty string; only saved when non-empty on POST. */
        private String password = "";
        private String videoDir = "";
    }

    public SmbSettingsDto getSettings() {
        SmbSettingsDto dto = new SmbSettingsDto();
        dto.setEnabled(smbProperties.isEnabled());
        dto.setHost(orEmpty(smbProperties.getHost()));
        dto.setShare(orEmpty(smbProperties.getShare()));
        dto.setPath(orEmpty(smbProperties.getPath()));
        dto.setDomain(orEmpty(smbProperties.getDomain()));
        dto.setUsername(orEmpty(smbProperties.getUsername()));
        dto.setPassword("");  // never expose password
        dto.setVideoDir(getVideoDir());
        return dto;
    }

    public void saveSettings(SmbSettingsDto dto) {
        save(K_SMB_ENABLED,  String.valueOf(dto.isEnabled()));
        save(K_SMB_HOST,     orEmpty(dto.getHost()));
        save(K_SMB_SHARE,    orEmpty(dto.getShare()));
        save(K_SMB_PATH,     orEmpty(dto.getPath()));
        save(K_SMB_DOMAIN,   orEmpty(dto.getDomain()));
        save(K_SMB_USERNAME, orEmpty(dto.getUsername()));
        // Only update password if provided
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            save(K_SMB_PASSWORD, dto.getPassword());
        }
        if (dto.getVideoDir() != null && !dto.getVideoDir().isEmpty()) {
            save(K_VIDEO_DIR, dto.getVideoDir());
        }

        // Apply to live bean immediately (no restart needed)
        smbProperties.setEnabled(dto.isEnabled());
        smbProperties.setHost(orEmpty(dto.getHost()));
        smbProperties.setShare(orEmpty(dto.getShare()));
        smbProperties.setPath(orEmpty(dto.getPath()));
        smbProperties.setDomain(orEmpty(dto.getDomain()));
        smbProperties.setUsername(orEmpty(dto.getUsername()));
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            smbProperties.setPassword(dto.getPassword());
        }

        log.info("Settings saved. SMB enabled={}, host={}", dto.isEnabled(), dto.getHost());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String get(String key, String fallback) {
        return repo.findById(key).map(AppSetting::getValue).orElse(fallback);
    }

    private boolean getBool(String key, boolean fallback) {
        return repo.findById(key)
                .map(s -> Boolean.parseBoolean(s.getValue()))
                .orElse(fallback);
    }

    private void save(String key, String value) {
        repo.save(new AppSetting(key, value == null ? "" : value));
    }

    private static String orEmpty(String s) {
        return s == null ? "" : s;
    }
}
