package devlog.devlog.standup;

import devlog.devlog.common.exception.ResourceNotFoundException;
import devlog.devlog.common.exception.UnauthorizedException;
import devlog.devlog.entry.Entry;
import devlog.devlog.entry.EntryRepository;
import devlog.devlog.standup.dto.StandupResponse;
import devlog.devlog.user.User;
import devlog.devlog.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StandupService {

    private final StandupRepository standupRepository;
    private final EntryRepository entryRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<StandupResponse> getStandups(UUID userId) {
        return standupRepository.findByUserIdOrderByStandupDateDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public StandupResponse getStandup(UUID userId, UUID standupId) {
        Standup standup = standupRepository.findById(standupId)
                .orElseThrow(() -> new ResourceNotFoundException("Standup not found"));
        if (!standup.getUser().getId().equals(userId))
            throw new UnauthorizedException("Access denied");
        return toResponse(standup);
    }

    @Transactional(readOnly = true)
    public Optional<StandupResponse> getTodayStandup(UUID userId) {
        return standupRepository.findByUserIdAndStandupDate(userId, LocalDate.now())
                .map(this::toResponse);
    }

    @Transactional
    public StandupResponse generateStandup(UUID userId, LocalDate date) {
        LocalDate standupDate = date != null ? date : LocalDate.now();

        standupRepository.findByUserIdAndStandupDate(userId, standupDate)
                .ifPresent(standupRepository::delete);

        List<Entry> yesterdayEntries = entryRepository
                .findEntriesWithTagsBetween(userId, standupDate.minusDays(1), standupDate.minusDays(1));
        List<Entry> todayEntries = entryRepository
                .findEntriesWithTagsBetween(userId, standupDate, standupDate);
        List<Entry> recentEntries = entryRepository
                .findEntriesWithTagsBetween(userId, standupDate.minusDays(1), standupDate);

        List<Entry> blockerEntries = recentEntries.stream()
                .filter(e -> e.getEntryTags() != null && e.getEntryTags().stream()
                        .anyMatch(et -> "blocker".equalsIgnoreCase(et.getTag().getName())))
                .toList();

        String yesterday = yesterdayEntries.isEmpty() ? "No entries yesterday"
                : yesterdayEntries.stream().map(Entry::getContent).collect(Collectors.joining("\n\n"));
        String today = todayEntries.isEmpty() ? "No entries today"
                : todayEntries.stream().map(Entry::getContent).collect(Collectors.joining("\n\n"));
        String blockers = blockerEntries.isEmpty() ? "No blockers"
                : blockerEntries.stream().map(Entry::getContent).collect(Collectors.joining("\n\n"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Standup standup = Standup.builder()
                .user(user)
                .standupDate(standupDate)
                .yesterday(yesterday)
                .today(today)
                .blockers(blockers)
                .generatedAt(LocalDateTime.now())
                .build();

        return toResponse(standupRepository.save(standup));
    }

    @Transactional
    public void deleteStandup(UUID userId, UUID standupId) {
        if (!standupRepository.existsByIdAndUserId(standupId, userId))
            throw new UnauthorizedException("Access denied");
        standupRepository.deleteById(standupId);
    }

    private StandupResponse toResponse(Standup s) {
        return new StandupResponse(s.getId(), s.getStandupDate(),
                s.getYesterday(), s.getToday(), s.getBlockers(), s.getGeneratedAt());
    }
}
