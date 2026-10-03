package dev.kardipopo.superoptimizer;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Exports a self-contained diagnostic report without uploading anything. */
public final class DiagnosticReportExporter {
    private DiagnosticReportExporter() {}

    public static Path export(Path configDir) {
        Path out = configDir.resolve("superoptimizer-diagnostic-" + System.currentTimeMillis() + ".txt");
        SuperOptimizerConfig c = SuperOptimizerClient.config();
        PerformanceProfiler.Snapshot s = PerformanceProfiler.snapshot();

        List<String> lines = new ArrayList<>();
        lines.add("SuperOptimizer 26.4 diagnostic report");
        lines.add("Generated: " + Instant.now());
        lines.add("");
        lines.add("=== Runtime ===");
        lines.add("Minecraft: 26.4-alpha.2");
        lines.add("Java: " + System.getProperty("java.version"));
        lines.add("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        lines.add("CPU cores: " + Runtime.getRuntime().availableProcessors());
        lines.add("");
        lines.add("=== Graphics / Mods ===");
        lines.add("Graphics backend: " + net.minecraft.client.Minecraft.getInstance().options.preferredGraphicsBackend());
        lines.add("Sodium: " + modLine("sodium"));
        lines.add("Iris: " + modLine("iris"));
        lines.add("Entity Culling: " + modLine("entityculling"));
        lines.add("Mod Menu: " + modLine("modmenu"));
        lines.add("");
        lines.add("=== Profiler ===");
        lines.add(String.format(java.util.Locale.ROOT, "FPS: %.2f", s.fps()));
        lines.add(String.format(java.util.Locale.ROOT, "Frame time: %.2f ms", s.frameMs()));
        lines.add(String.format(java.util.Locale.ROOT, "1%% low: %.2f FPS", s.onePercentLow()));
        lines.add(String.format(java.util.Locale.ROOT, "CPU: %s", formatLoad(s.cpuLoad())));
        lines.add(String.format(java.util.Locale.ROOT, "GPU: %s", formatLoad(s.gpuUtilization())));
        lines.add(String.format(java.util.Locale.ROOT, "GC pause window: %.2f ms", s.gcPauseMs()));
        lines.add(String.format(java.util.Locale.ROOT, "Heap: %.0f / %.0f MiB", s.heapUsedMb(), s.heapMaxMb()));
        lines.add("Bottleneck: " + s.bottleneck());
        lines.add("Frame samples: " + s.samples());
        lines.add("");
        lines.add("=== Active optimizations ===");
        if (c != null) {
            add(lines, "Smart Frame Budget", c.smartFrameBudget);
            add(lines, "Chunk Rebuild Deduplication", c.chunkRebuildDeduplication);
            add(lines, "Adaptive Chunk Scheduler", c.adaptiveChunkScheduler);
            add(lines, "Predictive Visibility", c.predictiveVisibility);
            add(lines, "Frame-Time Stabilizer", c.frameTimeStabilizer);
            add(lines, "Particle Optimization", c.particleOptimization);
            add(lines, "Particle Distance Culling", c.particleDistanceCulling);
            add(lines, "Adaptive Particles", c.particleAdaptive);
            add(lines, "Entity Culling", c.entityCulling);
            add(lines, "Entity Distance Culling", c.entityDistanceCulling);
            add(lines, "Entity LOD", c.entityLod);
            add(lines, "Block Entity Culling", c.blockEntityCulling);
            add(lines, "Memory Pressure Controller", c.memoryPressureController);
            add(lines, "Cache Admission Policy", c.cacheAdmissionPolicy);
            add(lines, "Resource Reload Diff", c.resourceReloadDiff);
            add(lines, "Task Backpressure", c.taskBackpressure);
            add(lines, "Self-Healing", c.selfHealing);
            add(lines, "Thermal-Friendly Mode", c.thermalFriendlyMode);
        }
        lines.add("");
        lines.add("=== Measured counters ===");
        lines.add("Entity culled: " + CullingContext.entityCulled() + " / " + CullingContext.entityChecks());
        lines.add("Block entities culled: " + CullingContext.blockEntityCulled() + " / " + CullingContext.blockEntityChecks());
        lines.add("Entity LOD states: " + CullingContext.entityLodApplied());
        lines.add("Particles rejected by distance: " + ParticleOptimizer.rejectedDistance());
        lines.add("Particles rejected by cap: " + ParticleOptimizer.rejectedCount());
        lines.add("Chunk logical queue: " + ChunkTaskController.queueSize());
        lines.add("Chunk deduplicated: " + ChunkTaskController.deduplicated());
        lines.add("Chunk rejected: " + ChunkTaskController.rejected());
        lines.add("Memory pressure: " + String.format(java.util.Locale.ROOT, "%.1f%%", MemoryPressureController.ratio() * 100.0));
        lines.add("");
        lines.add("=== Benchmark ===");
        appendBenchmark(lines, "Before", PerformanceProfiler.before());
        appendBenchmark(lines, "After", PerformanceProfiler.after());

        PerformanceProfiler.Comparison comparison = PerformanceProfiler.comparison();
        if (comparison != null) {
            lines.add(String.format(java.util.Locale.ROOT, "FPS delta: %.2f%%", comparison.fpsDeltaPercent()));
            lines.add(String.format(java.util.Locale.ROOT, "1%% low delta: %.2f%%", comparison.oneLowDeltaPercent()));
            lines.add(String.format(java.util.Locale.ROOT, "Frame time delta: %.2f%%", comparison.frameMsDeltaPercent()));
        }

        try {
            Files.createDirectories(configDir);
            Files.write(out, lines, StandardCharsets.UTF_8);
            SuperOptimizerLog.info("Диагностический отчёт экспортирован: " + out.getFileName());
            return out;
        } catch (IOException e) {
            SuperOptimizerLog.warn("Не удалось экспортировать диагностический отчёт: " + e.getMessage());
            return null;
        }
    }

    private static void add(List<String> lines, String name, boolean enabled) {
        lines.add((enabled ? "[ON] " : "[OFF] ") + name);
    }

    private static void appendBenchmark(List<String> lines, String label, PerformanceProfiler.Benchmark b) {
        if (b == null) {
            lines.add(label + ": нет");
            return;
        }
        lines.add(String.format(java.util.Locale.ROOT,
                "%s: %.2f FPS | %.2f ms | %.2f 1%% low | CPU %s | GPU %s | GC %.2f ms | %s",
                label, b.fps(), b.frameMs(), b.onePercentLow(), formatLoad(b.cpuLoad()),
                formatLoad(b.gpuUtilization()), b.gcPauseMs(), b.bottleneck()));
    }

    private static String formatLoad(double v) {
        return v < 0 ? "UNKNOWN" : String.format(java.util.Locale.ROOT, "%.1f%%", v);
    }

    private static String modLine(String id) {
        return FabricLoader.getInstance().getModContainer(id)
                .map(c -> "installed " + c.getMetadata().getVersion().getFriendlyString())
                .orElse("not installed");
    }
}