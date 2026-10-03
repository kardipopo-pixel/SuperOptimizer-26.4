package dev.kardipopo.superoptimizer.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import dev.kardipopo.superoptimizer.CullingContext;
import dev.kardipopo.superoptimizer.SelfHealingManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererEntityCullingMixin {
    @Inject(method = "submitEntities", at = @At("HEAD"))
    private void superoptimizer$beginEntityCulling(
            PoseStack poseStack,
            LevelRenderState levelRenderState,
            SubmitNodeCollector collector,
            CallbackInfo ci) {
        CullingContext.begin((LevelRenderer) (Object) this, false);
    }

    @Inject(method = "submitEntities", at = @At("TAIL"))
    private void superoptimizer$endEntityCulling(
            PoseStack poseStack,
            LevelRenderState levelRenderState,
            SubmitNodeCollector collector,
            CallbackInfo ci) {
        CullingContext.end();
    }

    @Redirect(
        method = "submitEntities",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lnet/minecraft/client/renderer/state/level/CameraRenderState;DDDLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V"
        )
    )
    private void superoptimizer$filterEntitySubmit(
            EntityRenderDispatcher dispatcher,
            EntityRenderState state,
            CameraRenderState cameraRenderState,
            double x,
            double y,
            double z,
            PoseStack poseStack,
            SubmitNodeCollector collector) {
        try {
            if (CullingContext.shouldSubmit(state)) {
                dispatcher.submit(state, cameraRenderState, x, y, z, poseStack, collector);
            }
        } catch (Throwable t) {
            SelfHealingManager.reportFailure("entity-culling", t);
            dispatcher.submit(state, cameraRenderState, x, y, z, poseStack, collector);
        }
    }
}
