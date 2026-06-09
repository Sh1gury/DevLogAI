package devlog.devlog.entry.dto;

import java.time.LocalDate;

/**
 * Typed projection for per-day entry counts (heatmap), replaces raw Object[] rows.
 */
public record EntryDateCount(LocalDate entryDate, long count) {}
