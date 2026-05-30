package devlog.devlog.tag;

import devlog.devlog.entry.Entry;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "entry_tags")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntryTag {
    @EmbeddedId
    EntryTagId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("entryId")
    Entry entry;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("tagId")
    Tag tag;
}
