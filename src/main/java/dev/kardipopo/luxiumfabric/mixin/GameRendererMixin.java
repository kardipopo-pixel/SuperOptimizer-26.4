package dev.kardipopo.luxiumfabric.mixin;

import dev.kardipopo.luxiumfabric.LuxiumConfig;
import dev.kardipopo.luxiumfabric.LuxiumFabricClient;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "update", at = @At("TAIL"))
    private void luxiumfabric$appendPostEffect(DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!LuxiumConfig.enabled) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;
        GameRenderer renderer = (GameRenderer) (Object) this;
        var effects = renderer.getRequestedPostEffects();
        if (!effects.contains(LuxiumFabricClient.POST_EFFECT)) effects.add(LuxiumFabricClient.POST_EFFECT);
    }
}