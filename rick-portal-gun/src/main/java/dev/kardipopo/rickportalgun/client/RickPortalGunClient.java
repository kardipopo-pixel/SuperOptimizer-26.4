package dev.kardipopo.rickportalgun.client;

import dev.kardipopo.rickportalgun.ModItems;
import dev.kardipopo.rickportalgun.ModSounds;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;

public final class RickPortalGunClient implements ClientModInitializer {
    private static boolean holding = false;
    private static long animationTicks = 0L;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(RickPortalGunClient::tick);
        PortalVisualRenderer.initialize();
    }

    private static void tick(MinecraftClient client) {
        animationTicks++;
        ClientPlayerEntity player = client.player;
        if (player == null) {
            holding = false;
            return;
        }

        ItemStack stack = player.getMainHandStack();
        boolean nowHolding = stack.isOf(ModItems.PORTAL_GUN);
        if (nowHolding && !holding) {
            player.playSound(ModSounds.GUN_DRAW, 0.72f, 1.0f);
        }
        holding = nowHolding;
    }

    public static float time(float tickDelta) {
        return (animationTicks + tickDelta) * 0.08f;
    }
}
