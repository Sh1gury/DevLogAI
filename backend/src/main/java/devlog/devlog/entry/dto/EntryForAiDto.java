package devlog.devlog.entry.dto;

import java.time.LocalDate;
import java.util.List;

public record EntryForAiDto(
        LocalDate entryDate,
        Integer moodScore,
        String content,
        List<String> tagNames
) {}
