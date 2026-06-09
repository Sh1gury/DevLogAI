package devlog.devlog.standup;

import devlog.devlog.ai.GroqClient;
import devlog.devlog.common.exception.ResourceNotFoundException;
import devlog.devlog.common.exception.UnauthorizedException;
import devlog.devlog.entry.EntryService;
import devlog.devlog.entry.dto.EntryForAiDto;
import devlog.devlog.standup.dto.StandupResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StandupService {

    private final StandupRepository standupRepository;
    private final EntryService entryService;
    private final GroqClient groqClient;

    @Transactional(readOnly = true)
    public List<StandupResponse> getStandups(UUID userId) {
        return standupRepository.findByUserIdOrderByStandupDateDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public StandupResponse getStandup(UUID userId, UUID standupId) {
        Standup standup = standupRepository.findById(standupId)
                .orElseThrow(() -> new ResourceNotFoundException("Standup not found"));
        if (!standup.getUserId().equals(userId))
            throw new UnauthorizedException("Access denied");
        return toResponse(standup);
    }

    @Transactional(readOnly = true)
    public Optional<StandupResponse> getTodayStandup(UUID userId) {
        return standupRepository.findByUserIdAndStandupDate(userId, LocalDate.now(ZoneOffset.UTC))
                .map(this::toResponse);
    }

    @Transactional
    public StandupResponse generateStandup(UUID userId, LocalDate date) {
        LocalDate standupDate = date != null ? date : LocalDate.now(ZoneOffset.UTC);
        LocalDate yesterday = standupDate.minusDays(1);

        standupRepository.findByUserIdAndStandupDate(userId, standupDate)
                .ifPresent(standupRepository::delete);

        // Single query covering yesterday+today; split in memory to avoid redundant round-trips.
        List<EntryForAiDto> recentEntries = entryService.getEntriesForPeriod(userId, yesterday, standupDate);
        List<EntryForAiDto> yesterdayEntries = recentEntries.stream()
                .filter(e -> yesterday.equals(e.entryDate()))
                .toList();
        List<EntryForAiDto> todayEntries = recentEntries.stream()
                .filter(e -> standupDate.equals(e.entryDate()))
                .toList();

        List<EntryForAiDto> blockerEntries = recentEntries.stream()
                .filter(e -> e.tagNames().contains("blocker"))
                .toList();

        String rawYesterday = yesterdayEntries.isEmpty() ? "No entries yesterday"
                : yesterdayEntries.stream().map(EntryForAiDto::content).collect(Collectors.joining("\n\n"));
        String rawToday = todayEntries.isEmpty() ? "No entries today"
                : todayEntries.stream().map(EntryForAiDto::content).collect(Collectors.joining("\n\n"));
        String rawBlockers = blockerEntries.isEmpty() ? "No blockers"
                : blockerEntries.stream().map(EntryForAiDto::content).collect(Collectors.joining("\n\n"));

        String aiResponse = groqClient.complete(buildStandupPrompt(rawYesterday, rawToday, rawBlockers, standupDate));
        StandupSections sections = parseSections(aiResponse, rawYesterday, rawToday, rawBlockers);

        Standup standup = Standup.builder()
                .userId(userId)
                .standupDate(standupDate)
                .yesterday(sections.yesterday())
                .today(sections.today())
                .blockers(sections.blockers())
                .generatedAt(LocalDateTime.now(ZoneOffset.UTC))
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

    private String buildStandupPrompt(String yesterday, String today, String blockers, LocalDate date) {
        return "You are a developer assistant. Format this daily standup for " + date + ".\n\n"
                + "Yesterday's work:\n" + yesterday + "\n\n"
                + "Today's plan:\n" + today + "\n\n"
                + "Potential blockers:\n" + blockers + "\n\n"
                + "Return ONLY the formatted standup with these exact section markers, nothing else:\n"
                + "[YESTERDAY]\n"
                + "formatted yesterday content here\n"
                + "[TODAY]\n"
                + "formatted today content here\n"
                + "[BLOCKERS]\n"
                + "formatted blockers content here\n\n"
                + "Rules: each section is 1-3 bullet points starting with '- '. Be brief and professional.";
    }

    private record StandupSections(String yesterday, String today, String blockers) {}

    private StandupSections parseSections(String aiResponse, String fallbackY, String fallbackT, String fallbackB) {
        String y = extractBetween(aiResponse, "[YESTERDAY]", "[TODAY]");
        String t = extractBetween(aiResponse, "[TODAY]", "[BLOCKERS]");
        String b = extractBetween(aiResponse, "[BLOCKERS]", null);
        return new StandupSections(
                y.isBlank() ? fallbackY : y,
                t.isBlank() ? fallbackT : t,
                b.isBlank() ? fallbackB : b
        );
    }

    private String extractBetween(String text, String startMarker, String endMarker) {
        int startIdx = text.indexOf(startMarker);
        if (startIdx == -1) return "";
        startIdx += startMarker.length();
        int endIdx = endMarker != null ? text.indexOf(endMarker, startIdx) : text.length();
        if (endIdx == -1) endIdx = text.length();
        return text.substring(startIdx, endIdx).trim();
    }
}
