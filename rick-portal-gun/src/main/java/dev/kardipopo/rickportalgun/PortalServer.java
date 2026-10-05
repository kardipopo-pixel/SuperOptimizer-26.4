package dev.kardipopo.rickportalgun;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Box;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PortalServer {
    private static final Map<UUID, Integer> COOLDOWNS = new HashMap<>();

    private PortalServer() {}

    public static void initialize() {
        ServerTickEvents.END_WORLD_TICK.register(PortalServer::tick);
    }

    private static void tick(ServerWorld world) {
        COOLDOWNS.replaceAll((uuid, value) -> value - 1);
        COOLDOWNS.entrySet().removeIf(entry -> entry.getValue() <= 0);

        for (ServerPlayerEntity player : world.getPlayers()) {
            if (COOLDOWNS.containsKey(player.getUuid())) continue;

            Box area = player.getBoundingBox().expand(1.65);
            for (ArmorStandEntity stand : world.getEntitiesByType(EntityType.ARMOR_STAND, area, PortalMarker::is)) {
                PortalMarker marker = PortalMarker.read(stand);
                if (marker == null || marker.target() == null) continue;
                if (player.squaredDistanceTo(stand) > 2.25 * 2.25) continue;

                teleport(player, marker, world);
                break;
            }
        }
    }

    private static void teleport(ServerPlayerEntity player, PortalMarker source, ServerWorld world) {
        double x = source.target().x + source.normal().getOffsetX() * 1.35;
        double y = source.target().y + source.normal().getOffsetY() * 1.35;
        double z = source.target().z + source.normal().getOffsetZ() * 1.35;

        player.setPosition(x, y, z);
        player.setVelocity(0.0, 0.0, 0.0);
        COOLDOWNS.put(player.getUuid(), 12);

        world.playSound(null, x, y, z,
                ModSounds.PORTAL_CREATE, SoundCategory.PLAYERS, 0.65f, 0.72f);
    }
}
