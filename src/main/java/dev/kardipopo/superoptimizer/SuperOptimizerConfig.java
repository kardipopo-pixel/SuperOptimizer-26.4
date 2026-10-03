package dev.kardipopo.superoptimizer;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * All optimizer controls live here. Runtime-only adaptive values are kept outside
 * this file so the adaptive controller cannot silently overwrite user settings.
 */
public final class SuperOptimizerConfig {
    public boolean enabled = true;

    // Profiling / diagnostics
    public boolean profilingEnabled = true;
    public int profilerHistoryFrames = 600;
    public boolean showTelemetry = true;
    public boolean saveBenchmarks = true;
    public boolean detectBottleneck = true;

    // Adaptive Performance
    public boolean adaptivePerformance = true;
    public double adaptiveTargetFps = 60.0;
    public double adaptiveDropThresholdMs = 25.0;
    public double adaptiveRecoverThresholdMs = 16.7;
    public int adaptiveHysteresisSeconds = 3;
    public int adaptiveChangeCooldownSeconds = 5;
    public int adaptiveStepPercent = 10;
    public boolean adaptiveAutoPreset = false;
    public boolean adaptiveUnfocusedFpsCap = true;
    public boolean thermalFriendlyMode = false;
    public boolean hardwareAwareProfiles = true;
    public boolean gpuStateOptimizer = true;
    public int unfocusedFps = 10;

    // Particles
    public boolean particleOptimization = true;
    public int maxParticles = 4096;
    public int particleDistance = 64;
    public boolean particleDistanceCulling = true;
    public boolean particleAdaptive = true;
    public boolean particlePool = true;

    // Entity rendering
    public boolean entityCulling = true;
    public boolean blockEntityCulling = false;
    public boolean entityDistanceCulling = true;
    public int entityRenderDistance = 160;
    public boolean entityLod = false;
    public int entityLodDistance = 96;
    public boolean hideDistantNames = false;
    public boolean hideDistantShadows = false;
    public boolean throttleDecorativeAnimation = false;

    // Individual entity / block-entity classes
    public boolean frameCulling = true;
    public boolean armorStandCulling = true;
    public boolean signCulling = true;
    public boolean chestCulling = true;
    public boolean hopperCulling = true;

    // Safe render behavior
    public boolean pauseDuringCameraMotion = true;
    public boolean skipNearEntityCulling = true;
    public int nearEntityDistance = 12;
    public boolean directionalEntityCulling = true;

    // Chunk/frame pacing
    public boolean smartFrameBudget = true;
    public int frameBudgetMs = 4;
    public boolean chunkRebuildDeduplication = true;
    public boolean adaptiveChunkScheduler = true;
    public boolean predictiveVisibility = true;
    public boolean frameTimeStabilizer = true;
    public int chunkQueueLimit = 128;
    public int chunkUploadBudgetMs = 2;
    public int backgroundTaskBudgetMs = 2;

    // Memory/resource cache
    public boolean memoryPressureController = true;
    public int cacheBudgetMb = 256;
    public boolean cacheAdmissionPolicy = true;
    public boolean resourceReloadDiff = true;
    public boolean objectPooling = true;
    public boolean modelCache = true;
    public boolean resourceCacheCleanup = true;
    public boolean measuredGcControl = true;

    // Loading / storage
    public boolean asyncResourcePreparation = true;
    public boolean modelBuildCache = true;
    public boolean slowDiskDiagnostics = true;
    public boolean worldDataPrefetch = true;
    public boolean taskBackpressure = true;
    public boolean safeAsyncSave = true;

    // Other rendering / game-side client optimizations
    public boolean animationWorkBudget = false;
    public boolean weatherLod = false;
    public int weatherDistance = 96;
    public boolean textLayoutCache = true;
    public boolean uiRenderCache = false;
    public boolean backgroundPathfindingBudget = true;
    public boolean soundLimit = false;
    public int maxConcurrentSounds = 64;
    public boolean lightingDiagnostics = true;

    // Compatibility
    public boolean disableCullingWithIris = true;
    public boolean disableCullingWithEntityCullingMod = true;
    public boolean sodiumCompatibility = true;
    public boolean irisCompatibility = true;

    // CPU
    public boolean backgroundTasks = true;
    public boolean shaderScanAsync = true;
    public int workerThreads = Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() / 2));
    public int reservedCores = Math.min(2, Math.max(1, Runtime.getRuntime().availableProcessors() / 4));

    // Recovery / diagnostics
    public boolean selfHealing = true;
    public int selfHealingFailureThreshold = 3;
    public boolean configurationRollback = true;
    public boolean fileLogging = true;

    // Runtime profile metadata
    public String activePreset = "BALANCED";

    public static SuperOptimizerConfig load(Path dir) {
        SuperOptimizerConfig c = new SuperOptimizerConfig();
        Path file = dir.resolve("superoptimizer.properties");
        if (!Files.isRegularFile(file)) return c;

        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(file)) {
            p.load(r);

            c.enabled = bool(p, "enabled", c.enabled);

            c.profilingEnabled = bool(p, "profilingEnabled", c.profilingEnabled);
            c.profilerHistoryFrames = clamp(integer(p, "profilerHistoryFrames", c.profilerHistoryFrames), 120, 2400);
            c.showTelemetry = bool(p, "showTelemetry", c.showTelemetry);
            c.saveBenchmarks = bool(p, "saveBenchmarks", c.saveBenchmarks);
            c.detectBottleneck = bool(p, "detectBottleneck", c.detectBottleneck);

            c.adaptivePerformance = bool(p, "adaptivePerformance", c.adaptivePerformance);
            c.adaptiveTargetFps = decimal(p, "adaptiveTargetFps", c.adaptiveTargetFps);
            c.adaptiveDropThresholdMs = decimal(p, "adaptiveDropThresholdMs", c.adaptiveDropThresholdMs);
            c.adaptiveRecoverThresholdMs = decimal(p, "adaptiveRecoverThresholdMs", c.adaptiveRecoverThresholdMs);
            c.adaptiveHysteresisSeconds = clamp(integer(p, "adaptiveHysteresisSeconds", c.adaptiveHysteresisSeconds), 1, 30);
            c.adaptiveChangeCooldownSeconds = clamp(integer(p, "adaptiveChangeCooldownSeconds", c.adaptiveChangeCooldownSeconds), 1, 60);
            c.adaptiveStepPercent = clamp(integer(p, "adaptiveStepPercent", c.adaptiveStepPercent), 1, 50);
            c.adaptiveAutoPreset = bool(p, "adaptiveAutoPreset", c.adaptiveAutoPreset);
            c.adaptiveUnfocusedFpsCap = bool(p, "adaptiveUnfocusedFpsCap", c.adaptiveUnfocusedFpsCap);
            c.unfocusedFps = clamp(integer(p, "unfocusedFps", c.unfocusedFps), 5, 30);
            c.thermalFriendlyMode = bool(p, "thermalFriendlyMode", c.thermalFriendlyMode);
            c.hardwareAwareProfiles = bool(p, "hardwareAwareProfiles", c.hardwareAwareProfiles);
            c.gpuStateOptimizer = bool(p, "gpuStateOptimizer", c.gpuStateOptimizer);

            c.particleOptimization = bool(p, "particleOptimization", c.particleOptimization);
            c.maxParticles = clamp(integer(p, "maxParticles", c.maxParticles), 64, 32768);
            c.particleDistance = clamp(integer(p, "particleDistance", c.particleDistance), 8, 256);
            c.particleDistanceCulling = bool(p, "particleDistanceCulling", c.particleDistanceCulling);
            c.particleAdaptive = bool(p, "particleAdaptive", c.particleAdaptive);
            c.particlePool = bool(p, "particlePool", c.particlePool);

            c.entityCulling = bool(p, "entityCulling", c.entityCulling);
            c.blockEntityCulling = bool(p, "blockEntityCulling", c.blockEntityCulling);
            c.entityDistanceCulling = bool(p, "entityDistanceCulling", c.entityDistanceCulling);
            c.entityRenderDistance = clamp(integer(p, "entityRenderDistance", c.entityRenderDistance), 32, 512);
            c.entityLod = bool(p, "entityLod", c.entityLod);
            c.entityLodDistance = clamp(integer(p, "entityLodDistance", c.entityLodDistance), 32, 256);
            c.hideDistantNames = bool(p, "hideDistantNames", c.hideDistantNames);
            c.hideDistantShadows = bool(p, "hideDistantShadows", c.hideDistantShadows);
            c.throttleDecorativeAnimation = bool(p, "throttleDecorativeAnimation", c.throttleDecorativeAnimation);

            c.frameCulling = bool(p, "frameCulling", c.frameCulling);
            c.armorStandCulling = bool(p, "armorStandCulling", c.armorStandCulling);
            c.signCulling = bool(p, "signCulling", c.signCulling);
            c.chestCulling = bool(p, "chestCulling", c.chestCulling);
            c.hopperCulling = bool(p, "hopperCulling", c.hopperCulling);

            c.pauseDuringCameraMotion = bool(p, "pauseDuringCameraMotion", c.pauseDuringCameraMotion);
            c.skipNearEntityCulling = bool(p, "skipNearEntityCulling", c.skipNearEntityCulling);
            c.nearEntityDistance = clamp(integer(p, "nearEntityDistance", c.nearEntityDistance), 0, 64);
            c.directionalEntityCulling = bool(p, "directionalEntityCulling", c.directionalEntityCulling);

            c.smartFrameBudget = bool(p, "smartFrameBudget", c.smartFrameBudget);
            c.frameBudgetMs = clamp(integer(p, "frameBudgetMs", c.frameBudgetMs), 1, 20);
            c.chunkRebuildDeduplication = bool(p, "chunkRebuildDeduplication", c.chunkRebuildDeduplication);
            c.adaptiveChunkScheduler = bool(p, "adaptiveChunkScheduler", c.adaptiveChunkScheduler);
            c.predictiveVisibility = bool(p, "predictiveVisibility", c.predictiveVisibility);
            c.frameTimeStabilizer = bool(p, "frameTimeStabilizer", c.frameTimeStabilizer);
            c.chunkQueueLimit = clamp(integer(p, "chunkQueueLimit", c.chunkQueueLimit), 16, 2048);
            c.chunkUploadBudgetMs = clamp(integer(p, "chunkUploadBudgetMs", c.chunkUploadBudgetMs), 0, 20);
            c.backgroundTaskBudgetMs = clamp(integer(p, "backgroundTaskBudgetMs", c.backgroundTaskBudgetMs), 0, 20);

            c.memoryPressureController = bool(p, "memoryPressureController", c.memoryPressureController);
            c.cacheBudgetMb = clamp(integer(p, "cacheBudgetMb", c.cacheBudgetMb), 64, 4096);
            c.cacheAdmissionPolicy = bool(p, "cacheAdmissionPolicy", c.cacheAdmissionPolicy);
            c.resourceReloadDiff = bool(p, "resourceReloadDiff", c.resourceReloadDiff);
            c.objectPooling = bool(p, "objectPooling", c.objectPooling);
            c.modelCache = bool(p, "modelCache", c.modelCache);
            c.resourceCacheCleanup = bool(p, "resourceCacheCleanup", c.resourceCacheCleanup);
            c.measuredGcControl = bool(p, "measuredGcControl", c.measuredGcControl);

            c.asyncResourcePreparation = bool(p, "asyncResourcePreparation", c.asyncResourcePreparation);
            c.modelBuildCache = bool(p, "modelBuildCache", c.modelBuildCache);
            c.slowDiskDiagnostics = bool(p, "slowDiskDiagnostics", c.slowDiskDiagnostics);
            c.worldDataPrefetch = bool(p, "worldDataPrefetch", c.worldDataPrefetch);
            c.taskBackpressure = bool(p, "taskBackpressure", c.taskBackpressure);
            c.safeAsyncSave = bool(p, "safeAsyncSave", c.safeAsyncSave);

            c.animationWorkBudget = bool(p, "animationWorkBudget", c.animationWorkBudget);
            c.weatherLod = bool(p, "weatherLod", c.weatherLod);
            c.weatherDistance = clamp(integer(p, "weatherDistance", c.weatherDistance), 16, 256);
            c.textLayoutCache = bool(p, "textLayoutCache", c.textLayoutCache);
            c.uiRenderCache = bool(p, "uiRenderCache", c.uiRenderCache);
            c.backgroundPathfindingBudget = bool(p, "backgroundPathfindingBudget", c.backgroundPathfindingBudget);
            c.soundLimit = bool(p, "soundLimit", c.soundLimit);
            c.maxConcurrentSounds = clamp(integer(p, "maxConcurrentSounds", c.maxConcurrentSounds), 8, 256);
            c.lightingDiagnostics = bool(p, "lightingDiagnostics", c.lightingDiagnostics);

            c.disableCullingWithIris = bool(p, "disableCullingWithIris", c.disableCullingWithIris);
            c.disableCullingWithEntityCullingMod = bool(p, "disableCullingWithEntityCullingMod", c.disableCullingWithEntityCullingMod);
            c.sodiumCompatibility = bool(p, "sodiumCompatibility", c.sodiumCompatibility);
            c.irisCompatibility = bool(p, "irisCompatibility", c.irisCompatibility);

            c.backgroundTasks = bool(p, "backgroundTasks", c.backgroundTasks);
            c.shaderScanAsync = bool(p, "shaderScanAsync", c.shaderScanAsync);
            c.workerThreads = clamp(integer(p, "workerThreads", c.workerThreads), 1, 32);
            c.reservedCores = clamp(integer(p, "reservedCores", c.reservedCores), 0, 64);

            c.selfHealing = bool(p, "selfHealing", c.selfHealing);
            c.selfHealingFailureThreshold = clamp(integer(p, "selfHealingFailureThreshold", c.selfHealingFailureThreshold), 1, 10);
            c.configurationRollback = bool(p, "configurationRollback", c.configurationRollback);
            c.fileLogging = bool(p, "fileLogging", c.fileLogging);
            c.activePreset = p.getProperty("activePreset", c.activePreset);
        } catch (IOException e) {
            SuperOptimizerClient.LOGGER.warn("Не удалось прочитать конфиг SuperOptimizer", e);
        }
        return c;
    }

    public void save(Path dir) {
        try {
            Files.createDirectories(dir);
            Properties p = new Properties();

            put(p,"enabled",enabled);
            put(p,"profilingEnabled",profilingEnabled);
            put(p,"profilerHistoryFrames",profilerHistoryFrames);
            put(p,"showTelemetry",showTelemetry);
            put(p,"saveBenchmarks",saveBenchmarks);
            put(p,"detectBottleneck",detectBottleneck);

            put(p,"adaptivePerformance",adaptivePerformance);
            put(p,"adaptiveTargetFps",adaptiveTargetFps);
            put(p,"adaptiveDropThresholdMs",adaptiveDropThresholdMs);
            put(p,"adaptiveRecoverThresholdMs",adaptiveRecoverThresholdMs);
            put(p,"adaptiveHysteresisSeconds",adaptiveHysteresisSeconds);
            put(p,"adaptiveChangeCooldownSeconds",adaptiveChangeCooldownSeconds);
            put(p,"adaptiveStepPercent",adaptiveStepPercent);
            put(p,"adaptiveAutoPreset",adaptiveAutoPreset);
            put(p,"adaptiveUnfocusedFpsCap",adaptiveUnfocusedFpsCap);
            put(p,"unfocusedFps",unfocusedFps);
            put(p,"thermalFriendlyMode",thermalFriendlyMode);
            put(p,"hardwareAwareProfiles",hardwareAwareProfiles);
            put(p,"gpuStateOptimizer",gpuStateOptimizer);

            put(p,"particleOptimization",particleOptimization);
            put(p,"maxParticles",maxParticles);
            put(p,"particleDistance",particleDistance);
            put(p,"particleDistanceCulling",particleDistanceCulling);
            put(p,"particleAdaptive",particleAdaptive);
            put(p,"particlePool",particlePool);

            put(p,"entityCulling",entityCulling);
            put(p,"blockEntityCulling",blockEntityCulling);
            put(p,"entityDistanceCulling",entityDistanceCulling);
            put(p,"entityRenderDistance",entityRenderDistance);
            put(p,"entityLod",entityLod);
            put(p,"entityLodDistance",entityLodDistance);
            put(p,"hideDistantNames",hideDistantNames);
            put(p,"hideDistantShadows",hideDistantShadows);
            put(p,"throttleDecorativeAnimation",throttleDecorativeAnimation);

            put(p,"frameCulling",frameCulling);
            put(p,"armorStandCulling",armorStandCulling);
            put(p,"signCulling",signCulling);
            put(p,"chestCulling",chestCulling);
            put(p,"hopperCulling",hopperCulling);

            put(p,"pauseDuringCameraMotion",pauseDuringCameraMotion);
            put(p,"skipNearEntityCulling",skipNearEntityCulling);
            put(p,"nearEntityDistance",nearEntityDistance);
            put(p,"directionalEntityCulling",directionalEntityCulling);

            put(p,"smartFrameBudget",smartFrameBudget);
            put(p,"frameBudgetMs",frameBudgetMs);
            put(p,"chunkRebuildDeduplication",chunkRebuildDeduplication);
            put(p,"adaptiveChunkScheduler",adaptiveChunkScheduler);
            put(p,"predictiveVisibility",predictiveVisibility);
            put(p,"frameTimeStabilizer",frameTimeStabilizer);
            put(p,"chunkQueueLimit",chunkQueueLimit);
            put(p,"chunkUploadBudgetMs",chunkUploadBudgetMs);
            put(p,"backgroundTaskBudgetMs",backgroundTaskBudgetMs);

            put(p,"memoryPressureController",memoryPressureController);
            put(p,"cacheBudgetMb",cacheBudgetMb);
            put(p,"cacheAdmissionPolicy",cacheAdmissionPolicy);
            put(p,"resourceReloadDiff",resourceReloadDiff);
            put(p,"objectPooling",objectPooling);
            put(p,"modelCache",modelCache);
            put(p,"resourceCacheCleanup",resourceCacheCleanup);
            put(p,"measuredGcControl",measuredGcControl);

            put(p,"asyncResourcePreparation",asyncResourcePreparation);
            put(p,"modelBuildCache",modelBuildCache);
            put(p,"slowDiskDiagnostics",slowDiskDiagnostics);
            put(p,"worldDataPrefetch",worldDataPrefetch);
            put(p,"taskBackpressure",taskBackpressure);
            put(p,"safeAsyncSave",safeAsyncSave);

            put(p,"animationWorkBudget",animationWorkBudget);
            put(p,"weatherLod",weatherLod);
            put(p,"weatherDistance",weatherDistance);
            put(p,"textLayoutCache",textLayoutCache);
            put(p,"uiRenderCache",uiRenderCache);
            put(p,"backgroundPathfindingBudget",backgroundPathfindingBudget);
            put(p,"soundLimit",soundLimit);
            put(p,"maxConcurrentSounds",maxConcurrentSounds);
            put(p,"lightingDiagnostics",lightingDiagnostics);

            put(p,"disableCullingWithIris",disableCullingWithIris);
            put(p,"disableCullingWithEntityCullingMod",disableCullingWithEntityCullingMod);
            put(p,"sodiumCompatibility",sodiumCompatibility);
            put(p,"irisCompatibility",irisCompatibility);

            put(p,"backgroundTasks",backgroundTasks);
            put(p,"shaderScanAsync",shaderScanAsync);
            put(p,"workerThreads",workerThreads);
            put(p,"reservedCores",reservedCores);

            put(p,"selfHealing",selfHealing);
            put(p,"selfHealingFailureThreshold",selfHealingFailureThreshold);
            put(p,"configurationRollback",configurationRollback);
            put(p,"fileLogging",fileLogging);
            p.setProperty("activePreset", activePreset);

            try (Writer w = Files.newBufferedWriter(dir.resolve("superoptimizer.properties"))) {
                p.store(w, "SuperOptimizer 26.4");
            }
        } catch (IOException e) {
            SuperOptimizerClient.LOGGER.warn("Не удалось сохранить конфиг SuperOptimizer", e);
        }
    }

    private static void put(Properties p, String k, Object v) {
        p.setProperty(k, String.valueOf(v));
    }
    private static boolean bool(Properties p, String key, boolean fallback) {
        String value=p.getProperty(key);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }
    private static int integer(Properties p, String key, int fallback) {
        try { return Integer.parseInt(p.getProperty(key, Integer.toString(fallback)).trim()); }
        catch (NumberFormatException e) { return fallback; }
    }
    private static double decimal(Properties p, String key, double fallback) {
        try { return Double.parseDouble(p.getProperty(key, Double.toString(fallback)).trim()); }
        catch (NumberFormatException e) { return fallback; }
    }
    private static int clamp(int v,int min,int max){return Math.max(min,Math.min(max,v));}
}