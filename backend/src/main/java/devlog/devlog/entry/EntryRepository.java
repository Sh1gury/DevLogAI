package devlog.devlog.entry;

import devlog.devlog.entry.dto.EntryDateCount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EntryRepository extends JpaRepository<Entry, UUID> {

    Page<Entry> findByUserIdOrderByEntryDateDesc(UUID userId, Pageable pageable);

    Page<Entry> findByUserIdAndEntryDateBetweenOrderByEntryDateDesc(
            UUID userId, LocalDate from, LocalDate to, Pageable pageable);

    List<Entry> findByUserIdOrderByEntryDateDesc(UUID userId);

    @Query("SELECT e FROM Entry e WHERE e.userId = :userId AND e.isPublic = true ORDER BY e.entryDate DESC")
    List<Entry> findPublicEntriesByUserId(@Param("userId") UUID userId);

    boolean existsByIdAndUserId(UUID entryId, UUID userId);

    @Query("SELECT DISTINCT e FROM Entry e " +
           "LEFT JOIN FETCH e.entryTags et " +
           "LEFT JOIN FETCH et.tag " +
           "WHERE e.id = :id")
    Optional<Entry> findByIdWithTags(@Param("id") UUID id);

    @Query("SELECT new devlog.devlog.entry.dto.EntryDateCount(e.entryDate, COUNT(e)) FROM Entry e " +
           "WHERE e.userId = :userId AND year(e.entryDate) = :year " +
           "GROUP BY e.entryDate")
    List<EntryDateCount> countEntriesByDateForYear(@Param("userId") UUID userId, @Param("year") int year);

    @Query("SELECT DISTINCT e FROM Entry e " +
           "LEFT JOIN FETCH e.entryTags et " +
           "LEFT JOIN FETCH et.tag " +
           "WHERE e.userId = :userId " +
           "AND e.entryDate BETWEEN :from AND :to " +
           "ORDER BY e.entryDate DESC")
    List<Entry> findEntriesWithTagsBetween(@Param("userId") UUID userId,
                                           @Param("from") LocalDate from,
                                           @Param("to") LocalDate to);

    @Query("SELECT COUNT(DISTINCT e.entryDate) FROM Entry e WHERE e.userId = :userId")
    long countActiveDays(@Param("userId") UUID userId);

    @Query("SELECT DISTINCT e.entryDate FROM Entry e " +
           "WHERE e.userId = :userId ORDER BY e.entryDate DESC")
    List<LocalDate> findAllEntryDatesByUserId(@Param("userId") UUID userId);

    @Query("SELECT AVG(e.moodScore) FROM Entry e " +
           "WHERE e.userId = :userId AND e.entryDate BETWEEN :from AND :to")
    Double averageMoodForPeriod(@Param("userId") UUID userId,
                                @Param("from") LocalDate from,
                                @Param("to") LocalDate to);

    long countByUserIdAndEntryDateBetween(UUID userId, LocalDate from, LocalDate to);
}
