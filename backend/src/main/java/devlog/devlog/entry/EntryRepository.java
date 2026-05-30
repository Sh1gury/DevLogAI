package devlog.devlog.entry;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface EntryRepository extends JpaRepository<Entry, UUID> {

    List<Entry> findByUserIdOrderByEntryDateDesc(UUID userId);

    List<Entry> findByUserIdAndEntryDateBetweenOrderByEntryDateDesc(
            UUID userId, LocalDate from, LocalDate to);

    @Query("SELECT e FROM Entry e WHERE e.user.id = :userId AND e.isPublic = true ORDER BY e.entryDate DESC")
    List<Entry> findPublicEntriesByUserId(@Param("userId") UUID userId);

    boolean existsByIdAndUserId(UUID entryId, UUID userId);

    @Query("SELECT e.entryDate, COUNT(e) FROM Entry e " +
           "WHERE e.user.id = :userId AND year(e.entryDate) = :year " +
           "GROUP BY e.entryDate")
    List<Object[]> countEntriesByDateForYear(@Param("userId") UUID userId, @Param("year") int year);

    @Query("SELECT DISTINCT e FROM Entry e " +
           "LEFT JOIN FETCH e.entryTags et " +
           "LEFT JOIN FETCH et.tag " +
           "WHERE e.user.id = :userId " +
           "AND e.entryDate BETWEEN :from AND :to " +
           "ORDER BY e.entryDate DESC")
    List<Entry> findEntriesWithTagsBetween(@Param("userId") UUID userId,
                                           @Param("from") LocalDate from,
                                           @Param("to") LocalDate to);

    @Query("SELECT COUNT(DISTINCT e.entryDate) FROM Entry e WHERE e.user.id = :userId")
    long countActiveDays(@Param("userId") UUID userId);

    @Query("SELECT DISTINCT e.entryDate FROM Entry e " +
           "WHERE e.user.id = :userId ORDER BY e.entryDate DESC")
    List<LocalDate> findAllEntryDatesByUserId(@Param("userId") UUID userId);
}
