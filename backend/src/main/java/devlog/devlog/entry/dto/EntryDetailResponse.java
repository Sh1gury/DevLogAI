package devlog.devlog.entry.dto;

import devlog.devlog.tag.dto.TagResponse;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class EntryDetailResponse {
    UUID id;
    String content;
    LocalDate entryDate;
    Integer moodScore;
    Boolean isPublic;
    List<TagResponse> tags;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
