package dev.kardipopo.rickportalgun;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class RickChallenge {
    private static final int COURSE_TIMEOUT_TICKS = 12_000;
    private static final int[] HEIGHTS = {0, 0, 1, 0, 1, 2, 1};
    private static final int[] LATERAL = {0, 0, 1, -1, 0, 1, 0};
    private static final Map<UUID, ActiveChallenge> ACTIVE = new HashMap<>();

    private RickChallenge() {}

    public static void initialize() {
        ServerTickEvents.END_WORLD_TICK.register(RickChallenge::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for (ActiveChallenge challenge : ACTIVE.values()) {
                restore(challenge.world, challenge);
            }
            ACTIVE.clear();
        });
    }

    public static boolean hasPortalGun(ServerPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.getStack(i).isOf(ModItems.PORTAL_GUN)) return true;
        }
        return false;
    }

    public static int start(ServerPlayerEntity player) {
        if (player.isCreative() || player.isSpectator()) {
            player.sendMessage(Text.literal("§a[Рик C-137] §fНет-нет. Сначала режим выживания."));
            return 0;
        }
        if (hasPortalGun(player)) {
            player.sendMessage(Text.literal("§a[Рик C-137] §fУ тебя уже есть пушка. Не потеряй её, Морти."));
            return 1;
        }
        if (ACTIVE.containsKey(player.getUuid())) {
            player.sendMessage(Text.literal("§a[Рик C-137] §fТы уже проходишь испытание. Прыгай по зелёным платформам!"));
            return 1;
        }

        ServerWorld world = (ServerWorld) player.getEntityWorld();
        BlockPos origin = player.getBlockPos();
        int baseY = Math.min(origin.getY() + 5, world.getBottomY() + world.getHeight() - 12);
        if (baseY < world.getBottomY() + 8) {
            player.sendMessage(Text.literal("§c[Рик C-137] Здесь не получится собрать курс. Перейди в обычное открытое место."));
            return 0;
        }

        // Try the direction the player faces first, then rotate until a clear line is found.
        int yawDirection = Math.floorMod(Math.round(player.getYaw() / 90.0f), 4);
        int[] dxByDirection = {0, -1, 0, 1};
        int[] dzByDirection = {1, 0, -1, 0};
        Course course = null;

        for (int turn = 0; turn < 4 && course == null; turn++) {
            int direction = (yawDirection + turn) % 4;
            int forwardX = dxByDirection[direction];
            int forwardZ = dzByDirection[direction];
            int sideX = -forwardZ;
            int sideZ = forwardX;
            List<BlockPos> checkpoints = new ArrayList<>();
            List<BlockPos> blocks = new ArrayList<>();
            Set<BlockPos> unique = new HashSet<>();
            boolean clear = true;

            for (int i = 0; i < HEIGHTS.length && clear; i++) {
                BlockPos center = new BlockPos(
                        origin.getX() + forwardX * (2 + i * 3) + sideX * LATERAL[i],
                        baseY + HEIGHTS[i],
                        origin.getZ() + forwardZ * (2 + i * 3) + sideZ * LATERAL[i]
                );
                checkpoints.add(center);

                // Each checkpoint is two blocks wide, so a missed pixel-perfect landing isn't fatal.
                for (int side = 0; side < 2; side++) {
                    BlockPos platform = center.add(sideX * side, 0, sideZ * side);
                    if (!unique.add(platform)) continue;
                    if (!world.getBlockState(platform).isAir()
                            || !world.getBlockState(platform.up()).isAir()
                            || !world.getBlockState(platform.up(2)).isAir()) {
                        clear = false;
                        break;
                    }
                    blocks.add(platform);
                }
            }

            if (clear) course = new Course(checkpoints, blocks);
        }

        if (course == null) {
            player.sendMessage(Text.literal("§c[Рик C-137] Нет свободной площадки для курса. Отойди от деревьев/построек и повтори /rick challenge."));
            return 0;
        }

        Map<BlockPos, BlockState> originals = new HashMap<>();
        for (BlockPos block : course.blocks()) {
            originals.put(block, world.getBlockState(block));
            world.setBlockState(block, Blocks.LIME_STAINED_GLASS.getDefaultState());
        }

        Vec3d returnPosition = new Vec3d(player.getX(), player.getY(), player.getZ());
        float returnYaw = player.getYaw();
        float returnPitch = player.getPitch();
        BlockPos start = course.checkpoints().get(0);
        player.teleport(world, start.getX() + 0.5, start.getY() + 1.0, start.getZ() + 0.5,
                Set.of(), player.getYaw(), player.getPitch(), true);

        ACTIVE.put(player.getUuid(), new ActiveChallenge(world, course.checkpoints(), course.blocks(),
                originals, returnPosition, returnYaw, returnPitch));
        player.sendMessage(Text.literal("§a[Рик C-137] §fЛадно, Морти! Доберись до конца моего паркура. Зелёные платформы — твоя дорога."));
        player.sendMessage(Text.literal("§a[Рик C-137] §fПрыгай через промежутки, не падай. Я верну тебя назад, когда закончишь. Время пошло."));
        player.sendMessage(Text.literal("§7Испытание: 0/" + HEIGHTS.length + " платформ"));
        return 1;
    }

    private static void tick(ServerWorld world) {
        Iterator<Map.Entry<UUID, ActiveChallenge>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ActiveChallenge> entry = iterator.next();
            ActiveChallenge challenge = entry.getValue();
            if (challenge.world != world) continue;

            ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(entry.getKey());
            if (player == null || player.isRemoved()) {
                restore(world, challenge);
                iterator.remove();
                continue;
            }
            if (player.getEntityWorld() != world) {
                restore(world, challenge);
                iterator.remove();
                continue;
            }

            challenge.ticksRemaining--;
            if (challenge.ticksRemaining <= 0) {
                finish(world, player, challenge, false);
                iterator.remove();
                continue;
            }

            if (player.getY() < challenge.lowestY - 2.5) {
                finish(world, player, challenge, false);
                iterator.remove();
                continue;
            }

            if (challenge.progress < challenge.checkpoints.size()
                    && standingNear(player, challenge.checkpoints.get(challenge.progress))) {
                challenge.progress++;
                if (challenge.progress < challenge.checkpoints.size()) {
                    player.sendMessage(Text.literal("§a[Рик C-137] §fНеплохо. §e" + challenge.progress
                            + "/" + challenge.checkpoints.size() + "§f. Не начинай гордиться."));
                } else {
                    finish(world, player, challenge, true);
                    iterator.remove();
                }
            }
        }
    }

    private static boolean standingNear(ServerPlayerEntity player, BlockPos platform) {
        double targetY = platform.getY() + 1.0;
        if (Math.abs(player.getY() - targetY) > 1.15) return false;
        for (int side = 0; side < 2; side++) {
            double dx = player.getX() - (platform.getX() + 0.5);
            double dz = player.getZ() - (platform.getZ() + 0.5);
            // The second block lies sideways from the checkpoint center.
            if (dx * dx + dz * dz <= 1.65 * 1.65) return true;
            break;
        }
        return false;
    }

    private static void finish(ServerWorld world, ServerPlayerEntity player, ActiveChallenge challenge, boolean success) {
        restore(world, challenge);
        player.teleport(world, challenge.returnPosition.x, challenge.returnPosition.y, challenge.returnPosition.z,
                Set.of(), challenge.returnYaw, challenge.returnPitch, true);

        if (!success) {
            player.sendMessage(Text.literal("§c[Рик C-137] Время вышло. Я не нянька. Попробуй ещё раз: /rick challenge"));
            return;
        }

        net.minecraft.item.ItemStack reward = new net.minecraft.item.ItemStack(ModItems.PORTAL_GUN);
        if (!player.getInventory().insertStack(reward)) player.dropItem(reward, false);
        player.sendMessage(Text.literal("§a[Рик C-137] §fЛадно, ты справился. Забирай портальную пушку C-137. Только не направляй её себе в лицо."));
        player.sendMessage(Text.literal("§7ПКМ — выстрелить, Shift + ПКМ — убрать порталы, F8 в руке с пушкой — настройки маршрута."));
    }

    private static void restore(ServerWorld world, ActiveChallenge challenge) {
        for (Map.Entry<BlockPos, BlockState> entry : challenge.originals.entrySet()) {
            if (world.getBlockState(entry.getKey()).isOf(Blocks.LIME_STAINED_GLASS)) {
                world.setBlockState(entry.getKey(), entry.getValue());
            }
        }
    }

    private record Course(List<BlockPos> checkpoints, List<BlockPos> blocks) {}

    private static final class ActiveChallenge {
        private final ServerWorld world;
        private final List<BlockPos> checkpoints;
        private final List<BlockPos> blocks;
        private final Map<BlockPos, BlockState> originals;
        private final Vec3d returnPosition;
        private final float returnYaw;
        private final float returnPitch;
        private final int lowestY;
        private int progress;
        private int ticksRemaining = COURSE_TIMEOUT_TICKS;

        private ActiveChallenge(ServerWorld world, List<BlockPos> checkpoints, List<BlockPos> blocks,
                                Map<BlockPos, BlockState> originals, Vec3d returnPosition,
                                float returnYaw, float returnPitch) {
            this.world = world;
            this.checkpoints = checkpoints;
            this.blocks = blocks;
            this.originals = originals;
            this.returnPosition = returnPosition;
            this.returnYaw = returnYaw;
            this.returnPitch = returnPitch;
            this.lowestY = checkpoints.stream().mapToInt(BlockPos::getY).min().orElse(0);
        }
    }
}
