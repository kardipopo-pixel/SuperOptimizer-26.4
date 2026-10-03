package dev.kardipopo.superoptimizer;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class SuperOptimizerClient implements ClientModInitializer {
    public enum Preset {
        MICROWAVE, LIGHT, BALANCED, ADVANCED
    }

    public static final String MOD_ID = "superoptimizer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static SuperOptimizerConfig config;
    private static ExecutorService executor;
    private static KeyMapping openSettings;
    private static KeyMapping benchmarkKey;

    @Override
    public void onInitializeClient() {
        Path configDir = Minecraft.getInstance().gameDirectory.toPath().resolve("config");

        config = SuperOptimizerConfig.load(configDir);
        SuperOptimizerLog.init(configDir, config.fileLogging);
        PerformanceProfiler.configure(config);
        AdaptivePerformanceController.init(config);
        SelfHealingManager.init(configDir);
        applyConfig();

        KeyMapping.Category category = KeyMapping.Category.register(
                net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID, "main")
        );

        openSettings = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.superoptimizer.open_settings",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_F8,
                category
        ));

        benchmarkKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.superoptimizer.benchmark",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_F7,
                category
        ));

        GraphicsSettingsIntegration.init();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openSettings.consumeClick()) {
                client.setScreenAndShow(new SuperOptimizerScreen(null, config));
            }
            while (benchmarkKey.consumeClick()) {
                Path dir = client.gameDirectory.toPath().resolve("config");
                if (PerformanceProfiler.before() == null) {
                    PerformanceProfiler.startBenchmark(dir);
                } else {
                    PerformanceProfiler.finishBenchmark(dir);
                }
            }
            MemoryPressureController.tick();
            AdaptivePerformanceController.tick();
        });

        verifyMixinTargetLoaded();
        SuperOptimizerLog.info("SuperOptimizer 26.4 запущен.");
    }

    public static synchronized void applyConfig() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }

        if (config == null || !config.enabled || !config.backgroundTasks) {
            if (config != null && !config.enabled) {
                SuperOptimizerLog.info("Оптимизатор отключён пользователем.");
            }
            return;
        }

        int cpus = Runtime.getRuntime().availableProcessors();
        int maxWorkers = Math.max(1, cpus - config.reservedCores);
        int workers = Math.min(config.workerThreads, maxWorkers);

        AtomicInteger sequence = new AtomicInteger();
        ThreadFactory factory = runnable -> {
            Thread t = new Thread(runnable, "SuperOptimizer-" + sequence.incrementAndGet());
            t.setDaemon(true);
            t.setPriority(Math.max(Thread.MIN_PRIORITY, Thread.NORM_PRIORITY - 2));
            return t;
        };

        executor = Executors.newFixedThreadPool(workers, factory);
        SuperOptimizerLog.info("CPU worker pool: " + workers
                + ", резерв CPU-ядер: " + config.reservedCores);
    }

    public static void applyPreset(Preset preset) {
        if (config == null) return;

        switch (preset) {
            case MICROWAVE -> {
                config.enabled = true;
                config.entityCulling = true;
                config.blockEntityCulling = true;
                config.entityDistanceCulling = true;
                config.entityRenderDistance = 96;
                config.entityLod = true;
                config.hideDistantNames = true;
                config.hideDistantShadows = true;
                config.frameCulling = true;
                config.armorStandCulling = true;
                config.signCulling = true;
                config.chestCulling = true;
                config.hopperCulling = true;
                config.particleOptimization = true;
                config.maxParticles = 1024;
                config.particleDistance = 48;
                config.particleDistanceCulling = true;
                config.particleAdaptive = true;
                config.adaptivePerformance = true;
                config.smartFrameBudget = true;
                config.chunkRebuildDeduplication = true;
                config.taskBackpressure = true;
                config.backgroundTasks = true;
                config.workerThreads = 1;
                config.reservedCores = 1;
                config.showTelemetry = false;
                config.fileLogging = false;
            }
            case LIGHT -> {
                config.enabled = true;
                config.entityCulling = true;
                config.blockEntityCulling = false;
                config.entityDistanceCulling = true;
                config.entityRenderDistance = 160;
                config.entityLod = false;
                config.hideDistantNames = false;
                config.hideDistantShadows = false;
                config.particleOptimization = true;
                config.maxParticles = 4096;
                config.particleDistance = 96;
                config.particleDistanceCulling = true;
                config.particleAdaptive = true;
                config.adaptivePerformance = false;
                config.smartFrameBudget = true;
                config.chunkRebuildDeduplication = true;
                config.taskBackpressure = true;
                config.backgroundTasks = true;
                config.workerThreads = Math.max(1, Math.min(2, Runtime.getRuntime().availableProcessors() / 4));
                config.reservedCores = Math.min(2, Math.max(1, Runtime.getRuntime().availableProcessors() / 4));
                config.showTelemetry = true;
                config.fileLogging = false;
            }
            case BALANCED -> {
                config.enabled = true;
                config.entityCulling = true;
                config.blockEntityCulling = true;
                config.entityDistanceCulling = true;
                config.entityRenderDistance = 192;
                config.entityLod = false;
                config.hideDistantNames = false;
                config.hideDistantShadows = false;
                config.particleOptimization = true;
                config.maxParticles = 8192;
                config.particleDistance = 128;
                config.particleDistanceCulling = true;
                config.particleAdaptive = true;
                config.adaptivePerformance = true;
                config.smartFrameBudget = true;
                config.chunkRebuildDeduplication = true;
                config.adaptiveChunkScheduler = true;
                config.taskBackpressure = true;
                config.backgroundTasks = true;
                config.workerThreads = Math.max(1, Math.min(3, Runtime.getRuntime().availableProcessors() / 3));
                config.reservedCores = Math.min(2, Math.max(1, Runtime.getRuntime().availableProcessors() / 4));
                config.showTelemetry = true;
                config.fileLogging = true;
            }
            case ADVANCED -> {
                config.enabled = true;
                config.entityCulling = true;
                config.blockEntityCulling = true;
                config.entityDistanceCulling = true;
                config.entityRenderDistance = 160;
                config.entityLod = true;
                config.hideDistantNames = true;
                config.hideDistantShadows = true;
                config.particleOptimization = true;
                config.maxParticles = 6144;
                config.particleDistance = 112;
                config.particleDistanceCulling = true;
                config.particleAdaptive = true;
                config.adaptivePerformance = true;
                config.smartFrameBudget = true;
                config.chunkRebuildDeduplication = true;
                config.adaptiveChunkScheduler = true;
                config.predictiveVisibility = true;
                config.taskBackpressure = true;
                config.backgroundTasks = true;
                config.workerThreads = Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() / 2));
                config.reservedCores = Math.min(2, Math.max(1, Runtime.getRuntime().availableProcessors() / 4));
                config.showTelemetry = true;
                config.fileLogging = true;
            }
        }

        config.activePreset = preset.name();
        config.save(Minecraft.getInstance().gameDirectory.toPath().resolve("config"));
        PerformanceProfiler.configure(config);
        AdaptivePerformanceController.init(config);
        applyConfig();
        SelfHealingManager.init(Minecraft.getInstance().gameDirectory.toPath().resolve("config"));
        SuperOptimizerLog.info("Применён профиль: " + preset.name());
    }

    public static void reloadConfig() {
        Path dir = Minecraft.getInstance().gameDirectory.toPath().resolve("config");
        config = SuperOptimizerConfig.load(dir);
        PerformanceProfiler.configure(config);
        AdaptivePerformanceController.init(config);
        applyConfig();
        SuperOptimizerLog.info("Конфигурация перечитана.");
    }

    private static void verifyMixinTargetLoaded() {
        try {
            Class.forName("net.minecraft.client.renderer.LevelRenderer", false,
                    SuperOptimizerClient.class.getClassLoader());
            LOGGER.info("SuperOptimizer 26.4: LevelRenderer target class loaded.");
        } catch (ClassNotFoundException | LinkageError e) {
            throw new IllegalStateException(
                    "SuperOptimizer could not load the 26.4 LevelRenderer target", e);
        }
    }

    public static SuperOptimizerConfig config() { return config; }
    public static ExecutorService executor() { return executor; }
}