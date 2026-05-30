package devlog.devlog.profile;

import devlog.devlog.profile.dto.ProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/me")
    public ProfileResponse getMyProfile(@AuthenticationPrincipal UUID userId) {
        return profileService.getMyProfile(userId);
    }

    @GetMapping("/{username}")
    public ProfileResponse getPublicProfile(@PathVariable String username) {
        return profileService.getPublicProfile(username);
    }
}
