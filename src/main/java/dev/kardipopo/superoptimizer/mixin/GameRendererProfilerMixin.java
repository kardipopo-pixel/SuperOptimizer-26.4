package dev.kardipopo.superoptimizer.mixin;

import dev.kardipopo.superoptimizer.AdaptivePerformanceController;
import dev.kardipopo.superoptimizer.PerformanceProfiler;
import dev.kardipopo.superoptimizer.SelfHealingManager;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererProfilerMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void superoptimizer$frameStart(CallbackInfo ci) {
        try {
            PerformanceProfiler.frameStart();
        } catch (Throwable t) {
            SelfHealingManager.reportFailure("frame-profiler", t);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void superoptimizer$frameEnd(CallbackInfo ci) {
        try {
            PerformanceProfiler.frameEnd();
            AdaptivePerformanceController.onFrameBoundary();
        } catch (Throwable t) {
            SelfHealingManager.reportFailure("frame-profiler", t);
        }
    }
}