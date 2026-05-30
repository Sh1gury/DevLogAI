package devlog.devlog.digest;

import devlog.devlog.digest.dto.DigestResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/digests")
@RequiredArgsConstructor
public class DigestController {

    private final DigestService digestService;

    @GetMapping
    public List<DigestResponse> getDigests(@AuthenticationPrincipal UUID userId) {
        return digestService.getDigests(userId);
    }

    @GetMapping("/latest")
    public ResponseEntity<DigestResponse> getLatestDigest(@AuthenticationPrincipal UUID userId) {
        return digestService.getLatestDigest(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/{id}")
    public DigestResponse getDigest(@AuthenticationPrincipal UUID userId,
                                    @PathVariable UUID id) {
        return digestService.getDigest(userId, id);
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public DigestResponse generateDigest(@AuthenticationPrincipal UUID userId,
                                         @RequestParam(required = false) LocalDate weekStart) {
        return digestService.generateDigest(userId, weekStart);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDigest(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        digestService.deleteDigest(userId, id);
    }
}
