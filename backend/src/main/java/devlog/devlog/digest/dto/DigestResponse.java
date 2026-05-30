package devlog.devlog.digest.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class DigestResponse {
    UUID id;
    LocalDate weekStart;
    LocalDate weekEnd;
    String aiSummary;
    Map<String, Object> stats;
    LocalDateTime generatedAt;
}
