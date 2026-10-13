package dev.kardipopo.rickportalgun;

import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.MarkerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public record PortalMarker(UUID owner, char side, Direction normal, String targetDimension, Vec3d target) {
    private static final String PREFIX = "rpg|";

    public static boolean is(Entity entity) {
        return entity instanceof MarkerEntity stand
                && stand.getCustomName() != null
                && stand.getCustomName().getString().startsWith(PREFIX);
    }

    public static PortalMarker read(MarkerEntity stand) {
        if (stand.getCustomName() == null) return null;
        String[] parts = stand.getCustomName().getString().split("\\|", -1);
        if (parts.length < 5 || !parts[0].equals("rpg")) return null;

        try {
            UUID owner = UUID.fromString(parts[1]);
            char side = parts[2].charAt(0);
            Direction normal = Direction.byId(parts[3]);

            // Current format: rpg|uuid|side|normal|dimension|x|y|z
            if (parts.length >= 8 && !parts[4].equals("NA")) {
                Vec3d target = new Vec3d(
                        Double.parseDouble(parts[5]),
                        Double.parseDouble(parts[6]),
                        Double.parseDouble(parts[7])
                );
                return new PortalMarker(owner, side, normal, parts[4], target);
            }

            // Read the previous build's same-dimension format for already-saved worlds.
            if (parts.length >= 7 && !parts[4].equals("NA")) {
                Vec3d target = new Vec3d(
                        Double.parseDouble(parts[4]),
                        Double.parseDouble(parts[5]),
                        Double.parseDouble(parts[6])
                );
                return new PortalMarker(owner, side, normal, null, target);
            }
            return new PortalMarker(owner, side, normal, null, null);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public Text encode() {
        if (target == null) {
            return Text.literal(PREFIX + owner + "|" + side + "|" + normal.getId() + "|NA|NA|NA|NA");
        }
        String dimension = targetDimension == null ? "NA" : targetDimension;
        return Text.literal(PREFIX + owner + "|" + side + "|" + normal.getId() + "|"
                + dimension + "|" + target.x + "|" + target.y + "|" + target.z);
    }
}
