package com.depthgate.client.mixin;

import com.depthgate.DepthGateConfig;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.particle.ParticleTextureSheet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Queue;

@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {
    @Shadow @Final private Map<ParticleTextureSheet, Queue<Particle>> particles;

    @Unique private int depthgate$count = 0;
    @Unique private long depthgate$lastCount = 0L;

    @Inject(method = "addParticle(Lnet/minecraft/client/particle/Particle;)V", at = @At("HEAD"), cancellable = true)
    private void depthgate$capParticles(Particle particle, CallbackInfo ci) {
        if (particle == null) return;
        DepthGateConfig config = DepthGateConfig.get();
        if (!config.particleSuppressionEnabled || config.particleCap <= 0) return;

        long now = System.nanoTime();
        if (now - depthgate$lastCount > 50_000_000L) {
            int total = 0;
            for (Queue<Particle> queue : this.particles.values()) {
                total += queue.size();
            }
            depthgate$count = total;
            depthgate$lastCount = now;
        }
        if (depthgate$count >= config.particleCap) {
            ci.cancel();
        } else {
            depthgate$count++;
        }
    }
}
