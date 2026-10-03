package dev.kardipopo.superoptimizer.mixin;

import dev.kardipopo.superoptimizer.ParticleOptimizer;
import dev.kardipopo.superoptimizer.SelfHealingManager;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineOptimizerMixin {
    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    private void superoptimizer$distanceGate(
            ParticleOptions options,
            double x, double y, double z,
            double vx, double vy, double vz,
            CallbackInfoReturnable<Particle> cir) {
        try {
            if (!ParticleOptimizer.allowCreation(x, y, z)) {
                cir.setReturnValue(null);
            }
        } catch (Throwable t) {
            SelfHealingManager.reportFailure("particles", t);
        }
    }

    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void superoptimizer$countGate(Particle particle, CallbackInfo ci) {
        try {
            if (!ParticleOptimizer.allowAdd()) {
                ci.cancel();
            }
        } catch (Throwable t) {
            SelfHealingManager.reportFailure("particles", t);
        }
    }
}