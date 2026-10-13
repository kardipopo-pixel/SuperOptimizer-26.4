package dev.kardipopo.rickportalgun;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;
import java.util.UUID;

public final class PortalGunItem extends Item {
    private static final double RANGE = 64.0;
    private static final Box WORLD_BOX = new Box(-30_000_000, -64, -30_000_000, 30_000_000, 384, 30_000_000);

    public PortalGunItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient()) return ActionResult.SUCCESS;
        if (!(world instanceof ServerWorld serverWorld)) return ActionResult.FAIL;

        UUID owner = user.getUuid();
        PortalServer.PlayerConfig config = PortalServer.getConfig(owner);

        if (user.isSneaking()) {
            int removed = removePortals(serverWorld, owner);
            if (removed > 0) {
                world.playSound(null, user.getX(), user.getY(), user.getZ(),
                        ModSounds.PORTAL_REMOVE, SoundCategory.PLAYERS, 1.0f, 1.0f);
            }
            return ActionResult.SUCCESS;
        }

        List<ArmorStandEntity> existing = findPortals(serverWorld, owner);
        if (existing.size() >= 2) {
            user.sendMessage(Text.translatable("message.rickportalgun.already_two"), true);
            return ActionResult.SUCCESS;
        }

        HitResult result = user.raycast(RANGE, 1.0f, false);
        if (!(result instanceof BlockHitResult blockHit) || result.getType() != HitResult.Type.BLOCK) {
            return ActionResult.PASS;
        }

        Direction normal = blockHit.getSide();
        BlockPos block = blockHit.getBlockPos();
        Vec3d surface = new Vec3d(block.getX() + 0.5, block.getY() + 0.5, block.getZ() + 0.5);
        if (normal.getAxis() == Direction.Axis.X) {
            surface = new Vec3d(block.getX() + (normal == Direction.EAST ? 1.0 : 0.0), surface.y, surface.z);
        } else if (normal.getAxis() == Direction.Axis.Y) {
            surface = new Vec3d(surface.x, block.getY() + (normal == Direction.UP ? 1.0 : 0.0), surface.z);
        } else {
            surface = new Vec3d(surface.x, surface.y, block.getZ() + (normal == Direction.SOUTH ? 1.0 : 0.0));
        }
        Vec3d pos = surface.add(Vec3d.of(normal.getVector()).multiply(0.025));

        String targetDimension = null;
        Vec3d targetPos = null;
        char side = existing.isEmpty() ? 'A' : 'B';

        if (config.destinationMode()) {
            if (PortalServer.resolveWorld(serverWorld.getServer(), config.dimension()) == null) {
                user.sendMessage(Text.literal("§c[Рик C-137] Измерение назначения не найдено. Открой F8 и выбери его снова."), false);
                return ActionResult.FAIL;
            }
            targetDimension = config.dimension();
            targetPos = new Vec3d(config.x() + 0.5, config.y() + 0.05, config.z() + 0.5);
            side = 'D';
        }

        ArmorStandEntity stand = EntityType.ARMOR_STAND.create(serverWorld, SpawnReason.TRIGGERED);
        if (stand == null) return ActionResult.FAIL;

        stand.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0.0f, 0.0f);
        stand.setInvisible(true);
        stand.setInvulnerable(true);
        stand.setNoGravity(true);
        stand.setCustomNameVisible(false);
        stand.setCustomName(new PortalMarker(owner, side, normal, targetDimension, targetPos).encode());
        if (!serverWorld.spawnEntity(stand)) {
            stand.discard();
            return ActionResult.FAIL;
        }

        if (config.destinationMode()) {
            user.sendMessage(Text.literal("§a[Рик C-137] §fПортал настроен на §e" + config.dimension()
                    + " " + config.x() + " " + config.y() + " " + config.z()), false);
        } else if (side == 'B') {
            ArmorStandEntity first = existing.get(0);
            PortalMarker firstData = PortalMarker.read(first);
            if (firstData != null) {
                String currentDimension = serverWorld.getRegistryKey().getValue().toString();
                Vec3d firstPos = new Vec3d(first.getX(), first.getY(), first.getZ());
                Vec3d firstExit = firstPos.add(Vec3d.of(firstData.normal().getVector()).multiply(2.75));
                Vec3d secondExit = pos.add(Vec3d.of(normal.getVector()).multiply(2.75));

                first.setCustomName(new PortalMarker(owner, firstData.side(), firstData.normal(),
                        currentDimension, secondExit).encode());
                stand.setCustomName(new PortalMarker(owner, 'B', normal,
                        currentDimension, firstExit).encode());
            } else {
                stand.setCustomName(new PortalMarker(owner, 'B', normal, null, null).encode());
            }
        }

        world.playSound(null, pos.x, pos.y, pos.z,
                ModSounds.PORTAL_CREATE, SoundCategory.PLAYERS, 1.0f, side == 'A' ? 1.0f : 1.08f);
        return ActionResult.SUCCESS;
    }

    private static int removePortals(ServerWorld world, UUID owner) {
        List<MarkerEntity> matches = world.getEntitiesByType(
                EntityType.ARMOR_STAND, WORLD_BOX,
                stand -> {
                    PortalMarker marker = PortalMarker.read(stand);
                    return marker != null && marker.owner().equals(owner);
                });
        for (ArmorStandEntity stand : matches) stand.discard();
        return matches.size();
    }

    private static List<MarkerEntity> findPortals(ServerWorld world, UUID owner) {
        return world.getEntitiesByType(
                EntityType.MARKER, WORLD_BOX,
                stand -> {
                    PortalMarker marker = PortalMarker.read(stand);
                    return marker != null && marker.owner().equals(owner);
                });
    }
}
