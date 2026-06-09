package devlog.devlog.profile;

import devlog.devlog.digest.DigestService;
import devlog.devlog.digest.dto.DigestResponse;
import devlog.devlog.entry.EntryService;
import devlog.devlog.entry.dto.EntryResponse;
import devlog.devlog.profile.dto.ProfileResponse;
import devlog.devlog.tag.TagService;
import devlog.devlog.tag.dto.TagResponse;
import devlog.devlog.user.UserResponse;
import devlog.devlog.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserService userService;
    private final EntryService entryService;
    private final TagService tagService;
    private final DigestService digestService;

    @Transactional(readOnly = true)
    public ProfileResponse getPublicProfile(String username) {
        UserResponse user = userService.getUserByUsername(username);
        List<EntryResponse> publicEntries = entryService.getPublicEntries(user.id());
        return buildProfile(user, publicEntries);
    }

    @Transactional(readOnly = true)
    public ProfileResponse getMyProfile(UUID userId) {
        UserResponse user = userService.getUserById(userId);
        List<EntryResponse> allEntries = entryService.getEntries(userId, null, null);
        return buildProfile(user, allEntries);
    }

    private ProfileResponse buildProfile(UserResponse user, List<EntryResponse> entries) {
        UUID userId = user.id();

        List<LocalDate> allDates = entryService.getAllEntryDates(userId);
        long activeDays = entryService.countActiveDays(userId);
        List<TagResponse> topTags = tagService.getTopTags(userId, 5);
        DigestResponse latestDigest = digestService.getLatestDigest(userId).orElse(null);

        List<EntryResponse> recentPublic = entries.stream()
                .filter(e -> Boolean.TRUE.equals(e.getIsPublic()))
                .limit(10)
                .toList();

        double avgMood = entries.stream()
                .filter(e -> e.getMoodScore() != null)
                .mapToInt(EntryResponse::getMoodScore)
                .average().orElse(0);

        return ProfileResponse.builder()
                .username(user.username())
                .memberSince(user.createdAt())
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
        if (dates.isEmpty() || !dates.get(0).equals(LocalDate.now(ZoneOffset.UTC))) return 0;
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
