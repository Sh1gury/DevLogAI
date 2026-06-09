package devlog.devlog.digest;

import devlog.devlog.ai.GroqClient;
import devlog.devlog.common.exception.ResourceNotFoundException;
import devlog.devlog.common.exception.UnauthorizedException;
import devlog.devlog.digest.dto.DigestResponse;
import devlog.devlog.entry.EntryService;
import devlog.devlog.entry.dto.EntryForAiDto;
import devlog.devlog.tag.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DigestService {

    private final DigestRepository digestRepository;
    private final EntryService entryService;
    private final TagService tagService;
    private final GroqClient groqClient;

    @Transactional(readOnly = true)
    public List<DigestResponse> getDigests(UUID userId) {
        return digestRepository.findByUserIdOrderByWeekStartDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public DigestResponse getDigest(UUID userId, UUID digestId) {
        Digest digest = digestRepository.findById(digestId)
                .orElseThrow(() -> new ResourceNotFoundException("Digest not found"));
        if (!digest.getUserId().equals(userId))
            throw new UnauthorizedException("Access denied");
        return toResponse(digest);
    }

    @Transactional(readOnly = true)
    public Optional<DigestResponse> getLatestDigest(UUID userId) {
        return digestRepository.findFirstByUserIdOrderByWeekStartDesc(userId)
                .map(this::toResponse);
    }

    @Transactional
    public DigestResponse generateDigest(UUID userId, LocalDate weekStart) {
        LocalDate start = weekStart != null ? weekStart : getCurrentWeekStart();
        LocalDate end = start.plusDays(6);

        Optional<Digest> existing = digestRepository.findByUserIdAndWeekStart(userId, start);
        if (existing.isPresent()) return toResponse(existing.get());

        List<EntryForAiDto> entries = entryService.getEntriesForPeriod(userId, start, end);
        Map<String, Integer> tagCounts = tagService.getTagUsageCounts(userId, start, end);

        double avgMood = entries.stream()
                .filter(e -> e.moodScore() != null)
                .mapToInt(EntryForAiDto::moodScore)
                .average().orElse(0);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("tag_counts", tagCounts);
        stats.put("avg_mood", avgMood);
        stats.put("entry_count", entries.size());

        String aiSummary = groqClient.complete(buildDigestPrompt(start, end, entries, tagCounts, avgMood));

        Digest digest = Digest.builder()
                .userId(userId)
                .weekStart(start)
                .aiSummary(aiSummary)
                .stats(stats)
                .generatedAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();

        return toResponse(digestRepository.save(digest));
    }

    @Transactional
    public void deleteDigest(UUID userId, UUID digestId) {
        if (!digestRepository.existsByIdAndUserId(digestId, userId))
            throw new UnauthorizedException("Access denied");
        digestRepository.deleteById(digestId);
    }

    public LocalDate getCurrentWeekStart() {
        return LocalDate.now(ZoneOffset.UTC).with(DayOfWeek.MONDAY);
    }

    public DigestResponse toResponse(Digest digest) {
        return DigestResponse.builder()
                .id(digest.getId())
                .weekStart(digest.getWeekStart())
                .weekEnd(digest.getWeekStart().plusDays(6))
                .aiSummary(digest.getAiSummary())
                .stats(digest.getStats())
                .generatedAt(digest.getGeneratedAt())
                .build();
    }

    private String buildDigestPrompt(LocalDate start, LocalDate end, List<EntryForAiDto> entries,
                                     Map<String, Integer> tagCounts, double avgMood) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a developer productivity assistant. ");
        sb.append("Write a concise weekly digest based on the journal entries below.\n\n");
        sb.append("Period: ").append(start).append(" to ").append(end).append("\n");
        sb.append("Total entries: ").append(entries.size()).append("\n");
        sb.append("Average mood (scale 1-5): ").append(String.format("%.1f", avgMood)).append("\n");

        if (!tagCounts.isEmpty()) {
            String topTags = tagCounts.entrySet().stream().limit(5)
                    .map(e -> e.getKey() + " (" + e.getValue() + "x)")
                    .collect(Collectors.joining(", "));
            sb.append("Top tags: ").append(topTags).append("\n");
        }

        sb.append("\nJournal entries:\n");
        entries.stream().limit(15).forEach(e -> {
            sb.append("---\n");
            sb.append("Date: ").append(e.entryDate()).append("\n");
            if (e.moodScore() != null) sb.append("Mood: ").append(e.moodScore()).append("/5\n");
            sb.append(e.content()).append("\n");
        });

        sb.append("\nTask: Write 2-3 paragraphs summarizing the main topics worked on this week, ");
        sb.append("productivity patterns, and mood trends. ");
        sb.append("Use past tense. Be professional and encouraging. Do not use bullet lists.");
        return sb.toString();
    }
}
