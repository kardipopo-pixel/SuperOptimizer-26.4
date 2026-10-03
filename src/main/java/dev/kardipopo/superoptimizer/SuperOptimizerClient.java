package dev.kardipopo.superoptimizer;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class SuperOptimizerClient implements ClientModInitializer {
    public enum Preset {
        MICROWAVE,
        LIGHT,
        BALANCED,
        ADVANCED
    }

    public static final String MOD_ID = "superoptimizer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static SuperOptimizerConfig config;
    private static ExecutorService executor;
    private static KeyMapping openSettings;

    @Override
    public void onInitializeClient() {
        Path configDir = Minecraft.getInstance().gameDirectory.toPath().resolve("config");

        config = SuperOptimizerConfig.load(configDir);
        SuperOptimizerLog.init(configDir, config.fileLogging);
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

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openSettings.consumeClick()) {
                client.setScreenAndShow(new SuperOptimizerScreen(null, config));
            }
        });

        verifyMixinTargetLoaded();
        SuperOptimizerLog.info("SuperOptimizer 26.4 запущен.");
    }

    public static synchronized void applyConfig() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }

        if (config == null || !config.enabled || !config.backgroundTasks || !config.shaderScanAsync) {
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
        SuperOptimizerLog.info("CPU worker pool: " + workers + ", резерв CPU-ядер: " + config.reservedCores);
    }

    public static void applyPreset(Preset preset) {
        switch (preset) {
            case MICROWAVE -> {
                // Maximum performance profile: disable background diagnostics and use
                // aggressive safe render submission filtering.
                config.enabled = true;
                config.entityCulling = true;
                config.blockEntityCulling = true;
                config.pauseDuringCameraMotion = true;
                config.skipNearEntityCulling = false;
                config.nearEntityDistance = 0;
                config.disableCullingWithIris = true;
                config.disableCullingWithEntityCullingMod = true;
                config.backgroundTasks = false;
                config.shaderScanAsync = false;
                config.diagnostics = false;
                config.fileLogging = false;
                config.reservedCores = 1;
                config.workerThreads = 1;
            }
            case LIGHT -> {
                config.enabled = true;
                config.entityCulling = true;
                config.blockEntityCulling = false;
                config.pauseDuringCameraMotion = true;
                config.skipNearEntityCulling = true;
                config.nearEntityDistance = 12;
                config.disableCullingWithIris = true;
                config.disableCullingWithEntityCullingMod = true;
                config.backgroundTasks = true;
                config.shaderScanAsync = true;
                config.workerThreads = Math.max(1, Math.min(2, Runtime.getRuntime().availableProcessors() / 4));
                config.reservedCores = Math.min(2, Math.max(1, Runtime.getRuntime().availableProcessors() / 4));
                config.diagnostics = true;
                config.fileLogging = false;
            }
            case BALANCED -> {
                config.enabled = true;
                config.entityCulling = true;
                config.blockEntityCulling = true;
                config.pauseDuringCameraMotion = true;
                config.skipNearEntityCulling = true;
                config.nearEntityDistance = 12;
                config.disableCullingWithIris = true;
                config.disableCullingWithEntityCullingMod = true;
                config.backgroundTasks = true;
                config.shaderScanAsync = true;
                config.workerThreads = Math.max(1, Math.min(3, Runtime.getRuntime().availableProcessors() / 3));
                config.reservedCores = Math.min(2, Math.max(1, Runtime.getRuntime().availableProcessors() / 4));
                config.diagnostics = true;
                config.fileLogging = true;
            }
            case ADVANCED -> {
                config.enabled = true;
                config.entityCulling = true;
                config.blockEntityCulling = true;
                config.pauseDuringCameraMotion = true;
                config.skipNearEntityCulling = false;
                config.nearEntityDistance = 0;
                config.disableCullingWithIris = true;
                config.disableCullingWithEntityCullingMod = true;
                config.backgroundTasks = true;
                config.shaderScanAsync = true;
                config.workerThreads = Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() / 2));
                config.reservedCores = Math.min(2, Math.max(1, Runtime.getRuntime().availableProcessors() / 4));
                config.diagnostics = true;
                config.fileLogging = true;
            }
        }

        config.save(Minecraft.getInstance().gameDirectory.toPath().resolve("config"));
        applyConfig();
        SuperOptimizerLog.info("Применён профиль: " + preset.name());
    }

    private static void verifyMixinTargetLoaded() {
        try {
            Class.forName("net.minecraft.client.renderer.LevelRenderer", false, SuperOptimizerClient.class.getClassLoader());
            LOGGER.info("SuperOptimizer 26.4: LevelRenderer target class loaded and Mixin transformation was accepted.");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("SuperOptimizer could not load the 26.4 LevelRenderer target", e);
        } catch (LinkageError e) {
            throw new IllegalStateException("SuperOptimizer Mixin target failed to link on Minecraft 26.4", e);
        }
    }

    public static SuperOptimizerConfig config() {
        return config;
    }

    public static ExecutorService executor() {
        return executor;
    }
}
