package dev.kardipopo.memento;

import java.time.Instant;

public record MemoryRecord(
        long id,
        long timestamp,
        String event,
        String world,
        String dimension,
        String biome,
        double x,
        double y,
        double z,
        float health,
        String screenshot
) {
    public String timeText() {
        return Instant.ofEpochMilli(timestamp).toString().replace('T', ' ').replace("Z", "");
    }

    public String coordsText() {
        return String.format(java.util.Locale.ROOT, "%.0f, %.0f, %.0f", x, y, z);
    }
}
