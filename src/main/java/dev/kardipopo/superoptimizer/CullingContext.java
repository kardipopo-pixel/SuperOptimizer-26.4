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
    private static boolean visibilityPrepared;
    private static LevelRenderer preparedRenderer;
    private static boolean entityPassOpen;

    private static double lastCamX, lastCamY, lastCamZ;
    private static boolean haveCamera;

    private static long entityChecks, entityCulled;
    private static long blockEntityChecks, blockEntityCulled;
    private static long entityLodApplied;
    private static String disabledReason = "неизвестно";

    private CullingContext() {}

    public static void begin(LevelRenderer renderer, boolean blockEntities) {
        SuperOptimizerConfig config = SuperOptimizerClient.config();

        boolean reuseVisibility = blockEntities && entityPassOpen
                && preparedRenderer == renderer && visibilityPrepared
                && config != null && config.blockEntityCulling;

        if (!reuseVisibility) {
            VISIBLE.clear();
            visibilityPrepared = false;
        }

        active = config != null && config.enabled
                && (blockEntities ? config.blockEntityCulling : (config.entityCulling || config.entityDistanceCulling));

        if (!active) {
            disabledReason = config == null ? "конфигурация не загружена"
                    : !config.enabled ? "оптимизатор выключен"
                    : blockEntities ? "block entity culling выключен" : "entity culling выключен";
            if (!blockEntities) entityPassOpen = false;
            lastActive = false;
            return;
        }

        if (config.disableCullingWithIris && config.irisCompatibility
                && FabricLoader.getInstance().isModLoaded("iris")
                && IrisBridge.shadersInUse()) {
            active = false;
            disabledReason = "активный Iris shader pack";
            if (!blockEntities) entityPassOpen = false;
            lastActive = false;
            return;
        }

        if (config.disableCullingWithEntityCullingMod && config.entityCulling
                && FabricLoader.getInstance().isModLoaded("entityculling")) {
            active = false;
            disabledReason = "обнаружен Entity Culling";
            if (!blockEntities) entityPassOpen = false;
            lastActive = false;
            return;
        }

        var cameraEntity = Minecraft.getInstance().getCameraEntity();
        if (cameraEntity != null && config.pauseDuringCameraMotion && haveCamera) {
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
                if (!blockEntities) entityPassOpen = false;
                lastActive = false;
                return;
            }
        }

        if (cameraEntity != null) {
            lastCamX = cameraEntity.getX();
            lastCamY = cameraEntity.getY();
            lastCamZ = cameraEntity.getZ();
            haveCamera = true;
        } else {
            haveCamera = false;
        }

        if (!reuseVisibility) {
            for (SectionRenderDispatcher.RenderSection section : renderer.visibleSections()) {
                if (section != null) {
                    VISIBLE.add(SectionPos.asLong(section.getRenderOrigin()));
                }
            }
            preparedRenderer = renderer;
            visibilityPrepared = true;
        }

        entityPassOpen = !blockEntities;
        if (!lastActive) {
            SuperOptimizerLog.info("Culling активирован: " + VISIBLE.size() + " видимых секций.");
        }
        lastActive = true;
        disabledReason = "";
    }

    public static void end() {
        SuperOptimizerConfig config = SuperOptimizerClient.config();
        if (entityPassOpen && config != null && config.blockEntityCulling) {
            active = false;
            return;
        }
        VISIBLE.clear();
        visibilityPrepared = false;
        preparedRenderer = null;
        entityPassOpen = false;
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

        SuperOptimizerConfig config = SuperOptimizerClient.config();
        var camera = Minecraft.getInstance().getCameraEntity();
        if (camera != null && config != null) {
            double dx = state.x - camera.getX();
            double dy = state.y - camera.getY();
            double dz = state.z - camera.getZ();
            double distanceSq = dx * dx + dy * dy + dz * dz;
            double distance = Math.sqrt(distanceSq);

            if (config.entityDistanceCulling) {
                double limit = AdaptivePerformanceController.isAdaptive()
                        ? AdaptivePerformanceController.entityDistance()
                        : config.entityRenderDistance;
                if (distance > limit) {
                    entityCulled++;
                    return false;
                }
            }

            if (config.entityLod && distance >= config.entityLodDistance) {
                applyLod(state, config);
            }

            if (config.skipNearEntityCulling) {
                double near = config.nearEntityDistance;
                if (distanceSq < near * near) return true;
            }

            if (config.directionalEntityCulling && distanceSq > 64.0) {
                double length = distance;
                if (length > 0.0001) {
                    net.minecraft.world.phys.Vec3 view = camera.getViewVector(1.0f);
                    double dot = (view.x * dx + view.y * dy + view.z * dz) / length;
                    if (dot < -0.25) {
                        entityCulled++;
                        return false;
                    }
                }
            }
        }

        int sx = SectionPos.blockToSectionCoord((int) Math.floor(state.x));
        int sy = SectionPos.blockToSectionCoord((int) Math.floor(state.y));
        int sz = SectionPos.blockToSectionCoord((int) Math.floor(state.z));

        boolean visible = VISIBLE.contains(SectionPos.asLong(sx, sy, sz))
                || neighborhoodVisible(sx, sy, sz);

        if (!visible) entityCulled++;
        return visible;
    }

    private static void applyLod(EntityRenderState state, SuperOptimizerConfig config) {
        String id = "";
        try {
            if (state.entityType != null) {
                var key = BuiltInRegistries.ENTITY_TYPE.getKey(state.entityType);
                if (key != null) id = key.toString();
            }
        } catch (Throwable ignored) {}

        // Do not alter entities with explicit gameplay-relevant overlays.
        if (config.hideDistantNames) {
            state.nameTag = null;
            state.scoreText = null;
        }
        if (config.hideDistantShadows) {
            state.shadowRadius = 0.0f;
            state.shadowPieces.clear();
        }

        // Cheap specialised LOD gate for item frames and armor stands.
        if ((id.equals("minecraft:item_frame") || id.equals("minecraft:glow_item_frame"))
                && config.frameCulling) {
            entityLodApplied++;
        } else if (id.equals("minecraft:armor_stand") && config.armorStandCulling) {
            entityLodApplied++;
        } else {
            entityLodApplied++;
        }
    }

    public static boolean shouldSubmitBlockEntity(BlockEntityRenderState state) {
        blockEntityChecks++;
        if (!active || state == null || state.blockPos == null || state.blockEntityType == null) return true;

        var id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(state.blockEntityType);
        if (id == null || !SAFE_BLOCK_ENTITY_TYPES.contains(id.toString())) return true;

        SuperOptimizerConfig config = SuperOptimizerClient.config();
        String type = id.toString();

        if (type.contains("sign") && !config.signCulling) return true;
        if (type.contains("chest") && !config.chestCulling) return true;
        if (type.equals("minecraft:hopper") && !config.hopperCulling) return true;

        var camera = Minecraft.getInstance().getCameraEntity();
        if (camera != null && config.entityDistanceCulling) {
            double dx = state.blockPos.getX() + 0.5 - camera.getX();
            double dy = state.blockPos.getY() + 0.5 - camera.getY();
            double dz = state.blockPos.getZ() + 0.5 - camera.getZ();
            double limit = AdaptivePerformanceController.isAdaptive()
                    ? AdaptivePerformanceController.entityDistance()
                    : config.entityRenderDistance;
            if (dx * dx + dy * dy + dz * dz > limit * limit) {
                blockEntityCulled++;
                return false;
            }
        }

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
    public static long entityLodApplied() { return entityLodApplied; }

    public static void resetStats() {
        entityChecks = entityCulled = 0;
        blockEntityChecks = blockEntityCulled = 0;
        entityLodApplied = 0;
    }
}