package dev.kardipopo.superoptimizer;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.Set;

public final class CullingContext {
    private static final LongOpenHashSet VISIBLE = new LongOpenHashSet();

    private static final Set<String> SAFE_BLOCK_ENTITY_TYPES = Set.of(
        "minecraft:chest",
        "minecraft:trapped_chest",
        "minecraft:ender_chest",
        "minecraft:shulker_box",
        "minecraft:furnace",
        "minecraft:blast_furnace",
        "minecraft:smoker",
        "minecraft:hopper",
        "minecraft:brewing_stand",
        "minecraft:decorated_pot",
        "minecraft:sign",
        "minecraft:hanging_sign",
        "minecraft:skull",
        "minecraft:banner",
        "minecraft:bell"
    );

    private static boolean active;
    private static boolean lastActive;
    private static double lastCamX;
    private static double lastCamY;
    private static double lastCamZ;
    private static boolean haveCamera;

    private static long entityChecks;
    private static long entityCulled;
    private static long blockEntityChecks;
    private static long blockEntityCulled;
    private static String disabledReason = "неизвестно";

    private CullingContext() {}

    public static void begin(LevelRenderer renderer, boolean blockEntities) {
        VISIBLE.clear();

        SuperOptimizerConfig config = SuperOptimizerClient.config();
        active = config != null
            && config.enabled
            && (blockEntities ? config.blockEntityCulling : config.entityCulling);

        if (!active) {
            disabledReason = config == null ? "конфигурация не загружена"
                : !config.enabled ? "оптимизатор выключен"
                : blockEntities ? "culling block entity выключен" : "culling сущностей выключен";
            lastActive = false;
            return;
        }

        if (config.disableCullingWithIris && FabricLoader.getInstance().isModLoaded("iris")) {
            active = false;
            disabledReason = "обнаружен Iris";
            lastActive = false;
            return;
        }

        if (config.disableCullingWithEntityCullingMod
                && FabricLoader.getInstance().isModLoaded("entityculling")) {
            active = false;
            disabledReason = "обнаружен Entity Culling";
            lastActive = false;
            return;
        }

        var cameraEntity = Minecraft.getInstance().getCameraEntity();
        if (cameraEntity != null) {
            if (config.pauseDuringCameraMotion && haveCamera) {
                double dx = cameraEntity.getX() - lastCamX;
                double dy = cameraEntity.getY() - lastCamY;
                double dz = cameraEntity.getZ() - lastCamZ;
                if (dx * dx + dy * dy + dz * dz > 0.0625) {
                    active = false;
                    disabledReason = "камера движется";
                    lastCamX = cameraEntity.getX();
                    lastCamY = cameraEntity.getY();
                    lastCamZ = cameraEntity.getZ();
                    haveCamera = true;
                    lastActive = false;
                    return;
                }
            }
            lastCamX = cameraEntity.getX();
            lastCamY = cameraEntity.getY();
            lastCamZ = cameraEntity.getZ();
            haveCamera = true;
        } else {
            haveCamera = false;
        }

        for (SectionRenderDispatcher.RenderSection section : renderer.visibleSections()) {
            if (section != null) {
                VISIBLE.add(SectionPos.asLong(section.getRenderOrigin()));
            }
        }

        if (!lastActive) {
            SuperOptimizerLog.info("Culling активирован: " + VISIBLE.size() + " видимых секций.");
        }
        lastActive = true;
        disabledReason = "";
    }

    public static void end() {
        VISIBLE.clear();
        active = false;
    }

    public static boolean shouldSubmit(EntityRenderState state) {
        entityChecks++;
        if (!active || state == null) return true;

        if (state.nameTag != null || state.scoreText != null
                || state.leashStates != null && !state.leashStates.isEmpty()
                || state.appearsGlowing()
                || state.outlineColor != EntityRenderState.NO_OUTLINE
                || state.boundingBoxWidth > 2.5f
                || state.boundingBoxHeight > 4.5f) {
            return true;
        }

        int sx = SectionPos.blockToSectionCoord((int) Math.floor(state.x));
        int sy = SectionPos.blockToSectionCoord((int) Math.floor(state.y));
        int sz = SectionPos.blockToSectionCoord((int) Math.floor(state.z));

        boolean visible = VISIBLE.contains(SectionPos.asLong(sx, sy, sz))
            || neighborhoodVisible(sx, sy, sz);

        if (!visible) entityCulled++;
        return visible;
    }

    public static boolean shouldSubmitBlockEntity(BlockEntityRenderState state) {
        blockEntityChecks++;
        if (!active || state == null || state.blockPos == null || state.blockEntityType == null) return true;

        var id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(state.blockEntityType);
        if (id == null || !SAFE_BLOCK_ENTITY_TYPES.contains(id.toString())) return true;

        SectionPos section = SectionPos.of(state.blockPos);
        boolean visible = VISIBLE.contains(section.asLong())
            || neighborhoodVisible(section.x(), section.y(), section.z());

        if (!visible) blockEntityCulled++;
        return visible;
    }

    private static boolean neighborhoodVisible(int sx, int sy, int sz) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (VISIBLE.contains(SectionPos.asLong(sx + dx, sy + dy, sz + dz))) return true;
                }
            }
        }
        return false;
    }

    public static boolean active() { return active; }
    public static String disabledReason() { return disabledReason; }
    public static long entityChecks() { return entityChecks; }
    public static long entityCulled() { return entityCulled; }
    public static long blockEntityChecks() { return blockEntityChecks; }
    public static long blockEntityCulled() { return blockEntityCulled; }

    public static void resetStats() {
        entityChecks = entityCulled = 0;
        blockEntityChecks = blockEntityCulled = 0;
    }
}
