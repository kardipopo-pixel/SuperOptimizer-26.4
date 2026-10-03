package dev.kardipopo.superoptimizer;

import java.util.concurrent.atomic.LongAdder;

/**
 * Runtime memory pressure gate. It never calls System.gc() on every frame.
 * It only tightens admission to optional caches/tasks when heap pressure is high.
 */
public final class MemoryPressureController {
    private static volatile boolean pressure;
    private static volatile double ratio;
    private static long lastLogNs;

    private MemoryPressureController() {}

    public static void tick() {
        SuperOptimizerConfig c = SuperOptimizerClient.config();
        if (c == null || !c.enabled || !c.memoryPressureController) {
            pressure = false;
            ratio = 0;
            return;
        }

        Runtime rt = Runtime.getRuntime();
        long max = rt.maxMemory();
        long used = rt.totalMemory() - rt.freeMemory();
        ratio = max > 0 ? (double) used / (double) max : 0;

        boolean next = ratio >= 0.90;
        if (next && !pressure) {
            lastLogNs = System.nanoTime();
            SuperOptimizerLog.warn("Memory Pressure: heap usage "
                    + Math.round(ratio * 100.0) + "%; уменьшаем необязательную работу.");
        } else if (!next && pressure) {
            long now = System.nanoTime();
            if (now - lastLogNs > 5_000_000_000L) {
                SuperOptimizerLog.info("Memory Pressure снят.");
            }
        }
        pressure = next;
    }

    public static boolean isUnderPressure() { return pressure; }
    public static double ratio() { return ratio; }

    public static int scaleLimit(int configured, int minimum) {
        if (!pressure) return configured;
        return Math.max(minimum, (int) Math.round(configured * 0.70));
    }
}