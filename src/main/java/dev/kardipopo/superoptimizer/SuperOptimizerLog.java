package dev.kardipopo.superoptimizer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class SuperOptimizerLog {
    private static final int MAX_LINES = 500;
    private static final ArrayDeque<String> LINES = new ArrayDeque<>(MAX_LINES);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static Path file;

    private SuperOptimizerLog() {}

    public static synchronized void init(Path configDir, boolean fileLogging) {
        file = configDir.resolve("superoptimizer.log");
        if (fileLogging) {
            try {
                Files.createDirectories(configDir);
            } catch (IOException e) {
                SuperOptimizerClient.LOGGER.warn("Не удалось создать папку логов SuperOptimizer", e);
            }
        }
    }

    public static synchronized void info(String message) {
        add("INFO", message);
    }

    public static synchronized void warn(String message) {
        add("WARN", message);
    }

    public static synchronized void error(String message) {
        add("ERROR", message);
    }

    private static void add(String level, String message) {
        String line = "[" + LocalTime.now().format(TIME) + "] [" + level + "] " + message;
        if (LINES.size() >= MAX_LINES) LINES.removeFirst();
        LINES.addLast(line);

        if (level.equals("ERROR")) SuperOptimizerClient.LOGGER.error(message);
        else if (level.equals("WARN")) SuperOptimizerClient.LOGGER.warn(message);
        else SuperOptimizerClient.LOGGER.info(message);

        SuperOptimizerConfig c = SuperOptimizerClient.config();
        if (file != null && c != null && c.fileLogging) {
            try {
                Files.writeString(
                    file,
                    line + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND
                );
            } catch (IOException ignored) {
                // Do not crash the game because a diagnostic file is unavailable.
            }
        }
    }

    public static synchronized List<String> snapshot() {
        return new ArrayList<>(LINES);
    }

    public static synchronized void clear() {
        LINES.clear();
        if (file != null) {
            try {
                Files.writeString(file, "", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException ignored) {}
        }
    }
}
