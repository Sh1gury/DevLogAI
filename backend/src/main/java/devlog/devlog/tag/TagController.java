package devlog.devlog.tag;

import devlog.devlog.tag.dto.StatsResponse;
import devlog.devlog.tag.dto.TagRequest;
import devlog.devlog.tag.dto.TagResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

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
        return tagService.getTagStats(userId, from, to);
    }
}
