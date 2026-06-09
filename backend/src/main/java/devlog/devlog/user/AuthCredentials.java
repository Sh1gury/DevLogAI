package devlog.devlog.user;

import java.util.UUID;

/**
 * Auth-specific DTO exposed by the user module so that the auth module can verify
 * credentials without the {@link User} entity crossing the module boundary.
 */
public record AuthCredentials(UUID id, String username, String email, String passwordHash) {}
