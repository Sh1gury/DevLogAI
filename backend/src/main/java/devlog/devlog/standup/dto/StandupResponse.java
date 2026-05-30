package devlog.devlog.standup.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record StandupResponse(
        UUID id,
        LocalDate standupDate,
        String yesterday,
        String today,
        String blockers,
        LocalDateTime generatedAt
) {}
