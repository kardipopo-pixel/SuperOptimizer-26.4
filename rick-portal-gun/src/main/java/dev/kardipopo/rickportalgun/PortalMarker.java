package dev.kardipopo.rickportalgun;

import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public record PortalMarker(UUID owner, char side, Direction normal, Vec3d target) {
    private static final String PREFIX = "rpg|";

    public static boolean is(Entity entity) {
        return entity instanceof ArmorStandEntity stand
                && stand.getCustomName() != null
                && stand.getCustomName().getString().startsWith(PREFIX);
    }

    public static PortalMarker read(ArmorStandEntity stand) {
        if (stand.getCustomName() == null) return null;
        String[] parts = stand.getCustomName().getString().split("\\|");
        if (parts.length < 5 || !parts[0].equals("rpg")) return null;

        try {
            UUID owner = UUID.fromString(parts[1]);
            char side = parts[2].charAt(0);
            Direction normal = Direction.byId(Integer.parseInt(parts[3]));
            Vec3d target = null;
            if (parts.length >= 7 && !parts[4].equals("NA")) {
                target = new Vec3d(
                        Double.parseDouble(parts[4]),
                        Double.parseDouble(parts[5]),
                        Double.parseDouble(parts[6])
                );
            }
            return new PortalMarker(owner, side, normal, target);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    public Text encode() {
        String targetPart = target == null ? "NA" : target.x + "|" + target.y + "|" + target.z;
        return Text.literal(PREFIX + owner + "|" + side + "|" + normal.getId() + "|" + targetPart);
    }
}
