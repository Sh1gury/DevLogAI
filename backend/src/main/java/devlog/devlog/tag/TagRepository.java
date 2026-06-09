package devlog.devlog.tag;

import devlog.devlog.tag.dto.TagResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, UUID> {

    List<Tag> findByUserId(UUID userId);

    Optional<Tag> findByUserIdAndName(UUID userId, String name);

    boolean existsByUserIdAndName(UUID userId, String name);

    boolean existsByIdAndUserId(UUID tagId, UUID userId);

    @Query("SELECT new devlog.devlog.tag.dto.TagResponse(t.id, t.name, t.color, COUNT(et)) " +
           "FROM Tag t LEFT JOIN t.entryTags et " +
           "WHERE t.userId = :userId " +
           "GROUP BY t.id, t.name, t.color ORDER BY t.name")
    List<TagResponse> findTagsWithUsage(@Param("userId") UUID userId);
}
