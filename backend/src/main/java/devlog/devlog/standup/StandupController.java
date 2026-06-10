package devlog.devlog.standup;

import devlog.devlog.standup.dto.StandupResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/standups")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class StandupController {

    private final StandupService standupService;

    @GetMapping
    public List<StandupResponse> getStandups(@AuthenticationPrincipal UUID userId) {
        return standupService.getStandups(userId);
    }

    @GetMapping("/today")
    public ResponseEntity<StandupResponse> getTodayStandup(@AuthenticationPrincipal UUID userId) {
        return standupService.getTodayStandup(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/{id}")
    public StandupResponse getStandup(@AuthenticationPrincipal UUID userId,
                                      @PathVariable UUID id) {
        return standupService.getStandup(userId, id);
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public StandupResponse generateStandup(@AuthenticationPrincipal UUID userId,
                                           @RequestParam(required = false) LocalDate date) {
        return standupService.generateStandup(userId, date);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStandup(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        standupService.deleteStandup(userId, id);
    }
}
