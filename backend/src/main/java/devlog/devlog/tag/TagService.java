package devlog.devlog.tag;

import devlog.devlog.common.exception.DuplicateResourceException;
import devlog.devlog.common.exception.ResourceNotFoundException;
import devlog.devlog.common.exception.UnauthorizedException;
import devlog.devlog.entry.Entry;
import devlog.devlog.tag.dto.StatsResponse;
import devlog.devlog.tag.dto.TagRequest;
import devlog.devlog.tag.dto.TagResponse;
import devlog.devlog.tag.dto.TagUsageCount;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final EntryTagRepository entryTagRepository;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<TagResponse> getTags(UUID userId) {
        return tagRepository.findTagsWithUsage(userId);
    }

    @Transactional
    public TagResponse createTag(UUID userId, TagRequest request) {
        if (tagRepository.existsByUserIdAndName(userId, request.getName()))
            throw new DuplicateResourceException("Tag already exists: " + request.getName());

        Tag tag = Tag.builder()
                .userId(userId)
                .name(request.getName())
                .color(request.getColor())
                .build();

        return toResponse(tagRepository.save(tag));
    }

    @Transactional
    public TagResponse updateTag(UUID userId, UUID tagId, TagRequest request) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found"));
        if (!tag.getUserId().equals(userId))
            throw new UnauthorizedException("Access denied");

        // Reject only if a *different* tag already uses the requested name.
        if (!tag.getName().equals(request.getName())
                && tagRepository.existsByUserIdAndName(userId, request.getName()))
            throw new DuplicateResourceException("Tag name already taken: " + request.getName());

        tag.setName(request.getName());
        tag.setColor(request.getColor());
        return toResponse(tagRepository.save(tag));
    }

    @Transactional
    public void deleteTag(UUID userId, UUID tagId) {
        if (!tagRepository.existsByIdAndUserId(tagId, userId))
            throw new UnauthorizedException("Access denied");
        tagRepository.deleteById(tagId);
    }

    /**
     * Tag-centric stats for a period. Entry-derived figures (avg mood, total entries) are
     * supplied by the caller so this service has no dependency on the entry module (avoids a cycle).
     */
    @Transactional(readOnly = true)
    public StatsResponse getTagStats(UUID userId, LocalDate from, LocalDate to,
                                     double avgMood, int totalEntries) {
        List<TagUsageCount> rows = entryTagRepository.countTagUsageForPeriod(userId, from, to);

        Map<String, Integer> tagCounts = new LinkedHashMap<>();
        int total = 0;
        for (TagUsageCount row : rows) {
            tagCounts.put(row.name(), (int) row.count());
            total += row.count();
        }

        final int totalFinal = total;
        List<StatsResponse.TagStatItem> topTags = rows.stream()
                .limit(10)
                .map(row -> new StatsResponse.TagStatItem(
                        row.name(),
                        (int) row.count(),
                        totalFinal == 0 ? 0 : row.count() * 100.0 / totalFinal
                ))
                .toList();

        return new StatsResponse(tagCounts, topTags, avgMood, totalEntries);
    }

    // ── Cross-module write API used by the entry module ─────────────────────────

    @Transactional
    public void syncTagsForEntry(UUID entryId, UUID userId, List<String> tagNames) {
        Entry entryRef = entityManager.getReference(Entry.class, entryId);
        for (String tagName : tagNames) {
            Tag tag = tagRepository.findByUserIdAndName(userId, tagName)
                    .orElseGet(() -> tagRepository.save(Tag.builder()
                            .userId(userId)
                            .name(tagName)
                            .color("#808080")
                            .build()));
            EntryTagId id = new EntryTagId(entryId, tag.getId());
            entryTagRepository.save(new EntryTag(id, entryRef, tag));
        }
    }

    @Transactional
    public void deleteTagsForEntry(UUID entryId) {
        entryTagRepository.deleteByEntryId(entryId);
    }

    @Transactional(readOnly = true)
    public Map<String, Integer> getTagUsageCounts(UUID userId, LocalDate from, LocalDate to) {
        Map<String, Integer> result = new LinkedHashMap<>();
        entryTagRepository.countTagUsageForPeriod(userId, from, to)
                .forEach(r -> result.put(r.name(), (int) r.count()));
        return result;
    }

    @Transactional(readOnly = true)
    public List<TagResponse> getTopTags(UUID userId, int limit) {
        return entryTagRepository.countTagUsageForPeriod(
                        userId, LocalDate.of(2000, 1, 1), LocalDate.now(ZoneOffset.UTC)).stream()
                .limit(limit)
                .map(r -> new TagResponse(null, r.name(), r.color(), r.count()))
                .toList();
    }

    private TagResponse toResponse(Tag tag) {
        long usageCount = entryTagRepository.countByTagId(tag.getId());
        return new TagResponse(tag.getId(), tag.getName(), tag.getColor(), usageCount);
    }
}
