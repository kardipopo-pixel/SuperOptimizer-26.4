package dev.kardipopo.luxiumfabric;

import net.fabricmc.api.ClientModInitializer;

public final class LuxiumFabricClient implements ClientModInitializer {
    public static final String MOD_ID = "luxiumfabric";
    public static final net.minecraft.resources.Identifier POST_EFFECT =
            net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID, "main");

    @Override
    public void onInitializeClient() { LuxiumConfig.load(); }
}