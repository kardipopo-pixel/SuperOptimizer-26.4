package dev.kardipopo.superoptimizer;

import net.minecraft.client.Minecraft;

/**
 * Adaptive controller uses measured frame time and hysteresis. It changes only
 * runtime effective values for features marked as adaptive; user config fields
 * remain untouched unless auto-preset is explicitly enabled.
 */
public final class AdaptivePerformanceController {
    private static int pressureTicks;
    private static int recoveryTicks;
    private static long lastChangeNs;
    private static double qualityScale = 1.0;
    private static int effectiveParticleDistance = 64;
    private static int effectiveMaxParticles = 4096;
    private static int effectiveEntityDistance = 160;
    private static boolean initialized;

    private AdaptivePerformanceController() {}

    public static void init(SuperOptimizerConfig config) {
        if (config == null) return;
        effectiveParticleDistance = config.particleDistance;
        effectiveMaxParticles = config.maxParticles;
        effectiveEntityDistance = config.entityRenderDistance;
        qualityScale = 1.0;
        pressureTicks = 0;
        recoveryTicks = 0;
        lastChangeNs = System.nanoTime();
        initialized = true;
    }

    public static void tick() {
        SuperOptimizerConfig c = SuperOptimizerClient.config();
        if (!initialized || c == null || !c.enabled || !c.adaptivePerformance) return;

        Minecraft mc = Minecraft.getInstance();

        // Unfocused rendering is handled by the modern FramerateLimitTracker when possible.
        // We only provide a safe renderer-thread sleep fallback here.
        if (c.adaptiveUnfocusedFpsCap && !mc.isWindowActive()) {
            throttleUnfocused(c.unfocusedFps);
        }

        if (!c.frameTimeStabilizer && !c.particleAdaptive && !c.entityLod) return;

        double frameMs = PerformanceProfiler.lastFrameMs();
        if (frameMs <= 0) return;

        double drop = Math.max(c.adaptiveDropThresholdMs, 1000.0 / Math.max(10.0, c.adaptiveTargetFps) * 1.5);
        double recover = Math.min(c.adaptiveRecoverThresholdMs, 1000.0 / Math.max(10.0, c.adaptiveTargetFps));

        if (frameMs > drop) {
            pressureTicks++;
            recoveryTicks = 0;
        } else if (frameMs < recover) {
            recoveryTicks++;
            pressureTicks = 0;
        } else {
            pressureTicks = 0;
            recoveryTicks = 0;
        }

        int hysteresis = Math.max(1, c.adaptiveHysteresisSeconds * 20);
        if (pressureTicks >= hysteresis && cooldownElapsed(c)) {
            adjust(-c.adaptiveStepPercent / 100.0, c);
            pressureTicks = 0;
        } else if (recoveryTicks >= hysteresis && cooldownElapsed(c)) {
            adjust(+c.adaptiveStepPercent / 100.0, c);
            recoveryTicks = 0;
        }
    }

    private static boolean cooldownElapsed(SuperOptimizerConfig c) {
        return System.nanoTime() - lastChangeNs
                >= Math.max(1, c.adaptiveChangeCooldownSeconds) * 1_000_000_000L;
    }

    private static void adjust(double delta, SuperOptimizerConfig c) {
        double old = qualityScale;
        qualityScale = clamp(qualityScale + delta, 0.30, 1.0);
        if (Math.abs(qualityScale - old) < 0.001) return;

        int step = Math.max(1, c.adaptiveStepPercent);

        if (c.particleAdaptive) {
            int distance = (int)Math.round(c.particleDistance * qualityScale);
            effectiveParticleDistance = clamp(distance, 8, c.particleDistance);

            int max = (int)Math.round(c.maxParticles * Math.max(0.35, qualityScale));
            effectiveMaxParticles = clamp(max, 64, c.maxParticles);
        }

        if (c.entityLod || c.entityDistanceCulling) {
            int distance = (int)Math.round(c.entityRenderDistance * Math.max(0.45, qualityScale));
            effectiveEntityDistance = clamp(distance, 32, c.entityRenderDistance);
        }

        lastChangeNs = System.nanoTime();
        SuperOptimizerLog.info("Adaptive Performance: qualityScale="
                + String.format(java.util.Locale.ROOT, "%.2f", qualityScale)
                + " (step " + step + "%, " + (delta < 0 ? "снижение" : "восстановление") + ")");
    }

    private static void throttleUnfocused(int fps) {
        int safeFps = Math.max(5, Math.min(30, fps));
        long minFrameNs = 1_000_000_000L / safeFps;
        long start = System.nanoTime();
        long frame = PerformanceProfiler.lastFrameNs();
        long remaining = minFrameNs - frame;
        if (remaining <= 0) return;
        long elapsed = System.nanoTime() - start;
        remaining -= elapsed;
        if (remaining <= 0) return;
        try {
            Thread.sleep(Math.max(0L, remaining / 1_000_000L),
                    (int)Math.max(0L, remaining % 1_000_000L));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static double qualityScale() { return qualityScale; }
    public static int particleDistance() {
        return effectiveParticleDistance > 0 ? effectiveParticleDistance : 64;
    }
    public static int maxParticles() {
        return effectiveMaxParticles > 0 ? effectiveMaxParticles : 4096;
    }
    public static int entityDistance() {
        return effectiveEntityDistance > 0 ? effectiveEntityDistance : 160;
    }
    public static boolean isAdaptive() {
        SuperOptimizerConfig c = SuperOptimizerClient.config();
        return c != null && c.enabled && c.adaptivePerformance;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}