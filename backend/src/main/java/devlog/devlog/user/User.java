package devlog.devlog.user;

import devlog.devlog.digest.Digest;
import devlog.devlog.entry.Entry;
import devlog.devlog.standup.Standup;
import devlog.devlog.tag.Tag;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(unique = true, nullable = false)
    String email;

    @Column(nullable = false)
    String passwordHash;

    @Column(unique = true, nullable = false)
    String username;

    @Column(nullable = false)
    LocalDateTime createdAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    List<Entry> entries;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    List<Tag> tags;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    List<Digest> digests;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    List<Standup> standups;
}