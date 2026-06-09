package devlog.devlog.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    @ToString.Include
    UUID id;

    @Column(unique = true, nullable = false)
    @ToString.Include
    String email;

    @Column(nullable = false)
    String passwordHash;

    @Column(unique = true, nullable = false)
    @ToString.Include
    String username;

    @Column(nullable = false)
    LocalDateTime createdAt;
}
