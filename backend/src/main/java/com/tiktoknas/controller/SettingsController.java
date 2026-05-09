package com.tiktoknas.controller;

import com.tiktoknas.service.SettingsService;
import com.tiktoknas.service.SettingsService.SmbSettingsDto;
import com.tiktoknas.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;
    private final VideoService videoService;

    @GetMapping("/smb")
    public SmbSettingsDto getSmb() {
        return settingsService.getSettings();
    }

    @PostMapping("/smb")
    public ResponseEntity<Map<String, String>> saveSmb(@RequestBody SmbSettingsDto dto) {
        settingsService.saveSettings(dto);
        return ResponseEntity.ok(Map.of("status", "saved"));
    }

    @PostMapping("/smb/scan")
    public ResponseEntity<Map<String, String>> saveAndScan(@RequestBody SmbSettingsDto dto) {
        settingsService.saveSettings(dto);
        videoService.triggerScan();
        return ResponseEntity.ok(Map.of("status", "scanning"));
    }
}
