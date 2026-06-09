package devlog.devlog.digest;

import devlog.devlog.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DigestScheduler {

    private final DigestService digestService;
    private final UserRepository userRepository;

    // Runs every Sunday at 23:00 UTC — generates digest for the week that just ended
    @Scheduled(cron = "0 0 23 * * SUN")
    public void generateWeeklyDigests() {
        log.info("DigestScheduler: generating weekly digests via Llama 3.3 70B (Groq)");
        userRepository.findAll().forEach(user -> {
            try {
                digestService.generateDigest(user.getId(), digestService.getCurrentWeekStart());
            } catch (Exception e) {
                log.error("Failed to generate digest for user {}: {}", user.getId(), e.getMessage());
            }
        });
    }
}
