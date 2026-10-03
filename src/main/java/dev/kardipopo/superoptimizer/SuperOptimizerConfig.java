package dev.kardipopo.superoptimizer;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class SuperOptimizerConfig {
    public boolean enabled = true;

    // Render-side optimizations
    public boolean entityCulling = true;
    public boolean blockEntityCulling = false;
    public boolean pauseDuringCameraMotion = true;
    public boolean skipNearEntityCulling = true;
    public int nearEntityDistance = 12;

    // Compatibility
    public boolean disableCullingWithIris = true;
    public boolean disableCullingWithEntityCullingMod = true;

    // Background work
    public boolean backgroundTasks = true;
    public boolean shaderScanAsync = true;
    public int workerThreads = Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() / 2));
    public int reservedCores = Math.min(2, Math.max(1, Runtime.getRuntime().availableProcessors() / 4));

    // Diagnostics
    public boolean diagnostics = true;
    public boolean fileLogging = true;

    public static SuperOptimizerConfig load(Path dir) {
        SuperOptimizerConfig c = new SuperOptimizerConfig();
        Path file = dir.resolve("superoptimizer.properties");
        if (!Files.isRegularFile(file)) return c;

        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(file)) {
            p.load(r);

            c.enabled = bool(p, "enabled", c.enabled);
            c.entityCulling = bool(p, "entityCulling", c.entityCulling);
            c.blockEntityCulling = bool(p, "blockEntityCulling", c.blockEntityCulling);
            c.pauseDuringCameraMotion = bool(p, "pauseDuringCameraMotion", c.pauseDuringCameraMotion);
            c.skipNearEntityCulling = bool(p, "skipNearEntityCulling", c.skipNearEntityCulling);
            c.nearEntityDistance = clamp(integer(p, "nearEntityDistance", c.nearEntityDistance), 0, 64);

            c.disableCullingWithIris = bool(p, "disableCullingWithIris", c.disableCullingWithIris);
            c.disableCullingWithEntityCullingMod = bool(
                p, "disableCullingWithEntityCullingMod", c.disableCullingWithEntityCullingMod
            );

            c.backgroundTasks = bool(p, "backgroundTasks", c.backgroundTasks);
            c.shaderScanAsync = bool(p, "shaderScanAsync", c.shaderScanAsync);
            c.workerThreads = clamp(integer(p, "workerThreads", c.workerThreads), 1, 32);
            c.reservedCores = clamp(integer(p, "reservedCores", c.reservedCores), 0, 64);

            c.diagnostics = bool(p, "diagnostics", c.diagnostics);
            c.fileLogging = bool(p, "fileLogging", c.fileLogging);
        } catch (IOException e) {
            SuperOptimizerClient.LOGGER.warn("Не удалось прочитать конфиг SuperOptimizer", e);
        }
        return c;
    }

    public void save(Path dir) {
        try {
            Files.createDirectories(dir);

            Properties p = new Properties();
            p.setProperty("enabled", Boolean.toString(enabled));
            p.setProperty("entityCulling", Boolean.toString(entityCulling));
            p.setProperty("blockEntityCulling", Boolean.toString(blockEntityCulling));
            p.setProperty("pauseDuringCameraMotion", Boolean.toString(pauseDuringCameraMotion));
            p.setProperty("skipNearEntityCulling", Boolean.toString(skipNearEntityCulling));
            p.setProperty("nearEntityDistance", Integer.toString(nearEntityDistance));

            p.setProperty("disableCullingWithIris", Boolean.toString(disableCullingWithIris));
            p.setProperty("disableCullingWithEntityCullingMod", Boolean.toString(disableCullingWithEntityCullingMod));

            p.setProperty("backgroundTasks", Boolean.toString(backgroundTasks));
            p.setProperty("shaderScanAsync", Boolean.toString(shaderScanAsync));
            p.setProperty("workerThreads", Integer.toString(workerThreads));
            p.setProperty("reservedCores", Integer.toString(reservedCores));

            p.setProperty("diagnostics", Boolean.toString(diagnostics));
            p.setProperty("fileLogging", Boolean.toString(fileLogging));

            try (Writer w = Files.newBufferedWriter(dir.resolve("superoptimizer.properties"))) {
                p.store(w, "SuperOptimizer 26.4 - русская конфигурация");
            }
        } catch (IOException e) {
            SuperOptimizerClient.LOGGER.warn("Не удалось сохранить конфиг SuperOptimizer", e);
        }
    }

    private static boolean bool(Properties p, String key, boolean fallback) {
        String value = p.getProperty(key);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }

    private static int integer(Properties p, String key, int fallback) {
        try {
            return Integer.parseInt(p.getProperty(key, Integer.toString(fallback)).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
