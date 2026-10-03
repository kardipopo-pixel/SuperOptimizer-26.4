package dev.kardipopo.superoptimizer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Bounded task bookkeeping used by the client-side scheduler. It deduplicates
 * repeated logical jobs and applies backpressure before work reaches a worker.
 * Minecraft's own chunk build pipeline remains authoritative.
 */
public final class ChunkTaskController {
    private static final Map<Long, PendingTask> PENDING = new ConcurrentHashMap<>();
    private static final LongAdder submitted = new LongAdder();
    private static final LongAdder deduplicated = new LongAdder();
    private static final LongAdder rejected = new LongAdder();

    private ChunkTaskController() {}

    public static boolean offer(long key, int priority, Runnable task) {
        SuperOptimizerConfig c = SuperOptimizerClient.config();
        if (c == null || !c.enabled || !c.chunkRebuildDeduplication || task == null) return true;

        if (PENDING.size() >= c.chunkQueueLimit) {
            rejected.increment();
            return false;
        }

        PendingTask existing = PENDING.putIfAbsent(key, new PendingTask(priority, task));
        if (existing != null) {
            deduplicated.increment();
            if (priority > existing.priority) {
                PENDING.replace(key, new PendingTask(priority, task));
            }
            return false;
        }

        submitted.increment();
        return true;
    }

    public static Runnable poll(long key) {
        PendingTask task = PENDING.remove(key);
        return task == null ? null : task.task;
    }

    public static void complete(long key) {
        PENDING.remove(key);
    }

    public static int queueSize() {
        return PENDING.size();
    }

    public static long submitted() { return submitted.sum(); }
    public static long deduplicated() { return deduplicated.sum(); }
    public static long rejected() { return rejected.sum(); }

    public record PendingTask(int priority, Runnable task) {}
}