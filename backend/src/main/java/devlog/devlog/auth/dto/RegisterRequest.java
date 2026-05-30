package devlog.devlog.auth.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank @Email
    String email;

    @NotBlank @Size(min = 8, max = 64)
    String password;

    @NotBlank @Size(min = 3, max = 32)
    @Pattern(regexp = "^[a-zA-Z0-9_]+$")
    String username;
}
