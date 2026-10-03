package dev.kardipopo.superoptimizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Feature-level failure protection and configuration rollback. */
public final class SelfHealingManager {
    private static final Map<String, Integer> failures = new ConcurrentHashMap<>();
    private static Path configFile;
    private static Path backupFile;

    private SelfHealingManager() {}

    public static void init(Path configDir) {
        configFile = configDir.resolve("superoptimizer.properties");
        backupFile = configDir.resolve("superoptimizer.properties.bak");
        createBackup();
    }

    private static void createBackup() {
        SuperOptimizerConfig c = SuperOptimizerClient.config();
        if (c == null || !c.configurationRollback || configFile == null) return;
        try {
            if (Files.isRegularFile(configFile)) {
                Files.copy(configFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            SuperOptimizerLog.warn("Не удалось создать резервную копию конфигурации: " + e.getMessage());
        }
    }

    public static void reportFailure(String feature, Throwable error) {
        SuperOptimizerConfig c = SuperOptimizerClient.config();
        if (c == null || !c.selfHealing) return;

        int count = failures.merge(feature, 1, Integer::sum);
        SuperOptimizerLog.warn("Self-Healing: ошибка функции " + feature + " (" + count + "/" + c.selfHealingFailureThreshold + "): " + error);

        if (count >= c.selfHealingFailureThreshold) {
            disable(feature);
            failures.remove(feature);
        }
    }

    private static void disable(String feature) {
        SuperOptimizerConfig c = SuperOptimizerClient.config();
        if (c == null) return;

        switch (feature) {
            case "particles" -> c.particleOptimization = false;
            case "entity-culling" -> c.entityCulling = false;
            case "block-entity-culling" -> c.blockEntityCulling = false;
            case "adaptive" -> c.adaptivePerformance = false;
            case "frame-profiler" -> c.profilingEnabled = false;
            case "background" -> c.backgroundTasks = false;
            default -> { return; }
        }

        c.save(configFile.getParent());
        SuperOptimizerLog.warn("Self-Healing отключил функцию: " + feature);
        SuperOptimizerClient.applyConfig();
    }

    public static boolean rollback() {
        if (configFile == null || backupFile == null || !Files.isRegularFile(backupFile)) return false;
        try {
            Files.copy(backupFile, configFile, StandardCopyOption.REPLACE_EXISTING);
            SuperOptimizerLog.info("Конфигурация восстановлена из резервной копии.");
            SuperOptimizerClient.reloadConfig();
            return true;
        } catch (IOException e) {
            SuperOptimizerLog.warn("Откат конфигурации не удался: " + e.getMessage());
            return false;
        }
    }

    public static int failures(String feature) { return failures.getOrDefault(feature, 0); }
}