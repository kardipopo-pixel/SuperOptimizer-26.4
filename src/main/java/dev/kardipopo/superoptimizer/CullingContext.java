package dev.kardipopo.superoptimizer;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.SectionPos;

/**
 * Render-thread-only context. It consumes Minecraft's already-computed visible
 * render sections. Unknown visibility always falls back to rendering.
 */
public final class CullingContext {
    private static final LongOpenHashSet VISIBLE = new LongOpenHashSet();
    private static boolean active;

    private CullingContext() {}

    public static void begin(LevelRenderer renderer) {
        VISIBLE.clear();
        active = SuperOptimizerClient.config() != null
                && SuperOptimizerClient.config().enabled
                && SuperOptimizerClient.config().entityCulling;

        if (!active) return;

        for (SectionRenderDispatcher.RenderSection section : renderer.visibleSections()) {
            if (section == null) continue;
            VISIBLE.add(SectionPos.asLong(section.getRenderOrigin()));
        }
    }

    public static void end() {
        VISIBLE.clear();
        active = false;
    }

    /**
     * Conservative entity policy:
     * - never cull large/extended render states;
     * - never cull entities with name/score/leash data;
     * - never cull entities that glow or have explicit outline data;
     * - otherwise, render if the entity section or any adjacent section is visible.
     */
    public static boolean shouldSubmit(EntityRenderState state) {
        if (!active || state == null) return true;

        if (state.nameTag != null || state.scoreText != null) return true;
        if (state.leashStates != null && !state.leashStates.isEmpty()) return true;
        if (state.appearsGlowing()) return true;
        if (state.outlineColor != EntityRenderState.NO_OUTLINE) return true;

        if (state.boundingBoxWidth > 2.5f || state.boundingBoxHeight > 4.5f) return true;

        int sx = SectionPos.blockToSectionCoord((int) Math.floor(state.x));
        int sy = SectionPos.blockToSectionCoord((int) Math.floor(state.y));
        int sz = SectionPos.blockToSectionCoord((int) Math.floor(state.z));

        long base = SectionPos.asLong(sx, sy, sz);
        if (VISIBLE.contains(base)) return true;

        // Conservative 3x3x3 neighborhood: an entity can overlap section borders.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    if (VISIBLE.contains(SectionPos.asLong(sx + dx, sy + dy, sz + dz))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
