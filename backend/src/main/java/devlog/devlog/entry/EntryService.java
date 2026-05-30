package devlog.devlog.entry;

import devlog.devlog.common.exception.ResourceNotFoundException;
import devlog.devlog.common.exception.UnauthorizedException;
import devlog.devlog.entry.dto.EntryDetailResponse;
import devlog.devlog.entry.dto.EntryRequest;
import devlog.devlog.entry.dto.EntryResponse;
import devlog.devlog.profile.dto.HeatmapResponse;
import devlog.devlog.tag.EntryTag;
import devlog.devlog.tag.EntryTagId;
import devlog.devlog.tag.EntryTagRepository;
import devlog.devlog.tag.Tag;
import devlog.devlog.tag.TagRepository;
import devlog.devlog.tag.dto.TagResponse;
import devlog.devlog.user.User;
import devlog.devlog.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class EntryService {

    private final EntryRepository entryRepository;
    private final TagRepository tagRepository;
    private final EntryTagRepository entryTagRepository;
    private final UserRepository userRepository;

    @Transactional
    public EntryResponse createEntry(UUID userId, EntryRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Entry entry = Entry.builder()
                .user(user)
                .content(request.getContent())
                .entryDate(request.getEntryDate())
                .moodScore(request.getMoodScore())
                .isPublic(Boolean.TRUE.equals(request.getIsPublic()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        entryRepository.save(entry);
        syncTags(entry, parseTagsFromContent(request.getContent()), userId);
        return toResponse(entry);
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
        entry.setUpdatedAt(LocalDateTime.now());

        entryTagRepository.deleteByEntryId(entryId);
        entryRepository.save(entry);
        syncTags(entry, parseTagsFromContent(request.getContent()), userId);
        return toResponse(entry);
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
        if (!entry.getUser().getId().equals(userId))
            throw new UnauthorizedException("Access denied");
        return toDetailResponse(entry);
    }

    @Transactional(readOnly = true)
    public List<EntryResponse> getEntries(UUID userId, LocalDate from, LocalDate to) {
        List<Entry> entries = (from != null && to != null)
                ? entryRepository.findByUserIdAndEntryDateBetweenOrderByEntryDateDesc(userId, from, to)
                : entryRepository.findByUserIdOrderByEntryDateDesc(userId);
        return entries.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public HeatmapResponse getHeatmapData(UUID userId, int year) {
        List<Object[]> rows = entryRepository.countEntriesByDateForYear(userId, year);
        Map<LocalDate, Integer> data = new LinkedHashMap<>();
        rows.forEach(r -> data.put((LocalDate) r[0], ((Long) r[1]).intValue()));
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
        entry.setUpdatedAt(LocalDateTime.now());
        return toResponse(entryRepository.save(entry));
    }

    private List<String> parseTagsFromContent(String content) {
        Pattern pattern = Pattern.compile("#(\\w+)");
        Matcher matcher = pattern.matcher(content);
        List<String> tags = new ArrayList<>();
        while (matcher.find()) tags.add(matcher.group(1).toLowerCase());
        return tags.stream().distinct().toList();
    }

    private void syncTags(Entry entry, List<String> tagNames, UUID userId) {
        User user = entry.getUser();
        for (String tagName : tagNames) {
            Tag tag = tagRepository.findByUserIdAndName(userId, tagName)
                    .orElseGet(() -> tagRepository.save(Tag.builder()
                            .user(user)
                            .name(tagName)
                            .color("#808080")
                            .build()));
            EntryTagId id = new EntryTagId(entry.getId(), tag.getId());
            entryTagRepository.save(new EntryTag(id, entry, tag));
        }
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
