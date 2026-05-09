package com.tiktoknas.controller;

import com.tiktoknas.repository.PlayHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final PlayHistoryRepository historyRepository;

    @GetMapping("/watch-summary")
    public Map<String, Object> getWatchSummary() {
        List<Object[]> rows = historyRepository.findDailyAggregates();

        List<Map<String, Object>> byDay = rows.stream().map(row -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("date", row[0] != null ? row[0].toString() : "");
            entry.put("seconds", row[1] != null ? ((Number) row[1]).longValue() : 0L);
            entry.put("count",   row[2] != null ? ((Number) row[2]).longValue() : 0L);
            return entry;
        }).collect(Collectors.toList());

        long totalSeconds = byDay.stream().mapToLong(d -> (Long) d.get("seconds")).sum();
        long totalCount   = byDay.stream().mapToLong(d -> (Long) d.get("count")).sum();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalSeconds", totalSeconds);
        result.put("totalCount", totalCount);
        result.put("byDay", byDay);
        return result;
    }
}
