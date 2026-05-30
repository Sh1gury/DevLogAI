package devlog.devlog.standup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StandupRepository extends JpaRepository<Standup, UUID> {

    List<Standup> findByUserIdOrderByStandupDateDesc(UUID userId);

    Optional<Standup> findByUserIdAndStandupDate(UUID userId, LocalDate date);

    boolean existsByIdAndUserId(UUID standupId, UUID userId);
}
