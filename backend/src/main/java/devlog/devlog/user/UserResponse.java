package devlog.devlog.user;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(UUID id, String username, String email, LocalDateTime createdAt) {}
