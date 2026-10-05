package dev.kardipopo.rickportalgun;

import net.fabricmc.api.ModInitializer;

public final class RickPortalGun implements ModInitializer {
    public static final String MOD_ID = "rickportalgun";

    @Override
    public void onInitialize() {
        ModSounds.initialize();
        ModItems.initialize();
        PortalServer.initialize();
    }
}
