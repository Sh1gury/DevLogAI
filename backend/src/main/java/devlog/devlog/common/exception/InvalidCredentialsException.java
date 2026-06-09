package devlog.devlog.common.exception;

/**
 * Thrown on failed authentication (unknown email or wrong password).
 * Carries a generic message so callers cannot distinguish the two cases (prevents user enumeration).
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
