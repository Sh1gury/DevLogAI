package devlog.devlog.digest;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DigestRepository extends JpaRepository<Digest, UUID> {

    List<Digest> findByUserIdOrderByWeekStartDesc(UUID userId);

    Optional<Digest> findByUserIdAndWeekStart(UUID userId, LocalDate weekStart);

    Optional<Digest> findFirstByUserIdOrderByWeekStartDesc(UUID userId);

    boolean existsByIdAndUserId(UUID digestId, UUID userId);
}
