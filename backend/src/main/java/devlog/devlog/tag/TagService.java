package devlog.devlog.tag;

import devlog.devlog.common.exception.DuplicateResourceException;
import devlog.devlog.common.exception.ResourceNotFoundException;
import devlog.devlog.common.exception.UnauthorizedException;
import devlog.devlog.entry.EntryRepository;
import devlog.devlog.tag.dto.StatsResponse;
import devlog.devlog.tag.dto.TagRequest;
import devlog.devlog.tag.dto.TagResponse;
import devlog.devlog.user.User;
import devlog.devlog.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final EntryTagRepository entryTagRepository;
    private final UserRepository userRepository;
    private final EntryRepository entryRepository;

    @Transactional(readOnly = true)
    public List<TagResponse> getTags(UUID userId) {
        return tagRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TagResponse createTag(UUID userId, TagRequest request) {
        if (tagRepository.existsByUserIdAndName(userId, request.getName()))
            throw new DuplicateResourceException("Tag already exists: " + request.getName());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Tag tag = Tag.builder()
                .user(user)
                .name(request.getName())
                .color(request.getColor())
                .build();

        return toResponse(tagRepository.save(tag));
    }

    @Transactional
    public TagResponse updateTag(UUID userId, UUID tagId, TagRequest request) {
        if (!tagRepository.existsByIdAndUserId(tagId, userId))
            throw new UnauthorizedException("Access denied");

        if (tagRepository.existsByUserIdAndName(userId, request.getName()))
            throw new DuplicateResourceException("Tag name already taken: " + request.getName());

        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found"));

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

    @Transactional(readOnly = true)
    public StatsResponse getTagStats(UUID userId, LocalDate from, LocalDate to) {
        LocalDate effectiveFrom = from != null ? from : LocalDate.of(2000, 1, 1);
        LocalDate effectiveTo = to != null ? to : LocalDate.now();

        List<Object[]> rows = entryTagRepository.countTagUsageForPeriod(userId, effectiveFrom, effectiveTo);

        Map<String, Integer> tagCounts = new LinkedHashMap<>();
        int total = 0;
        for (Object[] row : rows) {
            int count = ((Long) row[2]).intValue();
            tagCounts.put((String) row[0], count);
            total += count;
        }

        final int totalFinal = total;
        List<StatsResponse.TagStatItem> topTags = rows.stream()
                .limit(10)
                .map(row -> new StatsResponse.TagStatItem(
                        (String) row[0],
                        ((Long) row[2]).intValue(),
                        totalFinal == 0 ? 0 : ((Long) row[2]).intValue() * 100.0 / totalFinal
                ))
                .toList();

        Double avgMood = entryRepository
                .findByUserIdAndEntryDateBetweenOrderByEntryDateDesc(userId, effectiveFrom, effectiveTo)
                .stream()
                .filter(e -> e.getMoodScore() != null)
                .mapToInt(e -> e.getMoodScore())
                .average()
                .orElse(0);

        int totalEntries = entryRepository
                .findByUserIdAndEntryDateBetweenOrderByEntryDateDesc(userId, effectiveFrom, effectiveTo)
                .size();

        return new StatsResponse(tagCounts, topTags, avgMood, totalEntries);
    }

    private TagResponse toResponse(Tag tag) {
        long usageCount = entryTagRepository.countByTagId(tag.getId());
        return new TagResponse(tag.getId(), tag.getName(), tag.getColor(), usageCount);
    }
}
