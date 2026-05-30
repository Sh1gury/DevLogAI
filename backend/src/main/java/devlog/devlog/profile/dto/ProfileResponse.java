package devlog.devlog.profile.dto;

import devlog.devlog.digest.dto.DigestResponse;
import devlog.devlog.entry.dto.EntryResponse;
import devlog.devlog.tag.dto.TagResponse;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ProfileResponse {
    String username;
    LocalDateTime memberSince;
    int totalEntries;
    long activeDays;
    int currentStreak;
    int longestStreak;
    List<TagResponse> topTags;
    DigestResponse latestDigest;
    List<EntryResponse> recentPublicEntries;
    Double avgMood;
}
