package dev.kardipopo.superoptimizer;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

public final class SuperOptimizerScreen extends Screen {
    private enum Category {
        GENERAL("superoptimizer.category.general", "Главное"),
        PROFILING("superoptimizer.category.profiling", "Профилирование"),
        ADAPTIVE("superoptimizer.category.adaptive", "Adaptive Performance"),
        PARTICLES("superoptimizer.category.particles", "Частицы"),
        ENTITIES("superoptimizer.category.entities", "Сущности"),
        BLOCK_ENTITIES("superoptimizer.category.block_entities", "Блок-сущности"),
        CHUNKS("superoptimizer.category.chunks", "Чанки и Frame Pacing"),
        MEMORY("superoptimizer.category.memory", "Память и кэши"),
        LOADING("superoptimizer.category.loading", "Загрузка и Storage"),
        HARDWARE("superoptimizer.category.hardware", "CPU / GPU / Железо"),
        RESOURCES("superoptimizer.category.resources", "Ресурсы и текстуры"),
        WORLD("superoptimizer.category.world", "Мир / Сеть / Redstone"),
        JAVA("superoptimizer.category.java", "Java Runtime"),
        LIGHTING_SOUND("superoptimizer.category.lighting_sound", "Свет и звук"),
        COMPATIBILITY("superoptimizer.category.compatibility", "Совместимость"),
        DIAGNOSTICS("superoptimizer.category.diagnostics", "Диагностика"),
        EXPERIMENTAL("superoptimizer.category.experimental", "Экспериментальные"),
        PRESETS("superoptimizer.category.presets", "Профили"),
        SODIUM("superoptimizer.integration.sodium", "Sodium"),
        IRIS("superoptimizer.integration.iris", "Iris"),
        ENTITY_CULLING("superoptimizer.integration.entity_culling", "Entity Culling");

        final String key;
        final String fallback;
        Category(String key, String fallback) {
            this.key = key;
            this.fallback = fallback;
        }
    }

    private enum Impact {
        BEST(0xFF20D7A4, "BEST"),
        MEDIUM(0xFFFFD166, "MEDIUM"),
        LOW(0xFFFF6B6B, "LOW"),
        UNKNOWN(0xFF8793A8, "UNKNOWN");

        final int color;
        final String label;
        Impact(int color, String label) {
            this.color = color;
            this.label = label;
        }
    }

    private record Row(Button hitbox, String labelKey, String descKey, BooleanSupplier bool, IntSupplier integer,
                       int y, Impact impact) {}

    private final Screen parent;
    private final SuperOptimizerConfig config;
    private final Category category;
    private final List<Button> navButtons = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();
    private final List<Button> actionButtons = new ArrayList<>();

    private int navX, navW, mainX, mainW, navTop;
    private int contentTop, contentBottom, pageBottom;
    private double contentScroll, contentMaxScroll;
    private double navScroll, navMaxScroll;
    private Row hovered;

    public SuperOptimizerScreen(Screen parent, SuperOptimizerConfig config) {
        this(parent, config, Category.GENERAL);
    }

    public SuperOptimizerScreen(Screen parent, SuperOptimizerConfig config, Category category) {
        super(Component.translatable("superoptimizer.gui.graphics_title"));
        this.parent = parent;
        this.config = config;
        this.category = category;
    }

    @Override
    protected void init() {
        navButtons.clear();
        rows.clear();
        actionButtons.clear();
        contentScroll = 0;
        navScroll = 0;

        int margin = 14;
        int gap = 14;

        navX = margin;
        navW = Math.min(270, Math.max(220, this.width / 4));
        mainX = navX + navW + gap;
        mainW = Math.min(670, this.width - mainX - margin);
        if (mainW < 390) {
            navW = Math.max(180, this.width / 3 - gap);
            mainX = navX + navW + gap;
            mainW = this.width - mainX - margin;
        }

        contentTop = 78;
        contentBottom = this.height - 42;
        navTop = 236;

        buildNav();
        buildPage();

        contentMaxScroll = Math.max(0, pageBottom - contentBottom + 10);
        navMaxScroll = Math.max(0, Category.values().length * 36 - (contentBottom - navTop));
        applyScroll();
    }

    private void buildNav() {
        int y = navTop;
        for (Category c : Category.values()) {
            Button b = invisible(Component.literal(c.fallback),
                    q -> minecraft.setScreenAndShow(new SuperOptimizerScreen(parent, config, c)),
                    navX + 4, y, navW - 8, 32);
            navButtons.add(b);
            y += 36;
        }
    }

    private void buildPage() {
        int y = contentTop + 32;
        switch (category) {
            case GENERAL -> y = general(y);
            case PROFILING -> y = profiling(y);
            case ADAPTIVE -> y = adaptive(y);
            case PARTICLES -> y = particles(y);
            case ENTITIES -> y = entities(y);
            case BLOCK_ENTITIES -> y = blockEntities(y);
            case CHUNKS -> y = chunks(y);
            case MEMORY -> y = memory(y);
            case LOADING -> y = loading(y);
            case HARDWARE -> y = hardware(y);
            case RESOURCES -> y = resources(y);
            case WORLD -> y = world(y);
            case JAVA -> y = javaRuntime(y);
            case LIGHTING_SOUND -> y = lightingSound(y);
            case COMPATIBILITY -> y = compatibility(y);
            case DIAGNOSTICS -> y = diagnostics(y);
            case EXPERIMENTAL -> y = experimental(y);
            case PRESETS -> y = presets(y);
            case SODIUM -> y = external("sodium", y);
            case IRIS -> y = external("iris", y);
            case ENTITY_CULLING -> y = external("entityculling", y);
        }
        pageBottom = y + 12;
    }

    private int general(int y) {
        y = header(y, "superoptimizer.category.general", "superoptimizer.page.general.desc");
        y = profileRow(y);
        y = toggle(y, "superoptimizer.option.enabled", "superoptimizer.desc.enabled", () -> config.enabled, v -> {
            config.enabled = v;
            markCustom();
            SuperOptimizerClient.applyConfig();
        }, Impact.BEST);
        y = toggle(y, "superoptimizer.option.show_telemetry", "superoptimizer.desc.show_telemetry", () -> config.showTelemetry, v -> {
            config.showTelemetry = v;
            markCustom();
        }, Impact.MEDIUM);
        y = toggle(y, "superoptimizer.option.detect_bottleneck", "superoptimizer.desc.detect_bottleneck", () -> config.detectBottleneck, v -> {
            config.detectBottleneck = v;
            markCustom();
        }, Impact.BEST);
        y = toggle(y, "superoptimizer.option.smart_frame_budget", "superoptimizer.desc.smart_frame_budget", () -> config.smartFrameBudget, v -> {
            config.smartFrameBudget = v;
            markCustom();
        }, Impact.BEST);
        y = toggle(y, "superoptimizer.option.frame_stabilizer", "superoptimizer.desc.frame_stabilizer", () -> config.frameTimeStabilizer, v -> {
            config.frameTimeStabilizer = v;
            markCustom();
        }, Impact.BEST);
        return y;
    }

    private int profiling(int y) {
        y = header(y, "superoptimizer.category.profiling", "superoptimizer.page.profiling.desc");
        y = toggle(y, "superoptimizer.option.profiling", "superoptimizer.desc.profiling", () -> config.profilingEnabled, v -> {
            config.profilingEnabled = v; markCustom();
        }, Impact.BEST);

        PerformanceProfiler.Snapshot s = PerformanceProfiler.snapshot();
        y = status(y, "superoptimizer.profile.fps", format("%.1f FPS", s.fps()), Impact.BEST);
        y = status(y, "superoptimizer.profile.frame", format("%.2f ms", s.frameMs()), Impact.BEST);
        y = status(y, "superoptimizer.profile.one_low", format("%.1f FPS", s.onePercentLow()), Impact.BEST);
        y = status(y, "superoptimizer.profile.cpu", formatPercent(s.cpuLoad()), Impact.MEDIUM);
        y = status(y, "superoptimizer.profile.gpu", formatPercent(s.gpuUtilization()), Impact.MEDIUM);
        y = status(y, "superoptimizer.profile.gc", format("%.1f ms", s.gcPauseMs()), Impact.MEDIUM);
        y = status(y, "superoptimizer.profile.heap", format("%.0f / %.0f MiB", s.heapUsedMb(), s.heapMaxMb()), Impact.MEDIUM);
        y = status(y, "superoptimizer.profile.bottleneck", s.bottleneck(), Impact.BEST);

        y = graph(y);

        y = action(y, "superoptimizer.action.benchmark_before", "superoptimizer.desc.benchmark_before",
                q -> PerformanceProfiler.startBenchmark(configPath()), Impact.BEST);
        y = action(y, "superoptimizer.action.benchmark_after", "superoptimizer.desc.benchmark_after",
                q -> PerformanceProfiler.finishBenchmark(configPath()), Impact.BEST);
        y = action(y, "superoptimizer.action.reset_profiler", "superoptimizer.desc.reset_profiler",
                q -> PerformanceProfiler.clearHistory(), Impact.LOW);

        PerformanceProfiler.Benchmark before = PerformanceProfiler.before();
        PerformanceProfiler.Benchmark after = PerformanceProfiler.after();
        if (before != null) y = status(y, "superoptimizer.profile.before", format("%.1f FPS / %.1f 1%% low", before.fps(), before.onePercentLow()), Impact.MEDIUM);
        if (after != null) y = status(y, "superoptimizer.profile.after", format("%.1f FPS / %.1f 1%% low", after.fps(), after.onePercentLow()), Impact.MEDIUM);
        return y;
    }

    private int adaptive(int y) {
        y = header(y, "superoptimizer.category.adaptive", "superoptimizer.page.adaptive.desc");
        y = toggle(y, "superoptimizer.option.adaptive", "superoptimizer.desc.adaptive", () -> config.adaptivePerformance, v -> {
            config.adaptivePerformance = v; markCustom(); AdaptivePerformanceController.init(config);
        }, Impact.BEST);
        y = toggle(y, "superoptimizer.option.adaptive_autopreset", "superoptimizer.desc.adaptive_autopreset",
                () -> config.adaptiveAutoPreset, v -> { config.adaptiveAutoPreset = v; markCustom(); }, Impact.BEST);
        y = status(y, "superoptimizer.adaptive.scale", format("x%.2f", AdaptivePerformanceController.qualityScale()), Impact.BEST);
        y = value(y, "superoptimizer.option.target_fps", "superoptimizer.desc.target_fps", () -> (int)config.adaptiveTargetFps,
                30, 240, 10, v -> { config.adaptiveTargetFps = v; markCustom(); }, Impact.BEST);
        y = value(y, "superoptimizer.option.drop_threshold", "superoptimizer.desc.drop_threshold", () -> (int)config.adaptiveDropThresholdMs,
                17, 80, 1, v -> { config.adaptiveDropThresholdMs = v; markCustom(); }, Impact.BEST);
        y = value(y, "superoptimizer.option.recover_threshold", "superoptimizer.desc.recover_threshold", () -> (int)config.adaptiveRecoverThresholdMs,
                10, 60, 1, v -> { config.adaptiveRecoverThresholdMs = v; markCustom(); }, Impact.BEST);
        y = value(y, "superoptimizer.option.hysteresis", "superoptimizer.desc.hysteresis", () -> config.adaptiveHysteresisSeconds,
                1, 15, 1, v -> { config.adaptiveHysteresisSeconds = v; markCustom(); }, Impact.BEST);
        y = value(y, "superoptimizer.option.cooldown", "superoptimizer.desc.cooldown", () -> config.adaptiveChangeCooldownSeconds,
                1, 30, 1, v -> { config.adaptiveChangeCooldownSeconds = v; markCustom(); }, Impact.BEST);
        y = value(y, "superoptimizer.option.adaptive_step", "superoptimizer.desc.adaptive_step", () -> config.adaptiveStepPercent,
                1, 30, 1, v -> { config.adaptiveStepPercent = v; markCustom(); }, Impact.MEDIUM);
        y = toggle(y, "superoptimizer.option.unfocused_cap", "superoptimizer.desc.unfocused_cap",
                () -> config.adaptiveUnfocusedFpsCap, v -> { config.adaptiveUnfocusedFpsCap = v; markCustom(); }, Impact.MEDIUM);
        y = value(y, "superoptimizer.option.unfocused_fps", "superoptimizer.desc.unfocused_fps",
                () -> config.unfocusedFps, 5, 30, 1, v -> { config.unfocusedFps = v; markCustom(); }, Impact.MEDIUM);
        return y;
    }

    private int particles(int y) {
        y = header(y, "superoptimizer.category.particles", "superoptimizer.page.particles.desc");
        y = toggle(y, "superoptimizer.option.particle_optimization", "superoptimizer.desc.particle_optimization",
                () -> config.particleOptimization, v -> { config.particleOptimization = v; markCustom(); }, Impact.BEST);
        y = value(y, "superoptimizer.option.max_particles", "superoptimizer.desc.max_particles",
                () -> config.maxParticles, 64, 16384, 256, v -> { config.maxParticles = v; markCustom(); }, Impact.BEST);
        y = value(y, "superoptimizer.option.particle_distance", "superoptimizer.desc.particle_distance",
                () -> config.particleDistance, 8, 256, 8, v -> { config.particleDistance = v; markCustom(); AdaptivePerformanceController.init(config); }, Impact.BEST);
        y = toggle(y, "superoptimizer.option.particle_distance_culling", "superoptimizer.desc.particle_distance_culling",
                () -> config.particleDistanceCulling, v -> { config.particleDistanceCulling = v; markCustom(); }, Impact.BEST);
        y = toggle(y, "superoptimizer.option.particle_adaptive", "superoptimizer.desc.particle_adaptive",
                () -> config.particleAdaptive, v -> { config.particleAdaptive = v; markCustom(); }, Impact.BEST);
        y = toggle(y, "superoptimizer.option.particle_pool", "superoptimizer.desc.particle_pool",
                () -> config.particlePool, v -> { config.particlePool = v; markCustom(); }, Impact.MEDIUM);
        y = status(y, "superoptimizer.profile.particle_count", ParticleOptimizer.currentCount() + " активных", Impact.BEST);
        y = status(y, "superoptimizer.profile.particle_rejected", ParticleOptimizer.rejectedDistance() + " по дистанции • " + ParticleOptimizer.rejectedCount() + " по лимиту", Impact.MEDIUM);
        return y;
    }

    private int entities(int y) {
        y = header(y, "superoptimizer.category.entities", "superoptimizer.page.entities.desc");
        y = toggle(y, "superoptimizer.option.entity", "superoptimizer.desc.entity", () -> config.entityCulling, v -> { config.entityCulling = v; markCustom(); }, Impact.BEST);
        y = toggle(y, "superoptimizer.option.entity_distance_culling", "superoptimizer.desc.entity_distance_culling",
                () -> config.entityDistanceCulling, v -> { config.entityDistanceCulling = v; markCustom(); }, Impact.BEST);
        y = value(y, "superoptimizer.option.entity_distance", "superoptimizer.desc.entity_distance",
                () -> config.entityRenderDistance, 32, 512, 16, v -> { config.entityRenderDistance = v; markCustom(); AdaptivePerformanceController.init(config); }, Impact.BEST);
        y = toggle(y, "superoptimizer.option.entity_lod", "superoptimizer.desc.entity_lod",
                () -> config.entityLod, v -> { config.entityLod = v; markCustom(); }, Impact.BEST);
        y = value(y, "superoptimizer.option.entity_lod_distance", "superoptimizer.desc.entity_lod_distance",
                () -> config.entityLodDistance, 32, 256, 8, v -> { config.entityLodDistance = v; markCustom(); }, Impact.MEDIUM);
        y = toggle(y, "superoptimizer.option.hide_names", "superoptimizer.desc.hide_names",
                () -> config.hideDistantNames, v -> { config.hideDistantNames = v; markCustom(); }, Impact.LOW);
        y = toggle(y, "superoptimizer.option.hide_shadows", "superoptimizer.desc.hide_shadows",
                () -> config.hideDistantShadows, v -> { config.hideDistantShadows = v; markCustom(); }, Impact.MEDIUM);
        y = toggle(y, "superoptimizer.option.frame_culling", "superoptimizer.desc.frame_culling",
                () -> config.frameCulling, v -> { config.frameCulling = v; markCustom(); }, Impact.MEDIUM);
        y = toggle(y, "superoptimizer.option.armor_stand_culling", "superoptimizer.desc.armor_stand_culling",
                () -> config.armorStandCulling, v -> { config.armorStandCulling = v; markCustom(); }, Impact.MEDIUM);
        return y;
    }

    private int blockEntities(int y) {
        y = header(y, "superoptimizer.category.block_entities", "superoptimizer.page.block_entities.desc");
        y = toggle(y, "superoptimizer.option.block_entity", "superoptimizer.desc.block_entity",
                () -> config.blockEntityCulling, v -> { config.blockEntityCulling = v; markCustom(); }, Impact.BEST);
        y = toggle(y, "superoptimizer.option.sign_culling", "superoptimizer.desc.sign_culling",
                () -> config.signCulling, v -> { config.signCulling = v; markCustom(); }, Impact.MEDIUM);
        y = toggle(y, "superoptimizer.option.chest_culling", "superoptimizer.desc.chest_culling",
                () -> config.chestCulling, v -> { config.chestCulling = v; markCustom(); }, Impact.MEDIUM);
        y = toggle(y, "superoptimizer.option.hopper_culling", "superoptimizer.desc.hopper_culling",
                () -> config.hopperCulling, v -> { config.hopperCulling = v; markCustom(); }, Impact.LOW);
        y = toggle(y, "superoptimizer.option.pause_motion", "superoptimizer.desc.pause_motion",
                () -> config.pauseDuringCameraMotion, v -> { config.pauseDuringCameraMotion = v; markCustom(); }, Impact.MEDIUM);
        y = toggle(y, "superoptimizer.option.near_protection", "superoptimizer.desc.skip_near",
                () -> config.skipNearEntityCulling, v -> { config.skipNearEntityCulling = v; markCustom(); }, Impact.MEDIUM);
        y = value(y, "superoptimizer.option.near_distance", "superoptimizer.desc.near_distance",
                () -> config.nearEntityDistance, 0, 64, 4, v -> { config.nearEntityDistance = v; markCustom(); }, Impact.LOW);
        return y;
    }

    private int chunks(int y) {
        y = header(y, "superoptimizer.category.chunks", "superoptimizer.page.chunks.desc");
        y = toggle(y, "superoptimizer.option.smart_frame_budget", "superoptimizer.desc.smart_frame_budget",
                () -> config.smartFrameBudget, v -> { config.smartFrameBudget = v; markCustom(); }, Impact.BEST);
        y = value(y, "superoptimizer.option.frame_budget_ms", "superoptimizer.desc.frame_budget_ms",
                () -> config.frameBudgetMs, 1, 12, 1, v -> { config.frameBudgetMs = v; markCustom(); }, Impact.BEST);
        y = toggle(y, "superoptimizer.option.chunk_dedupe", "superoptimizer.desc.chunk_dedupe",
                () -> config.chunkRebuildDeduplication, v -> { config.chunkRebuildDeduplication = v; markCustom(); }, Impact.BEST);
        y = toggle(y, "superoptimizer.option.chunk_scheduler", "superoptimizer.desc.chunk_scheduler",
                () -> config.adaptiveChunkScheduler, v -> { config.adaptiveChunkScheduler = v; markCustom(); }, Impact.BEST);
        y = toggle(y, "superoptimizer.option.predictive_visibility", "superoptimizer.desc.predictive_visibility",
                () -> config.predictiveVisibility, v -> { config.predictiveVisibility = v; markCustom(); }, Impact.MEDIUM);
        y = value(y, "superoptimizer.option.chunk_queue_limit", "superoptimizer.desc.chunk_queue_limit",
                () -> config.chunkQueueLimit, 16, 512, 16, v -> { config.chunkQueueLimit = v; markCustom(); }, Impact.MEDIUM);
        y = value(y, "superoptimizer.option.chunk_upload_budget", "superoptimizer.desc.chunk_upload_budget",
                () -> config.chunkUploadBudgetMs, 0, 10, 1, v -> { config.chunkUploadBudgetMs = v; markCustom(); }, Impact.MEDIUM);
        y = toggle(y, "superoptimizer.option.task_backpressure", "superoptimizer.desc.task_backpressure",
                () -> config.taskBackpressure, v -> { config.taskBackpressure = v; markCustom(); }, Impact.BEST);
        y = status(y, "superoptimizer.profile.chunk_queue", ChunkTaskController.queueSize() + " в контроллере • " + ChunkTaskController.deduplicated() + " дедуплицировано", Impact.MEDIUM);
        return y;
    }

    private int memory(int y) {
        y = header(y, "superoptimizer.category.memory", "superoptimizer.page.memory.desc");
        y = toggle(y, "superoptimizer.option.memory_pressure", "superoptimizer.desc.memory_pressure",
                () -> config.memoryPressureController, v -> { config.memoryPressureController = v; markCustom(); }, Impact.BEST);
        y = value(y, "superoptimizer.option.cache_budget", "superoptimizer.desc.cache_budget",
                () -> config.cacheBudgetMb, 64, 2048, 64, v -> { config.cacheBudgetMb = v; markCustom(); }, Impact.MEDIUM);
        y = toggle(y, "superoptimizer.option.cache_admission", "superoptimizer.desc.cache_admission",
                () -> config.cacheAdmissionPolicy, v -> { config.cacheAdmissionPolicy = v; markCustom(); }, Impact.MEDIUM);
        y = toggle(y, "superoptimizer.option.resource_reload_diff", "superoptimizer.desc.resource_reload_diff",
                () -> config.resourceReloadDiff, v -> { config.resourceReloadDiff = v; markCustom(); }, Impact.MEDIUM);
        y = toggle(y, "superoptimizer.option.object_pooling", "superoptimizer.desc.object_pooling",
                () -> config.objectPooling, v -> { config.objectPooling = v; markCustom(); }, Impact.MEDIUM);
        y = toggle(y, "superoptimizer.option.model_cache", "superoptimizer.desc.model_cache",
                () -> config.modelCache, v -> { config.modelCache = v; markCustom(); }, Impact.MEDIUM);
        y = toggle(y, "superoptimizer.option.cache_cleanup", "superoptimizer.desc.cache_cleanup",
                () -> config.resourceCacheCleanup, v -> { config.resourceCacheCleanup = v; markCustom(); }, Impact.MEDIUM);
        y = toggle(y, "superoptimizer.option.measured_gc", "superoptimizer.desc.measured_gc",
                () -> config.measuredGcControl, v -> { config.measuredGcControl = v; markCustom(); }, Impact.MEDIUM);
        return y;
    }

    private int loading(int y) {
        y = header(y, "superoptimizer.category.loading", "superoptimizer.page.loading.desc");
        y = toggle(y, "superoptimizer.option.async_resources", "superoptimizer.desc.async_resources",
                () -> config.asyncResourcePreparation, v -> { config.asyncResourcePreparation = v; markCustom(); }, Impact.BEST);
        y = toggle(y, "superoptimizer.option.model_build_cache", "superoptimizer.desc.model_build_cache",
                () -> config.modelBuildCache, v -> { config.modelBuildCache = v; markCustom(); }, Impact.MEDIUM);
        y = toggle(y, "superoptimizer.option.slow_disk", "superoptimizer.desc.slow_disk",
                () -> config.slowDiskDiagnostics, v -> { config.slowDiskDiagnostics = v; markCustom(); }, Impact.MEDIUM);
        y = toggle(y, "superoptimizer.option.world_prefetch", "superoptimizer.desc.world_prefetch",
                () -> config.worldDataPrefetch, v -> { config.worldDataPrefetch = v; markCustom(); }, Impact.MEDIUM);
        y = toggle(y, "superoptimizer.option.safe_save", "superoptimizer.desc.safe_save",
                () -> config.safeAsyncSave, v -> { config.safeAsyncSave = v; markCustom(); }, Impact.BEST);
        y = toggle(y, "superoptimizer.option.background", "superoptimizer.desc.background",
                () -> config.backgroundTasks, v -> { config.backgroundTasks = v; markCustom(); SuperOptimizerClient.applyConfig(); }, Impact.MEDIUM);
        y = toggle(y, "superoptimizer.option.shader_scan_async", "superoptimizer.desc.shader_scan_async",
                () -> config.shaderScanAsync, v -> { config.shaderScanAsync = v; markCustom(); }, Impact.LOW);
        return y;
    }

    private int hardware(int y) {
        y = header(y, "superoptimizer.category.hardware", "superoptimizer.page.hardware.desc");
        y = toggle(y, "superoptimizer.option.hardware_profiles", "superoptimizer.desc.hardware_profiles",
                () -> config.hardwareAwareProfiles, v -> { config.hardwareAwareProfiles = v; markCustom(); }, Impact.BEST);
        y = toggle(y, "superoptimizer.option.gpu_state_optimizer", "superoptimizer.desc.gpu_state_optimizer",
                () -> config.gpuStateOptimizer, v -> { config.gpuStateOptimizer = v; markCustom(); }, Impact.MEDIUM);
        y = status(y, "superoptimizer.hardware.cpu", PerformanceProfiler.currentCpu() < 0 ? "UNKNOWN" : format("%.0f%%", PerformanceProfiler.currentCpu()), Impact.MEDIUM);
        y = status(y, "superoptimizer.hardware.gpu", PerformanceProfiler.currentGpu() < 0 ? "UNKNOWN" : format("%.0f%%", PerformanceProfiler.currentGpu()), Impact.MEDIUM);
        y = toggle(y, "superoptimizer.option.thermal", "superoptimizer.desc.thermal",
                () -> config.thermalFriendlyMode, v -> { config.thermalFriendlyMode = v; markCustom(); }, Impact.BEST);
        y = value(y, "superoptimizer.option.workers", "superoptimizer.desc.workers",
                () -> config.workerThreads, 1, Math.max(1, Math.min(16, Runtime.getRuntime().availableProcessors())), 1,
                v -> { config.workerThreads = v; markCustom(); SuperOptimizerClient.applyConfig(); }, Impact.MEDIUM);
        y = value(y, "superoptimizer.option.reserved", "superoptimizer.desc.reserved",
                () -> config.reservedCores, 0, Math.max(0, Runtime.getRuntime().availableProcessors() - 1), 1,
                v -> { config.reservedCores = v; markCustom(); SuperOptimizerClient.applyConfig(); }, Impact.MEDIUM);
        return y;
    }

    private int resources(int y) {
        y = header(y, "superoptimizer.category.resources", "superoptimizer.page.resources.desc");
        y = toggle(y, "superoptimizer.option.model_cache", "superoptimizer.desc.model_cache",
                () -> config.modelCache, v -> { config.modelCache = v; markCustom(); }, Impact.MEDIUM);
        y = toggle(y, "superoptimizer.option.resource_reload_diff", "superoptimizer.desc.resource_reload_diff",
                () -> config.resourceReloadDiff, v -> { config.resourceReloadDiff = v; markCustom(); }, Impact.MEDIUM);
        y = toggle(y, "superoptimizer.option.text_cache", "superoptimizer.desc.text_cache",
                () -> config.textLayoutCache, v -> { config.textLayoutCache = v; markCustom(); }, Impact.LOW);
        y = toggle(y, "superoptimizer.option.async_resources", "superoptimizer.desc.async_resources",
                () -> config.asyncResourcePreparation, v -> { config.asyncResourcePreparation = v; markCustom(); }, Impact.BEST);
        y = toggle(y, "superoptimizer.option.weather_lod", "superoptimizer.desc.weather_lod",
                () -> config.weatherLod, v -> { config.weatherLod = v; markCustom(); }, Impact.UNKNOWN);
        return y;
    }

    private int world(int y) {
        y = header(y, "superoptimizer.category.world", "superoptimizer.page.world.desc");
        y = toggle(y, "superoptimizer.option.world_prefetch", "superoptimizer.desc.world_prefetch",
                () -> config.worldDataPrefetch, v -> { config.worldDataPrefetch = v; markCustom(); }, Impact.MEDIUM);
        y = toggle(y, "superoptimizer.option.pathfinding_budget", "superoptimizer.desc.pathfinding_budget",
                () -> config.backgroundPathfindingBudget, v -> { config.backgroundPathfindingBudget = v; markCustom(); }, Impact.UNKNOWN);
        y = status(y, "superoptimizer.world.networking", "client-side policy only", Impact.UNKNOWN);
        y = status(y, "superoptimizer.world.redstone", "server logic unchanged", Impact.UNKNOWN);
        y = toggle(y, "superoptimizer.option.task_backpressure", "superoptimizer.desc.task_backpressure",
                () -> config.taskBackpressure, v -> { config.taskBackpressure = v; markCustom(); }, Impact.BEST);
        return y;
    }

    private int javaRuntime(int y) {
        y = header(y, "superoptimizer.category.java", "superoptimizer.page.java.desc");
        Runtime rt = Runtime.getRuntime();
        double used = (rt.totalMemory() - rt.freeMemory()) / 1048576.0;
        double max = rt.maxMemory() / 1048576.0;
        y = status(y, "superoptimizer.java.heap", format("%.0f / %.0f MiB", used, max), Impact.MEDIUM);
        y = toggle(y, "superoptimizer.option.measured_gc", "superoptimizer.desc.measured_gc",
                () -> config.measuredGcControl, v -> { config.measuredGcControl = v; markCustom(); }, Impact.BEST);
        y = value(y, "superoptimizer.option.cache_budget", "superoptimizer.desc.cache_budget",
                () -> config.cacheBudgetMb, 64, 4096, 64, v -> { config.cacheBudgetMb = v; markCustom(); }, Impact.MEDIUM);
        y = status(y, "superoptimizer.java.runtime", System.getProperty("java.version"), Impact.LOW);
        return y;
    }

    private int lightingSound(int y) {
        y = header(y, "superoptimizer.category.lighting_sound", "superoptimizer.page.lighting_sound.desc");
        y = toggle(y, "superoptimizer.option.lighting_diagnostics", "superoptimizer.desc.lighting_diagnostics",
                () -> config.lightingDiagnostics, v -> { config.lightingDiagnostics = v; markCustom(); }, Impact.MEDIUM);
        y = toggle(y, "superoptimizer.option.sound_limit", "superoptimizer.desc.sound_limit",
                () -> config.soundLimit, v -> { config.soundLimit = v; markCustom(); }, Impact.LOW);
        y = value(y, "superoptimizer.option.max_sounds", "superoptimizer.desc.max_sounds",
                () -> config.maxConcurrentSounds, 8, 256, 8, v -> { config.maxConcurrentSounds = v; markCustom(); }, Impact.LOW);
        return y;
    }

    private int compatibility(int y) {
        y = header(y, "superoptimizer.category.compatibility", "superoptimizer.page.compatibility.desc");
        y = toggle(y, "superoptimizer.option.sodium_compat", "superoptimizer.desc.sodium_compat",
                () -> config.sodiumCompatibility, v -> { config.sodiumCompatibility = v; markCustom(); }, Impact.BEST);
        y = toggle(y, "superoptimizer.option.iris_compat", "superoptimizer.desc.iris_compat",
                () -> config.irisCompatibility, v -> { config.irisCompatibility = v; markCustom(); }, Impact.BEST);
        y = toggle(y, "superoptimizer.option.iris_lock", "superoptimizer.desc.iris_lock",
                () -> config.disableCullingWithIris, v -> { config.disableCullingWithIris = v; markCustom(); }, Impact.BEST);
        y = toggle(y, "superoptimizer.option.entity_culling_lock", "superoptimizer.desc.entity_culling_lock",
                () -> config.disableCullingWithEntityCullingMod, v -> { config.disableCullingWithEntityCullingMod = v; markCustom(); }, Impact.BEST);
        y = status(y, "superoptimizer.compat.sodium", ExternalModBridge.loaded("sodium") ? ExternalModBridge.version("sodium") : "не установлен", Impact.BEST);
        y = status(y, "superoptimizer.compat.iris", ExternalModBridge.loaded("iris") ? ExternalModBridge.version("iris") : "не установлен", Impact.BEST);
        y = status(y, "superoptimizer.compat.entity_culling", ExternalModBridge.loaded("entityculling") ? ExternalModBridge.version("entityculling") : "не установлен", Impact.BEST);
        y = action(y, "superoptimizer.action.open_sodium", "superoptimizer.desc.open_external",
                q -> ExternalModBridge.openConfig("sodium", this), Impact.MEDIUM);
        y = action(y, "superoptimizer.action.open_iris", "superoptimizer.desc.open_external",
                q -> ExternalModBridge.openConfig("iris", this), Impact.MEDIUM);
        y = action(y, "superoptimizer.action.open_entity_culling", "superoptimizer.desc.open_external",
                q -> ExternalModBridge.openConfig("entityculling", this), Impact.MEDIUM);
        return y;
    }

    private int diagnostics(int y) {
        y = header(y, "superoptimizer.category.diagnostics", "superoptimizer.page.diagnostics.desc");
        y = toggle(y, "superoptimizer.option.self_healing", "superoptimizer.desc.self_healing",
                () -> config.selfHealing, v -> { config.selfHealing = v; markCustom(); }, Impact.BEST);
        y = value(y, "superoptimizer.option.failure_threshold", "superoptimizer.desc.failure_threshold",
                () -> config.selfHealingFailureThreshold, 1, 10, 1, v -> { config.selfHealingFailureThreshold = v; markCustom(); }, Impact.MEDIUM);
        y = toggle(y, "superoptimizer.option.rollback", "superoptimizer.desc.rollback",
                () -> config.configurationRollback, v -> { config.configurationRollback = v; markCustom(); }, Impact.MEDIUM);
        y = toggle(y, "superoptimizer.option.logging", "superoptimizer.desc.file_logging",
                () -> config.fileLogging, v -> { config.fileLogging = v; markCustom(); }, Impact.LOW);
        y = status(y, "superoptimizer.diag.entity", CullingContext.entityCulled() + " / " + CullingContext.entityChecks(), Impact.MEDIUM);
        y = status(y, "superoptimizer.diag.block_entity", CullingContext.blockEntityCulled() + " / " + CullingContext.blockEntityChecks(), Impact.MEDIUM);
        y = status(y, "superoptimizer.diag.entity_lod", CullingContext.entityLodApplied() + " LOD state changes", Impact.MEDIUM);
        y = status(y, "superoptimizer.diag.particles", ParticleOptimizer.rejectedDistance() + " distance / " + ParticleOptimizer.rejectedCount() + " cap", Impact.MEDIUM);
        y = status(y, "superoptimizer.diag.chunk_queue", ChunkTaskController.queueSize() + " queued / " + ChunkTaskController.rejected() + " rejected", Impact.MEDIUM);
        y = action(y, "superoptimizer.action.clear_logs", "superoptimizer.desc.clear_logs", q -> SuperOptimizerLog.clear(), Impact.LOW);
        y = action(y, "superoptimizer.action.reset_stats", "superoptimizer.desc.reset_stats", q -> {
            CullingContext.resetStats();
            ParticleOptimizer.resetStats();
        }, Impact.LOW);
        y = action(y, "superoptimizer.action.rollback", "superoptimizer.desc.rollback", q -> SelfHealingManager.rollback(), Impact.MEDIUM);
        return y;
    }

    private int experimental(int y) {
        y = header(y, "superoptimizer.category.experimental", "superoptimizer.page.experimental.desc");
        y = toggle(y, "superoptimizer.option.animation_budget", "superoptimizer.desc.animation_budget",
                () -> config.animationWorkBudget, v -> { config.animationWorkBudget = v; markCustom(); }, Impact.UNKNOWN);
        y = toggle(y, "superoptimizer.option.weather_lod", "superoptimizer.desc.weather_lod",
                () -> config.weatherLod, v -> { config.weatherLod = v; markCustom(); }, Impact.UNKNOWN);
        y = value(y, "superoptimizer.option.weather_distance", "superoptimizer.desc.weather_distance",
                () -> config.weatherDistance, 16, 256, 16, v -> { config.weatherDistance = v; markCustom(); }, Impact.LOW);
        y = toggle(y, "superoptimizer.option.text_cache", "superoptimizer.desc.text_cache",
                () -> config.textLayoutCache, v -> { config.textLayoutCache = v; markCustom(); }, Impact.LOW);
        y = toggle(y, "superoptimizer.option.ui_cache", "superoptimizer.desc.ui_cache",
                () -> config.uiRenderCache, v -> { config.uiRenderCache = v; markCustom(); }, Impact.UNKNOWN);
        y = toggle(y, "superoptimizer.option.pathfinding_budget", "superoptimizer.desc.pathfinding_budget",
                () -> config.backgroundPathfindingBudget, v -> { config.backgroundPathfindingBudget = v; markCustom(); }, Impact.UNKNOWN);
        y = toggle(y, "superoptimizer.option.sound_limit", "superoptimizer.desc.sound_limit",
                () -> config.soundLimit, v -> { config.soundLimit = v; markCustom(); }, Impact.LOW);
        y = value(y, "superoptimizer.option.max_sounds", "superoptimizer.desc.max_sounds",
                () -> config.maxConcurrentSounds, 8, 256, 8, v -> { config.maxConcurrentSounds = v; markCustom(); }, Impact.LOW);
        return y;
    }

    private int presets(int y) {
        y = header(y, "superoptimizer.category.presets", "superoptimizer.page.presets.desc");
        y = action(y, "superoptimizer.preset.light", "superoptimizer.desc.preset.light",
                q -> applyProfile(SuperOptimizerClient.Preset.LIGHT), Impact.MEDIUM);
        y = action(y, "superoptimizer.preset.balanced", "superoptimizer.desc.preset.balanced",
                q -> applyProfile(SuperOptimizerClient.Preset.BALANCED), Impact.BEST);
        y = action(y, "superoptimizer.preset.advanced", "superoptimizer.desc.preset.advanced",
                q -> applyProfile(SuperOptimizerClient.Preset.ADVANCED), Impact.BEST);
        y = action(y, "superoptimizer.preset.microwave", "superoptimizer.desc.preset.microwave",
                q -> applyProfile(SuperOptimizerClient.Preset.MICROWAVE), Impact.BEST);
        return y;
    }

    private int external(String modId, int y) {
        String key = modId.equals("sodium") ? "superoptimizer.integration.sodium"
                : modId.equals("iris") ? "superoptimizer.integration.iris"
                : "superoptimizer.integration.entity_culling";
        y = header(y, key, "superoptimizer.page.external.desc");

        boolean loaded = ExternalModBridge.loaded(modId);
        y = status(y, "superoptimizer.external.detected", loaded
                ? "Установлен • " + ExternalModBridge.version(modId)
                : "Не установлен", loaded ? Impact.BEST : Impact.UNKNOWN);

        y = action(y, loaded ? "superoptimizer.action.open_external" : "superoptimizer.action.external_unavailable",
                "superoptimizer.desc.open_external",
                q -> { if (loaded) ExternalModBridge.openConfig(modId, this); }, loaded ? Impact.MEDIUM : Impact.UNKNOWN);

        if ("iris".equals(modId)) {
            y = status(y, "superoptimizer.shaders.state", IrisBridge.shadersInUse() ? "шейдер-пак активен" : "шейдер-пак не используется",
                    Impact.BEST);
        }
        return y;
    }

    private int header(int y, String titleKey, String descKey) {
        return y;
    }

    private int profileRow(int y) {
        int current = profileIndex();
        String name = profileName();
        Button b = invisible(Component.literal(name), q -> {
            SuperOptimizerClient.Preset[] order = {
                    SuperOptimizerClient.Preset.LIGHT,
                    SuperOptimizerClient.Preset.BALANCED,
                    SuperOptimizerClient.Preset.ADVANCED,
                    SuperOptimizerClient.Preset.MICROWAVE
            };
            int next = current < 0 ? 0 : (current + 1) % order.length;
            applyProfile(order[next]);
        }, mainX, y, mainW, 34);
        b.setTooltip(Tooltip.create(Component.translatable("superoptimizer.desc.profile.current")));
        b.setTooltipDelay(Duration.ofMillis(200));
        rows.add(new Row(b, "superoptimizer.profile.current", "superoptimizer.desc.profile.current", null, () -> current, y, Impact.BEST));
        return y + 38;
    }

    private int toggle(int y, String key, String desc, BooleanSupplier getter, Consumer<Boolean> setter, Impact impact) {
        Button b = invisible(Component.translatable(key), q -> {
            setter.accept(!getter.getAsBoolean());
        }, mainX, y, mainW, 32);
        setupTooltip(b, desc);
        rows.add(new Row(b, key, desc, getter, () -> 0, y, impact));
        return y + 34;
    }

    private int value(int y, String key, String desc, IntSupplier getter, int min, int max, int step, Consumer<Integer> setter, Impact impact) {
        Button b = invisible(Component.translatable(key), q -> {
            int value = getter.getAsInt() + step;
            if (value > max) value = min;
            setter.accept(value);
        }, mainX, y, mainW, 32);
        setupTooltip(b, desc);
        rows.add(new Row(b, key, desc, null, getter, y, impact));
        return y + 34;
    }

    private int status(int y, String key, String value, Impact impact) {
        Button b = invisible(Component.translatable(key), q -> {}, mainX, y, mainW, 32);
        b.active = false;
        rows.add(new Row(b, key, key, null, () -> 0, y, impact));
        // Store the visible value in an auxiliary message.
        b.setMessage(Component.translatable(key, value));
        return y + 34;
    }

    private int action(int y, String key, String desc, Consumer<Button> action, Impact impact) {
        Button b = invisible(Component.translatable(key), action, mainX, y, mainW, 32);
        setupTooltip(b, desc);
        actionButtons.add(b);
        rows.add(new Row(b, key, desc, null, () -> 0, y, impact));
        return y + 34;
    }

    private int graph(int y) {
        return y + 94;
    }

    private void applyProfile(SuperOptimizerClient.Preset preset) {
        SuperOptimizerClient.applyPreset(preset);
        config.activePreset = preset.name();
        PerformanceProfiler.configure(config);
        AdaptivePerformanceController.init(config);
        init();
    }

    private int profileIndex() {
        return switch (config.activePreset == null ? "" : config.activePreset) {
            case "LIGHT" -> 0;
            case "BALANCED" -> 1;
            case "ADVANCED" -> 2;
            case "MICROWAVE" -> 3;
            default -> -1;
        };
    }

    private String profileName() {
        return switch (profileIndex()) {
            case 0 -> "Лёгкий";
            case 1 -> "Сбалансированный";
            case 2 -> "Максимальный FPS";
            case 3 -> "Микроволновка";
            default -> "Пользовательский";
        };
    }

    private void markCustom() {
        config.activePreset = "CUSTOM";
        config.save(configPath());
    }

    private Path configPath() {
        return minecraft.gameDirectory.toPath().resolve("config");
    }

    private Button invisible(Component text, Consumer<Button> action, int x, int y, int w, int h) {
        Button b = Button.builder(text, q -> action.accept(q))
                .bounds(x, y, Math.max(1, w), h)
                .build();
        b.setAlpha(0f);
        addRenderableWidget(b);
        return b;
    }

    private void setupTooltip(Button b, String key) {
        b.setTooltip(Tooltip.create(Component.translatable(key)));
        b.setTooltipDelay(Duration.ofMillis(220));
    }

    private void applyScroll() {
        for (int i = 0; i < navButtons.size(); i++) {
            Button b = navButtons.get(i);
            b.setY(navTop + i * 36 - (int) navScroll);
            b.setVisible(b.getY() + b.getHeight() >= navTop && b.getY() <= contentBottom);
        }
        for (Row row : rows) {
            int y = row.y() - (int) contentScroll;
            row.hitbox().setY(y);
            row.hitbox().setVisible(y + row.hitbox().getHeight() >= contentTop + 28 && y <= contentBottom);
        }
        for (Button b : actionButtons) {
            if (!b.isVisible()) continue;
        }
    }

    @Override
    public void tick() {
        hovered = null;
        super.tick();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (mouseX >= navX && mouseX <= navX + navW && mouseY >= navTop && mouseY <= contentBottom) {
            navScroll = clamp(navScroll - vertical * 28, 0, navMaxScroll);
            applyScroll();
            return true;
        }
        if (mouseX >= mainX && mouseX <= mainX + mainW && mouseY >= contentTop && mouseY <= contentBottom) {
            contentScroll = clamp(contentScroll - vertical * 28, 0, contentMaxScroll);
            applyScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);

        g.fill(0, 0, width, height, 0xE306101A);
        g.fill(0, 0, width, 2, 0xFF20D7C7);

        drawHeader(g);
        drawSidebar(g, mouseX, mouseY);
        drawMain(g, mouseX, mouseY);

        if (contentMaxScroll > 0) drawScrollbar(g, mainX + mainW + 4, contentTop + 30, contentBottom - 4, contentScroll, contentMaxScroll);
        if (navMaxScroll > 0) drawScrollbar(g, navX + navW - 5, navTop, contentBottom, navScroll, navMaxScroll);
    }

    private void drawHeader(GuiGraphicsExtractor g) {
        g.text(font, Component.literal("SuperOptimizer"), 18, 10, 0xFFF3F7FF, true);
        g.text(font, Component.translatable("superoptimizer.gui.subtitle"), 18, 26, 0xFF8F9DB3, false);

        String backend = Minecraft.getInstance().options.preferredGraphicsBackend().toString();
        String right = "Графический API: " + backend;
        g.text(font, Component.literal(right), Math.max(mainX, width - font.width(right) - 18), 17, 0xFF9BA9BD, false);
        int lx = Math.max(mainX, width - 285);
        int ly = 31;
        g.text(font, Component.literal("BEST"), lx, ly, 0xFF20D7A4, true);
        g.text(font, Component.literal("MEDIUM"), lx + 45, ly, 0xFFFFD166, true);
        g.text(font, Component.literal("LOW"), lx + 103, ly, 0xFFFF6B6B, true);
        g.text(font, Component.literal("UNKNOWN"), lx + 135, ly, 0xFF8793A8, true);
    }

    private void drawSidebar(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.fill(navX, 46, navX + navW, height - 38, 0xB508111C);

        g.text(font, Component.literal("SuperOptimizer"), navX + 12, 51, 0xFFF0F5FC, true);
        g.text(font, Component.literal("0.3.0-alpha"), navX + 12, 65, 0xFF77869D, false);

        int modY = 82;
        drawMod(g, "Sodium", ExternalModBridge.loaded("sodium") ? ExternalModBridge.version("sodium") : "не установлен",
                modY, ExternalModBridge.loaded("sodium"));
        modY += 58;
        drawMod(g, "Iris", ExternalModBridge.loaded("iris") ? ExternalModBridge.version("iris") : "не установлен",
                modY, ExternalModBridge.loaded("iris"));
        modY += 58;
        drawMod(g, "Entity Culling", ExternalModBridge.loaded("entityculling") ? ExternalModBridge.version("entityculling") : "не установлен",
                modY, ExternalModBridge.loaded("entityculling"));

        for (int i = 0; i < navButtons.size(); i++) {
            Category c = Category.values()[i];
            int y = navTop + i * 36 - (int) navScroll;
            if (y + 32 < navTop || y > contentBottom) continue;
            boolean selected = c == category;
            boolean hoveredNav = mouseX >= navX + 4 && mouseX <= navX + navW - 4
                    && mouseY >= y && mouseY <= y + 32;
            g.fill(navX + 4, y, navX + navW - 4, y + 32,
                    selected ? 0xB91B3A48 : hoveredNav ? 0x80152230 : 0x590B1621);
            if (selected) g.fill(navX + 4, y, navX + 7, y + 32, 0xFF20D7C7);
            String label = translatableOrFallback(c.key, c.fallback);
            g.text(font, Component.literal(fit(label, navW - 26)), navX + 14, y + 10,
                    selected ? 0xFF20D7C7 : 0xFFC8D4E4, selected);
        }
    }

    private void drawMod(GuiGraphicsExtractor g, String name, String version, int y, boolean installed) {
        g.text(font, Component.literal(installed ? "●" : "○"), navX + 12, y + 4, installed ? 0xFF20D7A4 : 0xFF707E94, true);
        g.text(font, Component.literal(name), navX + 28, y, installed ? 0xFFE8F1FB : 0xFFABB7C7, true);
        g.text(font, Component.literal(version), navX + 28, y + 16, 0xFF718198, false);
    }

    private void drawMain(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.fill(mainX, contentTop, mainX + mainW, contentBottom, 0xB307121F);

        String title = translatableOrFallback(category.key, category.fallback);
        g.text(font, Component.literal(title), mainX + 14, contentTop + 10, 0xFFF0F5FC, true);

        if (category == Category.PROFILING) {
            drawProfilerGraph(g, mainX + 14, contentTop + 40, mainW - 28, 86);
        }

        for (Row row : rows) {
            int y = row.y() - (int) contentScroll;
            if (y < contentTop + 34 || y > contentBottom) continue;

            boolean h = mouseX >= mainX && mouseX <= mainX + mainW
                    && mouseY >= y && mouseY <= y + row.hitbox().getHeight();
            if (h) hovered = row;

            g.fill(mainX + 6, y, mainX + mainW - 6, y + 31, h ? 0xA21A2938 : 0x76101A27);
            if (row.impact() != null) {
                g.fill(mainX + 6, y, mainX + 8, y + 31, row.impact().color);
            }

            String label = row.hitbox().getMessage().getString();
            if (label.contains(" = ")) label = label.substring(0, label.indexOf(" = "));
            if (row.descKey().equals(row.labelKey()) && label.contains(": ")) {
                label = label.substring(0, label.indexOf(": "));
            } else if (label.contains(": ")) {
                int colon = label.indexOf(": ");
                String suffix = label.substring(colon + 2);
                if (suffix.equals("ВКЛ") || suffix.equals("ВЫКЛ")) label = label.substring(0, colon);
            }
            label = fit(label, mainW - 210);
            g.text(font, Component.literal(label), mainX + 16, y + 10, 0xFFDFE8F4, false);

            String value = rowValue(row);
            int rw = font.width(value);
            g.text(font, Component.literal(value), mainX + mainW - rw - 48, y + 10, 0xFFF2F6FA, hovered == row);

            if (row.impact() != null) {
                int pw = font.width(row.impact().label) + 10;
                int px = mainX + mainW - pw - 8;
                g.fill(px, y + 7, px + pw, y + 24, row.impact().color);
                g.text(font, Component.literal(row.impact().label), px + 5, y + 10, 0xFF07111A, true);
            }
        }

        if (hovered != null) drawHoverDescription(g, hovered);
    }

    private void drawProfilerGraph(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        double[] samples = PerformanceProfiler.frameHistory();
        g.fill(x, y, x + w, y + h, 0x7F0B1724);
        if (samples.length == 0) {
            g.text(font, Component.literal("Ожидание кадров…"), x + 10, y + 10, 0xFF8796AA, false);
            return;
        }

        double max = 16.7;
        for (double value : samples) max = Math.max(max, Math.min(100, value));
        int visible = Math.min(samples.length, Math.max(2, w - 8));
        int start = samples.length - visible;

        int previousX = x + 4;
        int previousY = y + h - 4 - (int)Math.round((samples[start] / max) * (h - 12));
        for (int i = 1; i < visible; i++) {
            int px = x + 4 + (i * (w - 8) / Math.max(1, visible - 1));
            double value = samples[start + i];
            int py = y + h - 4 - (int)Math.round((Math.min(max, value) / max) * (h - 12));
            drawLine(g, previousX, previousY, px, py, 0xFF20D7C7);
            previousX = px;
            previousY = py;
        }
        g.text(font, Component.literal("Время кадра"), x + 8, y + 6, 0xFF9BA8BA, false);
        g.text(font, Component.literal(String.format(Locale.ROOT, "%.2f ms", PerformanceProfiler.lastFrameMs())),
                x + w - 90, y + 6, 0xFFE7EEF7, true);
    }

    private void drawLine(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1);
        int steps = Math.max(1, dx);
        for (int i = 0; i <= steps; i++) {
            int x = x1 + (x2 - x1) * i / steps;
            int y = y1 + (y2 - y1) * i / steps;
            g.fill(x, y, x + 2, y + 2, color);
        }
    }

    private void drawHoverDescription(GuiGraphicsExtractor g, Row row) {
        String text = Component.translatable(row.descKey()).getString();
        int w = Math.min(420, Math.max(280, width / 3));
        int h = 46;
        int x = Math.min(width - w - 10, mainX + mainW + 8);
        int y = height - 92;

        g.fill(x, y, x + w, y + h, 0xEA111D2A);
        g.fill(x, y, x + 3, y + h, row.impact().color);
        g.text(font, Component.literal("Подсказка"), x + 10, y + 8, row.impact().color, true);
        g.text(font, Component.literal(fit(text, w - 22)), x + 10, y + 25, 0xFFCBD5E4, false);
    }

    private void drawScrollbar(GuiGraphicsExtractor g, int x, int top, int bottom, double pos, double max) {
        if (bottom <= top || max <= 0) return;
        int track = bottom - top;
        int thumb = Math.max(20, (int)(track * track / (track + max)));
        int y = top + (int)((track - thumb) * (pos / max));
        g.fill(x, top, x + 3, bottom, 0x551E2B3A);
        g.fill(x, y, x + 3, y + thumb, 0xFF20D7C7);
    }

    private String rowValue(Row row) {
        if (row.labelKey().equals("superoptimizer.profile.current")) return profileName();
        String rawMessage = row.hitbox().getMessage().getString();
        if (row.descKey().equals(row.labelKey()) && rawMessage.contains(": ")) {
            return rawMessage.substring(rawMessage.indexOf(": ") + 2);
        }
        if (row.bool() != null) return row.bool().getAsBoolean() ? "ВКЛ" : "ВЫКЛ";
        if (row.bool() != null && (row.labelKey().contains("enabled") || row.labelKey().contains("option"))) {
            String key = row.labelKey();
            if (!key.contains("profile") && !key.contains("profiling") && !key.contains("target")) return "ВЫКЛ";
        }
        int v = row.integer().getAsInt();
        if (row.labelKey().contains("fps")) return Integer.toString(v);
        if (row.labelKey().contains("distance")) return v + " бл.";
        if (row.labelKey().contains("budget") || row.labelKey().contains("threshold") || row.labelKey().contains("cooldown")) return v + " ms";
        if (row.labelKey().contains("hysteresis")) return v + " с";
        if (row.labelKey().contains("step")) return v + "%";
        if (row.labelKey().contains("cache_budget")) return v + " MiB";
        if (row.labelKey().contains("particles")) return Integer.toString(v);
        return Integer.toString(v);
    }

    private String translatableOrFallback(String key, String fallback) {
        String text = Component.translatable(key).getString();
        return text.equals(key) ? fallback : text;
    }

    private String format(String fmt, Object... args) {
        return String.format(Locale.ROOT, fmt, args);
    }

    private String formatPercent(double value) {
        return value < 0 ? "UNKNOWN" : String.format(Locale.ROOT, "%.0f%%", value);
    }

    private String fit(String value, int maxWidth) {
        if (font.width(value) <= maxWidth) return value;
        String s = value;
        while (s.length() > 1 && font.width(s + "...") > maxWidth) {
            s = s.substring(0, s.length() - 1);
        }
        return s + "...";
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    @Override
    public void onClose() {
        config.save(configPath());
        SuperOptimizerClient.applyConfig();
        minecraft.setScreenAndShow(parent);
    }
}