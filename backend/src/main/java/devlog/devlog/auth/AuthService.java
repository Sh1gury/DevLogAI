package devlog.devlog.auth;

import devlog.devlog.auth.dto.AuthResponse;
import devlog.devlog.auth.dto.LoginRequest;
import devlog.devlog.auth.dto.RegisterRequest;
import devlog.devlog.common.exception.DuplicateResourceException;
import devlog.devlog.common.exception.ResourceNotFoundException;
import devlog.devlog.common.exception.UnauthorizedException;
import devlog.devlog.user.User;
import devlog.devlog.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail()))
            throw new DuplicateResourceException("Email already in use");
        if (userRepository.existsByUsername(request.getUsername()))
            throw new DuplicateResourceException("Username already taken");

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .username(request.getUsername())
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);
        String token = jwtTokenProvider.generateToken(user.getId().toString());
        return new AuthResponse(token, user.getId(), user.getUsername(), user.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash()))
            throw new UnauthorizedException("Invalid password");

        String token = jwtTokenProvider.generateToken(user.getId().toString());
        return new AuthResponse(token, user.getId(), user.getUsername(), user.getEmail());
    }

    public User getCurrentUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
