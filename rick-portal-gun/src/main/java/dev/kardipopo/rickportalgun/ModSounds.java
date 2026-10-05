package dev.kardipopo.rickportalgun;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class ModSounds {
    public static final SoundEvent PORTAL_CREATE = register("portal_create");
    public static final SoundEvent PORTAL_REMOVE = register("portal_remove");
    public static final SoundEvent GUN_DRAW = register("gun_draw");

    private static SoundEvent register(String id) {
        Identifier identifier = Identifier.of(RickPortalGun.MOD_ID, id);
        return Registry.register(Registries.SOUND_EVENT, identifier, SoundEvent.of(identifier));
    }

    public static void initialize() {}
}
