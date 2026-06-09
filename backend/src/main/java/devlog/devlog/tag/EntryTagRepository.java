package devlog.devlog.tag;

import devlog.devlog.tag.dto.TagUsageCount;
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

    @Query("SELECT new devlog.devlog.tag.dto.TagUsageCount(et.tag.name, et.tag.color, COUNT(et)) " +
           "FROM EntryTag et " +
           "WHERE et.entry.userId = :userId " +
           "AND et.entry.entryDate BETWEEN :from AND :to " +
           "GROUP BY et.tag.name, et.tag.color ORDER BY COUNT(et) DESC")
    List<TagUsageCount> countTagUsageForPeriod(@Param("userId") UUID userId,
                                               @Param("from") LocalDate from,
                                               @Param("to") LocalDate to);

    @Query("SELECT COUNT(et) FROM EntryTag et WHERE et.tag.id = :tagId")
    long countByTagId(@Param("tagId") UUID tagId);
}
