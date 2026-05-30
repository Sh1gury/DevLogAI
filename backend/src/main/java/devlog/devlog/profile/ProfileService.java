package devlog.devlog.profile;

import devlog.devlog.common.exception.ResourceNotFoundException;
import devlog.devlog.digest.DigestService;
import devlog.devlog.digest.dto.DigestResponse;
import devlog.devlog.entry.Entry;
import devlog.devlog.entry.EntryRepository;
import devlog.devlog.entry.dto.EntryResponse;
import devlog.devlog.profile.dto.ProfileResponse;
import devlog.devlog.tag.EntryTagRepository;
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
public class ProfileService {

    private final UserRepository userRepository;
    private final EntryRepository entryRepository;
    private final EntryTagRepository entryTagRepository;
    private final DigestService digestService;

    @Transactional(readOnly = true)
    public ProfileResponse getPublicProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        List<Entry> publicEntries = entryRepository.findPublicEntriesByUserId(user.getId());
        return buildProfile(user, publicEntries);
    }

    @Transactional(readOnly = true)
    public ProfileResponse getMyProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Entry> allEntries = entryRepository.findByUserIdOrderByEntryDateDesc(userId);
        return buildProfile(user, allEntries);
    }

    private ProfileResponse buildProfile(User user, List<Entry> entries) {
        UUID userId = user.getId();

        List<LocalDate> allDates = entryRepository.findAllEntryDatesByUserId(userId);
        long activeDays = entryRepository.countActiveDays(userId);

        List<Object[]> tagRows = entryTagRepository.countTagUsageForPeriod(
                userId, LocalDate.of(2000, 1, 1), LocalDate.now());
        List<TagResponse> topTags = tagRows.stream()
                .limit(5)
                .map(r -> new TagResponse(null, (String) r[0], (String) r[1], ((Long) r[2])))
                .toList();

        DigestResponse latestDigest = digestService.getLatestDigest(userId).orElse(null);

        List<EntryResponse> recentPublic = entries.stream()
                .filter(Entry::isPublic)
                .limit(10)
                .map(e -> {
                    String preview = e.getContent().length() > 200
                            ? e.getContent().substring(0, 200) : e.getContent();
                    return devlog.devlog.entry.dto.EntryResponse.builder()
                            .id(e.getId()).contentPreview(preview)
                            .entryDate(e.getEntryDate()).moodScore(e.getMoodScore())
                            .isPublic(e.isPublic()).tags(List.of())
                            .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt())
                            .build();
                })
                .toList();

        double avgMood = entries.stream()
                .filter(e -> e.getMoodScore() != null)
                .mapToInt(Entry::getMoodScore)
                .average().orElse(0);

        return ProfileResponse.builder()
                .username(user.getUsername())
                .memberSince(user.getCreatedAt())
                .totalEntries(entries.size())
                .activeDays(activeDays)
                .currentStreak(calculateCurrentStreak(allDates))
                .longestStreak(calculateLongestStreak(allDates))
                .topTags(topTags)
                .latestDigest(latestDigest)
                .recentPublicEntries(recentPublic)
                .avgMood(avgMood)
                .build();
    }

    private int calculateCurrentStreak(List<LocalDate> dates) {
        if (dates.isEmpty() || !dates.get(0).equals(LocalDate.now())) return 0;
        int streak = 1;
        for (int i = 1; i < dates.size(); i++) {
            if (dates.get(i - 1).minusDays(1).equals(dates.get(i))) streak++;
            else break;
        }
        return streak;
    }

    private int calculateLongestStreak(List<LocalDate> dates) {
        if (dates.isEmpty()) return 0;
        List<LocalDate> sorted = new ArrayList<>(dates);
        Collections.sort(sorted);
        int longest = 1, current = 1;
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i - 1).plusDays(1).equals(sorted.get(i))) {
                current++;
                if (current > longest) longest = current;
            } else {
                current = 1;
            }
        }
        return longest;
    }
}
