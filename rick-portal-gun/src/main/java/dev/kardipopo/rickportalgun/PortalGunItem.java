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

        ServerWorld serverWorld = (ServerWorld) world;
        UUID owner = user.getUuid();

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
        Vec3d pos = blockHit.getPos().add(Vec3d.of(normal.getVector()).multiply(0.02));

        ArmorStandEntity stand = EntityType.ARMOR_STAND.create(serverWorld, SpawnReason.TRIGGERED);
        if (stand == null) return ActionResult.FAIL;

        char side = existing.isEmpty() ? 'A' : 'B';
        stand.refreshPositionAndAngles(pos.x, pos.y, pos.z, 0.0f, 0.0f);
        stand.setInvisible(true);
        stand.setInvulnerable(true);
        stand.setNoGravity(true);
        stand.setCustomNameVisible(false);
        stand.setCustomName(new PortalMarker(owner, side, normal, null).encode());
        serverWorld.spawnEntity(stand);

        if (side == 'B') {
            ArmorStandEntity first = existing.get(0);
            PortalMarker firstData = PortalMarker.read(first);
            if (firstData != null) {
                Vec3d firstPos = new Vec3d(first.getX(), first.getY(), first.getZ());
                first.setCustomName(new PortalMarker(firstData.owner(), firstData.side(), firstData.normal(), pos).encode());
                stand.setCustomName(new PortalMarker(owner, 'B', normal, firstPos).encode());
            }
        }

        world.playSound(null, pos.x, pos.y, pos.z,
                ModSounds.PORTAL_CREATE, SoundCategory.PLAYERS, 1.0f, side == 'A' ? 1.0f : 1.08f);
        return ActionResult.SUCCESS;
    }

    private static int removePortals(ServerWorld world, UUID owner) {
        List<ArmorStandEntity> matches = world.getEntitiesByType(
                EntityType.ARMOR_STAND, WORLD_BOX,
                stand -> {
                    PortalMarker marker = PortalMarker.read(stand);
                    return marker != null && marker.owner().equals(owner);
                });
        for (ArmorStandEntity stand : matches) stand.discard();
        return matches.size();
    }

    private static List<ArmorStandEntity> findPortals(ServerWorld world, UUID owner) {
        return world.getEntitiesByType(
                EntityType.ARMOR_STAND, WORLD_BOX,
                stand -> {
                    PortalMarker marker = PortalMarker.read(stand);
                    return marker != null && marker.owner().equals(owner);
                });
    }
}
