package devlog.devlog.tag.dto;

/**
 * Typed projection for tag usage aggregation (replaces raw Object[] rows).
 */
public record TagUsageCount(String name, String color, long count) {}
