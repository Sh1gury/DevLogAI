package devlog.devlog.entry.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class EntryRequest {

    @NotBlank
    String content;

    @NotNull
    LocalDate entryDate;

    @Min(1) @Max(5)
    Integer moodScore;

    Boolean isPublic;
}
