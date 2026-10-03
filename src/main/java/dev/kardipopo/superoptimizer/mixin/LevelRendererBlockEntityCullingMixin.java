package dev.kardipopo.superoptimizer.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.kardipopo.superoptimizer.CullingContext;
import dev.kardipopo.superoptimizer.SelfHealingManager;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererBlockEntityCullingMixin {
    @Inject(method = "submitBlockEntities", at = @At("HEAD"))
    private void superoptimizer$beginBlockEntityCulling(
            PoseStack poseStack,
            LevelRenderState levelRenderState,
            SubmitNodeCollector collector,
            CallbackInfo ci) {
        CullingContext.begin((LevelRenderer) (Object) this, true);
    }

    @Inject(method = "submitBlockEntities", at = @At("TAIL"))
    private void superoptimizer$endBlockEntityCulling(
            PoseStack poseStack,
            LevelRenderState levelRenderState,
            SubmitNodeCollector collector,
            CallbackInfo ci) {
        CullingContext.end();
    }

    @Redirect(
        method = "submitBlockEntities",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher;submit(Lnet/minecraft/client/renderer/blockentity/state/BlockEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V"
        )
    )
    private void superoptimizer$filterBlockEntitySubmit(
            BlockEntityRenderDispatcher dispatcher,
            BlockEntityRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState cameraRenderState) {
        try {
            if (CullingContext.shouldSubmitBlockEntity(state)) {
                dispatcher.submit(state, poseStack, collector, cameraRenderState);
            }
        } catch (Throwable t) {
            SelfHealingManager.reportFailure("block-entity-culling", t);
            dispatcher.submit(state, poseStack, collector, cameraRenderState);
        }
    }
}
