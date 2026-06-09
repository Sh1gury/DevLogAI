package devlog.devlog.tag;

import devlog.devlog.entry.Entry;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "entry_tags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class EntryTag {
    @EmbeddedId
    @EqualsAndHashCode.Include
    @ToString.Include
    EntryTagId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("entryId")
    Entry entry;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("tagId")
    Tag tag;
}
