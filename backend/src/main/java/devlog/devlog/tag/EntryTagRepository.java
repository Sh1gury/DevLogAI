package devlog.devlog.tag;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface EntryTagRepository extends JpaRepository<EntryTag, EntryTagId> {

    @Modifying
    @Query("DELETE FROM EntryTag et WHERE et.entry.id = :entryId")
    void deleteByEntryId(@Param("entryId") UUID entryId);

    @Query("SELECT et.tag.name, et.tag.color, COUNT(et) FROM EntryTag et " +
           "WHERE et.entry.user.id = :userId " +
           "AND et.entry.entryDate BETWEEN :from AND :to " +
           "GROUP BY et.tag.name, et.tag.color ORDER BY COUNT(et) DESC")
    List<Object[]> countTagUsageForPeriod(@Param("userId") UUID userId,
                                          @Param("from") LocalDate from,
                                          @Param("to") LocalDate to);

    @Query("SELECT COUNT(et) FROM EntryTag et WHERE et.tag.id = :tagId")
    long countByTagId(@Param("tagId") UUID tagId);
}
