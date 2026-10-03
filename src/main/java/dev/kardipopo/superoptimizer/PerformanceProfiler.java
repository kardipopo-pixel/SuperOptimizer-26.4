package dev.kardipopo.superoptimizer;

import com.sun.management.OperatingSystemMXBean;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Lightweight render-thread profiler. It measures the actual render call, not tick time. */
public final class PerformanceProfiler {
    public record Snapshot(
            double fps,
            double frameMs,
            double onePercentLow,
            double cpuLoad,
            double gpuUtilization,
            double heapUsedMb,
            double heapMaxMb,
            double gcPauseMs,
            String bottleneck,
            long samples
    ) {}

    public record Benchmark(
            String label,
            double fps,
            double frameMs,
            double onePercentLow,
            double cpuLoad,
            double gpuUtilization,
            double heapUsedMb,
            double gcPauseMs,
            String bottleneck
    ) {}

    private static long frameStartNs;
    private static boolean measuring;
    private static long lastFrameNs;
    private static double lastFrameMs;
    private static final ArrayDeque<Double> frameTimes = new ArrayDeque<>();
    private static int historyLimit = 600;

    private static double cpuLoad = -1;
    private static double gpuUtilization = -1;
    private static double gcPauseMs = 0;
    private static long lastGcTime;
    private static long lastGcCount;
    private static long gcWindowStartNs = System.nanoTime();

    private static Benchmark before;
    private static Benchmark after;
    private static Field gpuField;
    private static boolean gpuFieldChecked;

    private PerformanceProfiler() {}

    public static synchronized void configure(SuperOptimizerConfig config) {
        if (config == null) return;
        historyLimit = Math.max(120, Math.min(2400, config.profilerHistoryFrames));
        while (frameTimes.size() > historyLimit) frameTimes.removeFirst();
    }

    public static void frameStart() {
        if (!isEnabled()) return;
        frameStartNs = System.nanoTime();
        measuring = true;
    }

    public static void frameEnd() {
        if (!isEnabled() || !measuring) return;

        long now = System.nanoTime();
        long duration = Math.max(1L, now - frameStartNs);
        lastFrameNs = duration;
        lastFrameMs = duration / 1_000_000.0;

        synchronized (PerformanceProfiler.class) {
            frameTimes.addLast(lastFrameMs);
            while (frameTimes.size() > historyLimit) frameTimes.removeFirst();
        }

        sampleSystem(now);
        measuring = false;
    }

    private static boolean isEnabled() {
        SuperOptimizerConfig c = SuperOptimizerClient.config();
        return c != null && c.enabled && c.profilingEnabled;
    }

    private static void sampleSystem(long now) {
        try {
            java.lang.management.OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
            if (bean instanceof OperatingSystemMXBean os) {
                cpuLoad = os.getProcessCpuLoad() >= 0 ? os.getProcessCpuLoad() * 100.0 : os.getCpuLoad() * 100.0;
                if (cpuLoad < 0) cpuLoad = -1;
            }
        } catch (Throwable ignored) {
            cpuLoad = -1;
        }

        Runtime rt = Runtime.getRuntime();
        double used = (rt.totalMemory() - rt.freeMemory()) / 1048576.0;
        double max = rt.maxMemory() / 1048576.0;
        if (max > 0 && used / max > 0.98) {
            SuperOptimizerLog.warn("Память JVM почти заполнена: " + Math.round(used) + "/" + Math.round(max) + " MiB");
        }

        long gcTime = 0;
        long gcCount = 0;
        List<GarbageCollectorMXBean> beans = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean bean : beans) {
            if (bean.getCollectionTime() >= 0) gcTime += bean.getCollectionTime();
            if (bean.getCollectionCount() >= 0) gcCount += bean.getCollectionCount();
        }

        long dTime = gcTime - lastGcTime;
        long dCount = gcCount - lastGcCount;
        long windowMs = Math.max(1L, (now - gcWindowStartNs) / 1_000_000L);
        if (windowMs >= 250) {
            gcPauseMs = Math.max(0, dTime);
            lastGcTime = gcTime;
            lastGcCount = gcCount;
            gcWindowStartNs = now;
        }
        gpuUtilization = readGpuUtilization();
    }

    private static double readGpuUtilization() {
        try {
            if (!gpuFieldChecked) {
                gpuFieldChecked = true;
                Class<?> c = Minecraft.class;
                for (String name : new String[]{"gpuUtilization", "gpuUtilizationPercent"}) {
                    try {
                        gpuField = c.getDeclaredField(name);
                        gpuField.setAccessible(true);
                        break;
                    } catch (NoSuchFieldException ignored) {}
                }
            }
            if (gpuField != null) {
                Object value = gpuField.get(Minecraft.getInstance());
                if (value instanceof Number n) {
                    double v = n.doubleValue();
                    if (v >= 0 && v <= 1000) return v <= 1 ? v * 100.0 : v;
                }
            }
        } catch (Throwable ignored) {}
        return -1;
    }

    public static Snapshot snapshot() {
        double fps = lastFrameMs > 0 ? 1000.0 / lastFrameMs : 0;
        double oneLow = onePercentLow();
        Runtime rt=Runtime.getRuntime();
        double used=(rt.totalMemory()-rt.freeMemory())/1048576.0;
        double max=rt.maxMemory()/1048576.0;
        return new Snapshot(fps,lastFrameMs,oneLow,cpuLoad,gpuUtilization,used,max,gcPauseMs,bottleneck(),frameTimes.size());
    }

    public static synchronized double onePercentLow() {
        if (frameTimes.size() < 10) return lastFrameMs > 0 ? 1000.0 / lastFrameMs : 0;
        Double[] values=frameTimes.toArray(Double[]::new);
        Arrays.sort(values, Comparator.reverseOrder());
        int count=Math.max(1, (int)Math.ceil(values.length*0.01));
        double sum=0;
        for(int i=0;i<count;i++) sum+=values[i];
        double slowestMeanMs=sum/count;
        return slowestMeanMs>0 ? 1000.0/slowestMeanMs : 0;
    }

    public static String bottleneck() {
        SuperOptimizerConfig c=SuperOptimizerClient.config();
        if(c==null) return "UNKNOWN";
        Snapshot s=rawSnapshot();
        if(s.gcPauseMs() >= 20) return "GC";
        if(s.heapMaxMb() > 0 && s.heapUsedMb()/s.heapMaxMb() >= 0.90) return "Память";
        double target = 1000.0/Math.max(15.0,c.adaptiveTargetFps);
        if(s.frameMs() <= 0 || s.frameMs() < target*0.90) return "Нет явного узкого места";
        if(s.cpuLoad() >= 80) return "CPU";
        if(s.gpuUtilization() >= 92) return "GPU";
        if(ChunkTaskController.queueSize() > 32) return "Чанки";
        return "Рендер";
    }

    private static Snapshot rawSnapshot() {
        Runtime rt=Runtime.getRuntime();
        double used=(rt.totalMemory()-rt.freeMemory())/1048576.0;
        double max=rt.maxMemory()/1048576.0;
        double fps=lastFrameMs>0?1000.0/lastFrameMs:0;
        return new Snapshot(fps,lastFrameMs,onePercentLow(),cpuLoad,gpuUtilization,used,max,gcPauseMs,"",frameTimes.size());
    }

    public static synchronized boolean startBenchmark(Path dir) {
        if (frameTimes.size() < 30) {
            SuperOptimizerLog.warn("Для базового теста нужно накопить хотя бы 30 кадров.");
            return false;
        }
        before = makeBenchmark("До");
        after = null;
        if (SuperOptimizerClient.config() != null && SuperOptimizerClient.config().saveBenchmarks) saveBenchmarks(dir);
        SuperOptimizerLog.info("Профилирование: сохранён результат ДО.");
        return true;
    }

    public static synchronized boolean finishBenchmark(Path dir) {
        if (before == null) {
            SuperOptimizerLog.warn("Сначала запусти тест «До», затем «После».");
            return false;
        }
        after = makeBenchmark("После");
        saveBenchmarks(dir);
        SuperOptimizerLog.info("Профилирование: сохранён результат ПОСЛЕ.");
        return true;
    }

    private static Benchmark makeBenchmark(String label) {
        Snapshot s=snapshot();
        return new Benchmark(label,s.fps(),s.frameMs(),s.onePercentLow(),s.cpuLoad(),s.gpuUtilization(),s.heapUsedMb(),s.gcPauseMs(),s.bottleneck());
    }

    private static void saveBenchmarks(Path dir) {
        if(before==null && after==null) return;
        try {
            Files.createDirectories(dir);
            PropertiesStore.write(dir.resolve("superoptimizer-benchmark.properties"), before, after);
        } catch(IOException e) {
            SuperOptimizerLog.warn("Не удалось сохранить benchmark: "+e.getMessage());
        }
    }

    public static Benchmark before() { return before; }
    public static Benchmark after() { return after; }

    public static synchronized void clearHistory() {
        frameTimes.clear();
        before=null; after=null;
        lastFrameMs=0;
    }

    public static long lastFrameNs(){return lastFrameNs;}
    public static double lastFrameMs(){return lastFrameMs;}
    public static double currentFps(){return lastFrameMs>0?1000.0/lastFrameMs:0;}
    public static double currentCpu(){return cpuLoad;}
    public static double currentGpu(){return gpuUtilization;}
    public static double currentGcPauseMs(){return gcPauseMs;}

    private static final class PropertiesStore {
        private static void write(Path file, Benchmark b, Benchmark a) throws IOException {
            java.util.Properties p=new java.util.Properties();
            if(b!=null) put(p,"before",b);
            if(a!=null) put(p,"after",a);
            try(Writer w=Files.newBufferedWriter(file)){p.store(w,"SuperOptimizer benchmark");}
        }
        private static void put(java.util.Properties p,String prefix,Benchmark b){
            p.setProperty(prefix+".fps",Double.toString(b.fps()));
            p.setProperty(prefix+".frameMs",Double.toString(b.frameMs()));
            p.setProperty(prefix+".onePercentLow",Double.toString(b.onePercentLow()));
            p.setProperty(prefix+".cpuLoad",Double.toString(b.cpuLoad()));
            p.setProperty(prefix+".gpuUtilization",Double.toString(b.gpuUtilization()));
            p.setProperty(prefix+".heapUsedMb",Double.toString(b.heapUsedMb()));
            p.setProperty(prefix+".gcPauseMs",Double.toString(b.gcPauseMs()));
            p.setProperty(prefix+".bottleneck",b.bottleneck());
        }
    }
}