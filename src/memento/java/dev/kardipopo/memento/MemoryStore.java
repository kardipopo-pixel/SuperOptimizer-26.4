package dev.kardipopo.memento;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

public final class MemoryStore {
    private static Path memoryDir;
    private static final List<MemoryRecord> records = new ArrayList<>();
    private static long nextId = 1;

    private MemoryStore() {}

    public static synchronized void init(Path configRoot) {
        memoryDir = configRoot.resolve("memories");
        try {
            Files.createDirectories(memoryDir);
            load();
        } catch (IOException e) {
            MementoLog.warn("Не удалось открыть хранилище: " + e.getMessage());
        }
    }

    public static synchronized List<MemoryRecord> all() {
        return List.copyOf(records);
    }

    public static synchronized long nextId() {
        return nextId++;
    }

    public static synchronized void add(MemoryRecord record) {
        records.removeIf(r -> r.id() == record.id());
        records.add(record);
        records.sort(Comparator.comparingLong(MemoryRecord::timestamp).reversed());
        save(record);
        while (records.size() > 250) {
            MemoryRecord old = records.remove(records.size() - 1);
            try {
                Files.deleteIfExists(fileFor(old.id()));
            } catch (IOException ignored) {}
        }
    }

    private static Path fileFor(long id) {
        return memoryDir.resolve(id + ".properties");
    }

    private static void save(MemoryRecord r) {
        try {
            Properties p = new Properties();
            p.setProperty("id", Long.toString(r.id()));
            p.setProperty("timestamp", Long.toString(r.timestamp()));
            p.setProperty("event", r.event());
            p.setProperty("world", r.world());
            p.setProperty("dimension", r.dimension());
            p.setProperty("biome", r.biome());
            p.setProperty("x", Double.toString(r.x()));
            p.setProperty("y", Double.toString(r.y()));
            p.setProperty("z", Double.toString(r.z()));
            p.setProperty("health", Float.toString(r.health()));
            p.setProperty("screenshot", r.screenshot());
            try (Writer writer = Files.newBufferedWriter(fileFor(r.id()))) {
                p.store(writer, "Memento memory");
            }
        } catch (IOException e) {
            MementoLog.warn("Не удалось сохранить воспоминание: " + e.getMessage());
        }
    }

    private static void load() throws IOException {
        records.clear();
        long maxId = 0;
        try (var stream = Files.list(memoryDir)) {
            for (Path file : stream.filter(p -> p.getFileName().toString().endsWith(".properties")).toList()) {
                try (Reader reader = Files.newBufferedReader(file)) {
                    Properties p = new Properties();
                    p.load(reader);
                    long id = Long.parseLong(p.getProperty("id"));
                    records.add(new MemoryRecord(
                            id,
                            Long.parseLong(p.getProperty("timestamp")),
                            p.getProperty("event", "Неизвестно"),
                            p.getProperty("world", "Неизвестный мир"),
                            p.getProperty("dimension", "Неизвестное измерение"),
                            p.getProperty("biome", "Неизвестный биом"),
                            Double.parseDouble(p.getProperty("x", "0")),
                            Double.parseDouble(p.getProperty("y", "0")),
                            Double.parseDouble(p.getProperty("z", "0")),
                            Float.parseFloat(p.getProperty("health", "0")),
                            p.getProperty("screenshot", "")
                    ));
                    maxId = Math.max(maxId, id);
                } catch (Exception e) {
                    MementoLog.warn("Пропущена повреждённая запись: " + file.getFileName());
                }
            }
        }
        records.sort(Comparator.comparingLong(MemoryRecord::timestamp).reversed());
        nextId = maxId + 1;
        MementoLog.info("Загружено воспоминаний: " + records.size());
    }

    public static synchronized void clearAll() {
        records.clear();
        try (var stream = Files.list(memoryDir)) {
            for (Path p : stream.toList()) Files.deleteIfExists(p);
        } catch (IOException e) {
            MementoLog.warn("Не удалось очистить архив: " + e.getMessage());
        }
        nextId = 1;
    }

    public static synchronized Path archivePath() {
        return memoryDir;
    }
}
