package devlog.devlog.entry;

import devlog.devlog.entry.dto.EntryDetailResponse;
import devlog.devlog.entry.dto.EntryRequest;
import devlog.devlog.entry.dto.EntryResponse;
import devlog.devlog.profile.dto.HeatmapResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@RestController
@RequestMapping("/api/entries")
@RequiredArgsConstructor
public class EntryController {

    private final EntryService entryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EntryResponse createEntry(@AuthenticationPrincipal UUID userId,
                                     @Valid @RequestBody EntryRequest request) {
        return entryService.createEntry(userId, request);
    }

    @GetMapping
    public Page<EntryResponse> getEntries(@AuthenticationPrincipal UUID userId,
                                          @RequestParam(required = false) LocalDate from,
                                          @RequestParam(required = false) LocalDate to,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return entryService.getEntries(userId, from, to, pageable);
    }

    @GetMapping("/{id}")
    public EntryDetailResponse getEntry(@AuthenticationPrincipal UUID userId,
                                        @PathVariable UUID id) {
        return entryService.getEntry(userId, id);
    }

    @PutMapping("/{id}")
    public EntryResponse updateEntry(@AuthenticationPrincipal UUID userId,
                                     @PathVariable UUID id,
                                     @Valid @RequestBody EntryRequest request) {
        return entryService.updateEntry(userId, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEntry(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        entryService.deleteEntry(userId, id);
    }

    @GetMapping("/heatmap")
    public HeatmapResponse getHeatmap(@AuthenticationPrincipal UUID userId,
                                      @RequestParam(required = false) Integer year) {
        int targetYear = year != null ? year : LocalDate.now(ZoneOffset.UTC).getYear();
        return entryService.getHeatmapData(userId, targetYear);
    }

    @PatchMapping("/{id}/visibility")
    public EntryResponse toggleVisibility(@AuthenticationPrincipal UUID userId,
                                          @PathVariable UUID id,
                                          @RequestParam boolean isPublic) {
        return entryService.toggleVisibility(userId, id, isPublic);
    }
}
