package dev.kardipopo.rickportalgun.mixin;

import dev.kardipopo.rickportalgun.ModItems;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"))
    private void rpg$push(AbstractClientPlayerEntity player, float tickProgress, float pitch,
                           Hand hand, float swingProgress, ItemStack itemStack, float equipProgress,
                           MatrixStack matrices, ItemRenderState renderState, int light,
                           CallbackInfo ci) {
        if (!itemStack.isOf(ModItems.PORTAL_GUN)) return;

        float time = (player.age + tickProgress) * 0.08f;
        float use = Math.max(0.0f, 1.0f - swingProgress * 4.0f);
        float idle = (float) Math.sin(time) * 0.012f;
        float sway = (float) Math.cos(time * 0.77f) * 0.7f;

        matrices.push();
        matrices.translate(0.015 + idle, -0.015 - use * 0.055f, -0.02);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(2.0f + use * 8.0f));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sway + use * 2.5f));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-2.0f + sway * 0.55f));
        matrices.scale(0.92f, 0.92f, 0.92f);
    }

    @Inject(method = "renderFirstPersonItem", at = @At("RETURN"))
    private void rpg$pop(AbstractClientPlayerEntity player, float tickProgress, float pitch,
                          Hand hand, float swingProgress, ItemStack itemStack, float equipProgress,
                          MatrixStack matrices, ItemRenderState renderState, int light,
                          CallbackInfo ci) {
        if (itemStack.isOf(ModItems.PORTAL_GUN)) matrices.pop();
    }
}
