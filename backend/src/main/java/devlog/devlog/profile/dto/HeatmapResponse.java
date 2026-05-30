package devlog.devlog.profile.dto;

import java.time.LocalDate;
import java.util.Map;

public record HeatmapResponse(
        int year,
        Map<LocalDate, Integer> data,
        int totalEntries,
        int activeDays,
        int maxEntriesInDay
) {}
