package devlog.devlog.user;

import devlog.devlog.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // ── Reads (cross-module DTO) ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        return toResponse(userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found")));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        return toResponse(userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username)));
    }

    @Transactional(readOnly = true)
    public List<UUID> getAllUserIds() {
        return userRepository.findAll().stream().map(User::getId).toList();
    }

    // ── Auth support (credentials never expose the entity) ──────────────────────

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Transactional
    public UserResponse createUser(String email, String passwordHash, String username) {
        User user = User.builder()
                .email(email)
                .passwordHash(passwordHash)
                .username(username)
                .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();
        return toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public Optional<AuthCredentials> findCredentialsByEmail(String email) {
        return userRepository.findByEmail(email).map(this::toCredentials);
    }

    @Transactional(readOnly = true)
    public Optional<AuthCredentials> findCredentialsById(UUID userId) {
        return userRepository.findById(userId).map(this::toCredentials);
    }

    // ── Mapping ─────────────────────────────────────────────────────────────────

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getCreatedAt());
    }

    private AuthCredentials toCredentials(User user) {
        return new AuthCredentials(user.getId(), user.getUsername(), user.getEmail(), user.getPasswordHash());
    }
}
