package devlog.devlog.standup;

import devlog.devlog.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "standups")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Standup {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(nullable = false)
    LocalDate standupDate;

    @Column(columnDefinition = "TEXT")
    String yesterday;

    @Column(columnDefinition = "TEXT")
    String today;

    @Column(columnDefinition = "TEXT")
    String blockers;

    @Column(nullable = false)
    LocalDateTime generatedAt;
}