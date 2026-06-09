package devlog.devlog.entry;

import devlog.devlog.common.exception.ResourceNotFoundException;
import devlog.devlog.common.exception.UnauthorizedException;
import devlog.devlog.entry.dto.EntryDetailResponse;
import devlog.devlog.entry.dto.EntryForAiDto;
import devlog.devlog.entry.dto.EntryRequest;
import devlog.devlog.entry.dto.EntryResponse;
import devlog.devlog.profile.dto.HeatmapResponse;
import devlog.devlog.tag.TagService;
import devlog.devlog.tag.dto.TagResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class EntryService {

    private final EntryRepository entryRepository;
    private final TagService tagService;

    @Transactional
    public EntryResponse createEntry(UUID userId, EntryRequest request) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        Entry entry = Entry.builder()
                .userId(userId)
                .content(request.getContent())
                .entryDate(request.getEntryDate())
                .moodScore(request.getMoodScore())
                .isPublic(Boolean.TRUE.equals(request.getIsPublic()))
                .createdAt(now)
                .updatedAt(now)
                .build();
        entryRepository.save(entry);
        tagService.syncTagsForEntry(entry.getId(), userId, parseTagsFromContent(request.getContent()));
        return toResponse(reloadWithTags(entry.getId()));
    }

    @Transactional
    public EntryResponse updateEntry(UUID userId, UUID entryId, EntryRequest request) {
        if (!entryRepository.existsByIdAndUserId(entryId, userId))
            throw new UnauthorizedException("Access denied");
        Entry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Entry not found"));
        entry.setContent(request.getContent());
        entry.setEntryDate(request.getEntryDate());
        entry.setMoodScore(request.getMoodScore());
        entry.setPublic(Boolean.TRUE.equals(request.getIsPublic()));
        entry.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        tagService.deleteTagsForEntry(entryId);
        entryRepository.save(entry);
        tagService.syncTagsForEntry(entryId, userId, parseTagsFromContent(request.getContent()));
        return toResponse(reloadWithTags(entryId));
    }

    @Transactional
    public void deleteEntry(UUID userId, UUID entryId) {
        if (!entryRepository.existsByIdAndUserId(entryId, userId))
            throw new UnauthorizedException("Access denied");
        entryRepository.deleteById(entryId);
    }

    @Transactional(readOnly = true)
    public EntryDetailResponse getEntry(UUID userId, UUID entryId) {
        Entry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Entry not found"));
        if (!entry.getUserId().equals(userId))
            throw new UnauthorizedException("Access denied");
        return toDetailResponse(entry);
    }

    @Transactional(readOnly = true)
    public Page<EntryResponse> getEntries(UUID userId, LocalDate from, LocalDate to, Pageable pageable) {
        Page<Entry> page = (from != null && to != null)
                ? entryRepository.findByUserIdAndEntryDateBetweenOrderByEntryDateDesc(userId, from, to, pageable)
                : entryRepository.findByUserIdOrderByEntryDateDesc(userId, pageable);
        return page.map(this::toResponse);
    }

    /** Unpaginated list used internally by other modules (e.g. profile aggregation). */
    @Transactional(readOnly = true)
    public List<EntryResponse> getEntries(UUID userId, LocalDate from, LocalDate to) {
        return entryRepository.findByUserIdOrderByEntryDateDesc(userId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public HeatmapResponse getHeatmapData(UUID userId, int year) {
        Map<LocalDate, Integer> data = new LinkedHashMap<>();
        entryRepository.countEntriesByDateForYear(userId, year)
                .forEach(r -> data.put(r.entryDate(), (int) r.count()));
        int totalEntries = data.values().stream().mapToInt(i -> i).sum();
        int maxEntriesInDay = data.values().stream().mapToInt(i -> i).max().orElse(0);
        return new HeatmapResponse(year, data, totalEntries, data.size(), maxEntriesInDay);
    }

    @Transactional
    public EntryResponse toggleVisibility(UUID userId, UUID entryId, boolean isPublic) {
        if (!entryRepository.existsByIdAndUserId(entryId, userId))
            throw new UnauthorizedException("Access denied");
        Entry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Entry not found"));
        entry.setPublic(isPublic);
        entry.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        return toResponse(entryRepository.save(entry));
    }

    // ── Cross-module read API (DTOs only) ───────────────────────────────────────

    @Transactional(readOnly = true)
    public List<EntryForAiDto> getEntriesForPeriod(UUID userId, LocalDate from, LocalDate to) {
        return entryRepository.findEntriesWithTagsBetween(userId, from, to).stream()
                .map(e -> new EntryForAiDto(
                        e.getEntryDate(),
                        e.getMoodScore(),
                        e.getContent(),
                        e.getEntryTags() == null ? List.of() :
                                e.getEntryTags().stream().map(et -> et.getTag().getName()).toList()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EntryResponse> getPublicEntries(UUID userId) {
        return entryRepository.findPublicEntriesByUserId(userId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<LocalDate> getAllEntryDates(UUID userId) {
        return entryRepository.findAllEntryDatesByUserId(userId);
    }

    @Transactional(readOnly = true)
    public long countActiveDays(UUID userId) {
        return entryRepository.countActiveDays(userId);
    }

    @Transactional(readOnly = true)
    public double getAverageMoodForPeriod(UUID userId, LocalDate from, LocalDate to) {
        Double avg = entryRepository.averageMoodForPeriod(userId, from, to);
        return avg != null ? avg : 0;
    }

    @Transactional(readOnly = true)
    public int countEntriesForPeriod(UUID userId, LocalDate from, LocalDate to) {
        return (int) entryRepository.countByUserIdAndEntryDateBetween(userId, from, to);
    }

    // ── Private ─────────────────────────────────────────────────────────────────

    private Entry reloadWithTags(UUID entryId) {
        return entryRepository.findByIdWithTags(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Entry not found"));
    }

    private List<String> parseTagsFromContent(String content) {
        Pattern pattern = Pattern.compile("#(\\w+)");
        Matcher matcher = pattern.matcher(content);
        List<String> tags = new ArrayList<>();
        while (matcher.find()) tags.add(matcher.group(1).toLowerCase());
        return tags.stream().distinct().toList();
    }

    private EntryResponse toResponse(Entry entry) {
        List<TagResponse> tags = entry.getEntryTags() == null ? List.of() :
                entry.getEntryTags().stream()
                        .map(et -> new TagResponse(
                                et.getTag().getId(), et.getTag().getName(),
                                et.getTag().getColor(), 0))
                        .toList();
        String preview = entry.getContent().length() > 200
                ? entry.getContent().substring(0, 200)
                : entry.getContent();
        return EntryResponse.builder()
                .id(entry.getId())
                .contentPreview(preview)
                .entryDate(entry.getEntryDate())
                .moodScore(entry.getMoodScore())
                .isPublic(entry.isPublic())
                .tags(tags)
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .build();
    }

    private EntryDetailResponse toDetailResponse(Entry entry) {
        List<TagResponse> tags = entry.getEntryTags() == null ? List.of() :
                entry.getEntryTags().stream()
                        .map(et -> new TagResponse(
                                et.getTag().getId(), et.getTag().getName(),
                                et.getTag().getColor(), 0))
                        .toList();
        return EntryDetailResponse.builder()
                .id(entry.getId())
                .content(entry.getContent())
                .entryDate(entry.getEntryDate())
                .moodScore(entry.getMoodScore())
                .isPublic(entry.isPublic())
                .tags(tags)
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .build();
    }
}
