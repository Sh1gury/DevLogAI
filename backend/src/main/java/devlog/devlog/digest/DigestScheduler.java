package devlog.devlog.digest;

import devlog.devlog.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DigestScheduler {

    private final DigestService digestService;
    private final UserRepository userRepository;

    public void generateWeeklyDigests() {
        log.info("DigestScheduler: заглушка, AI ще не підключено");
        // TODO: після AI-інтеграції розкоментувати:
        // userRepository.findAll().forEach(user ->
        //     digestService.generateDigest(user.getId(), digestService.getCurrentWeekStart()));
    }
}
