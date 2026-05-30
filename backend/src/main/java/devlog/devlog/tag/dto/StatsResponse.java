package devlog.devlog.tag.dto;

import java.util.List;
import java.util.Map;

public record StatsResponse(
        Map<String, Integer> tagCounts,
        List<TagStatItem> topTags,
        Double avgMood,
        int totalEntries
) {
    public record TagStatItem(String name, int count, double percent) {}
}
