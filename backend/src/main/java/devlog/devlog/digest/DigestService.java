package devlog.devlog.digest;

import devlog.devlog.common.exception.ResourceNotFoundException;
import devlog.devlog.common.exception.UnauthorizedException;
import devlog.devlog.digest.dto.DigestResponse;
import devlog.devlog.entry.Entry;
import devlog.devlog.entry.EntryRepository;
import devlog.devlog.tag.EntryTagRepository;
import devlog.devlog.user.User;
import devlog.devlog.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DigestService {

    private final DigestRepository digestRepository;
    private final EntryRepository entryRepository;
    private final EntryTagRepository entryTagRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<DigestResponse> getDigests(UUID userId) {
        return digestRepository.findByUserIdOrderByWeekStartDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public DigestResponse getDigest(UUID userId, UUID digestId) {
        Digest digest = digestRepository.findById(digestId)
                .orElseThrow(() -> new ResourceNotFoundException("Digest not found"));
        if (!digest.getUser().getId().equals(userId))
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

        List<Entry> entries = entryRepository.findEntriesWithTagsBetween(userId, start, end);

        List<Object[]> tagRows = entryTagRepository.countTagUsageForPeriod(userId, start, end);
        Map<String, Integer> tagCounts = new LinkedHashMap<>();
        tagRows.forEach(r -> tagCounts.put((String) r[0], ((Long) r[2]).intValue()));

        double avgMood = entries.stream()
                .filter(e -> e.getMoodScore() != null)
                .mapToInt(Entry::getMoodScore)
                .average().orElse(0);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("tag_counts", tagCounts);
        stats.put("avg_mood", avgMood);
        stats.put("entry_count", entries.size());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Digest digest = Digest.builder()
                .user(user)
                .weekStart(start)
                .aiSummary("AI summary will be available soon")
                .stats(stats)
                .generatedAt(LocalDateTime.now())
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
        return LocalDate.now().with(DayOfWeek.MONDAY);
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
}
