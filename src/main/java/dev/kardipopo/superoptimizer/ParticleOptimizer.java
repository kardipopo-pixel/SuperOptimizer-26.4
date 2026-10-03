package dev.kardipopo.superoptimizer;

import net.minecraft.client.Minecraft;

import java.util.concurrent.atomic.LongAdder;

/** Particle admission controller. Rejects particles before creation when safe limits are hit. */
public final class ParticleOptimizer {
    private static final LongAdder created = new LongAdder();
    private static final LongAdder rejectedDistance = new LongAdder();
    private static final LongAdder rejectedCount = new LongAdder();
    private static final LongAdder rejectedDisabled = new LongAdder();

    private ParticleOptimizer() {}

    public static boolean allowCreation(double x, double y, double z) {
        SuperOptimizerConfig c = SuperOptimizerClient.config();
        if (c == null || !c.enabled || !c.particleOptimization) {
            rejectedDisabled.increment();
            return true;
        }

        if (c.particleDistanceCulling) {
            var camera = Minecraft.getInstance().getCameraEntity();
            if (camera != null) {
                double dx = x - camera.getX();
                double dy = y - camera.getY();
                double dz = z - camera.getZ();
                double distanceSq = dx * dx + dy * dy + dz * dz;
                double radius = AdaptivePerformanceController.isAdaptive()
                        ? AdaptivePerformanceController.particleDistance()
                        : c.particleDistance;
                if (distanceSq > radius * radius) {
                    rejectedDistance.increment();
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean allowAdd() {
        SuperOptimizerConfig c = SuperOptimizerClient.config();
        if (c == null || !c.enabled || !c.particleOptimization) return true;

        int max = AdaptivePerformanceController.isAdaptive()
                ? AdaptivePerformanceController.maxParticles()
                : c.maxParticles;

        try {
            int count = parseCount(Minecraft.getInstance().particleEngine.countParticles());
            if (count >= max) {
                rejectedCount.increment();
                return false;
            }
        } catch (Throwable ignored) {}

        created.increment();
        return true;
    }

    public static long created() { return created.sum(); }
    public static long rejectedDistance() { return rejectedDistance.sum(); }
    public static long rejectedCount() { return rejectedCount.sum(); }

    public static int currentCount() {
        try {
            return parseCount(Minecraft.getInstance().particleEngine.countParticles());
        } catch (Throwable ignored) {
            return -1;
        }
    }

    private static int parseCount(String raw) {
        if (raw == null) return -1;
        int end = raw.length() - 1;
        while (end >= 0 && !Character.isDigit(raw.charAt(end))) end--;
        if (end < 0) return -1;
        int start = end;
        while (start >= 0 && Character.isDigit(raw.charAt(start))) start--;
        try {
            return Integer.parseInt(raw.substring(start + 1, end + 1));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static void resetStats() {
        created.reset();
        rejectedDistance.reset();
        rejectedCount.reset();
        rejectedDisabled.reset();
    }
}