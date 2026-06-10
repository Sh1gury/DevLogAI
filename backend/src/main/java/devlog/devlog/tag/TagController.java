package devlog.devlog.tag;

import devlog.devlog.entry.EntryService;
import devlog.devlog.tag.dto.StatsResponse;
import devlog.devlog.tag.dto.TagRequest;
import devlog.devlog.tag.dto.TagResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class TagController {

    private final TagService tagService;
    private final EntryService entryService;

    @GetMapping
    public List<TagResponse> getTags(@AuthenticationPrincipal UUID userId) {
        return tagService.getTags(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TagResponse createTag(@AuthenticationPrincipal UUID userId,
                                 @Valid @RequestBody TagRequest request) {
        return tagService.createTag(userId, request);
    }

    @PutMapping("/{id}")
    public TagResponse updateTag(@AuthenticationPrincipal UUID userId,
                                 @PathVariable UUID id,
                                 @Valid @RequestBody TagRequest request) {
        return tagService.updateTag(userId, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTag(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        tagService.deleteTag(userId, id);
    }

    @GetMapping("/stats")
    public StatsResponse getTagStats(@AuthenticationPrincipal UUID userId,
                                     @RequestParam(required = false) LocalDate from,
                                     @RequestParam(required = false) LocalDate to) {
        LocalDate effectiveFrom = from != null ? from : LocalDate.of(2000, 1, 1);
        LocalDate effectiveTo = to != null ? to : LocalDate.now(ZoneOffset.UTC);
        double avgMood = entryService.getAverageMoodForPeriod(userId, effectiveFrom, effectiveTo);
        int totalEntries = entryService.countEntriesForPeriod(userId, effectiveFrom, effectiveTo);
        return tagService.getTagStats(userId, effectiveFrom, effectiveTo, avgMood, totalEntries);
    }
}
