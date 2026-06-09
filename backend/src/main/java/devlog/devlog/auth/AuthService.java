package devlog.devlog.auth;

import devlog.devlog.auth.dto.AuthResponse;
import devlog.devlog.auth.dto.LoginRequest;
import devlog.devlog.auth.dto.RegisterRequest;
import devlog.devlog.common.exception.DuplicateResourceException;
import devlog.devlog.common.exception.InvalidCredentialsException;
import devlog.devlog.user.AuthCredentials;
import devlog.devlog.user.UserResponse;
import devlog.devlog.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userService.existsByEmail(request.getEmail()))
            throw new DuplicateResourceException("Email already in use");
        if (userService.existsByUsername(request.getUsername()))
            throw new DuplicateResourceException("Username already taken");

        UserResponse user = userService.createUser(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getUsername()
        );

        String token = jwtTokenProvider.generateToken(user.id().toString());
        return new AuthResponse(token, user.id(), user.username(), user.email());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        AuthCredentials credentials = userService.findCredentialsByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), credentials.passwordHash()))
            throw new InvalidCredentialsException("Invalid email or password");

        String token = jwtTokenProvider.generateToken(credentials.id().toString());
        return new AuthResponse(token, credentials.id(), credentials.username(), credentials.email());
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        return userService.getUserById(userId);
    }
}
