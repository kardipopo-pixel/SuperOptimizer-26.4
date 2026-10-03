package dev.kardipopo.superoptimizer;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class SuperOptimizerClient implements ClientModInitializer {
    public static final String MOD_ID = "superoptimizer";
    public static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(MOD_ID);

    private static SuperOptimizerConfig config;
    private static ExecutorService executor;
    private static KeyMapping openSettings;
    private static int diagnosticsTicks;

    @Override
    public void onInitializeClient() {
        Path configDir = Minecraft.getInstance().gameDirectory.toPath().resolve("config");
        config = SuperOptimizerConfig.load(configDir);
        rebuildExecutor();

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
                client.gui.setScreen(new SuperOptimizerScreen(null, config));
            }
        });

        verifyMixinTargetLoaded();
        LOGGER.info("SuperOptimizer 26.4: clean client bootstrap loaded.");
    }

    public static synchronized void rebuildExecutor() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        if (config == null || !config.enabled || !config.asyncPreparation) return;

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

    public static SuperOptimizerConfig config() { return config; }
    public static ExecutorService executor() { return executor; }
}
