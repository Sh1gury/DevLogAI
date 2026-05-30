package devlog.devlog.tag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TagRequest {

    @NotBlank @Size(max = 32)
    String name;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$")
    String color;
}
