package com.depthgate.client.mixin;
import com.depthgate.DepthGateConfig;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.particle.ParticleTextureSheet;
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
    @Shadow private Map<ParticleTextureSheet, Queue<Particle>> particles;
    @Unique private int cachedCount = 0;
    @Unique private long lastCountTick = 0;
    @Inject(method = "addParticle(Lnet/minecraft/client/particle/Particle;)V", at = @At("HEAD"), cancellable = true)
    private void depthgate_suppressParticles(Particle particle, CallbackInfo ci) {
        if (particle == null) return;
        DepthGateConfig config = DepthGateConfig.get();
        if (!config.particleSuppressionEnabled) return;
        long now = System.nanoTime();
        if (now - lastCountTick > 50_000_000L) {
            cachedCount = 0;
            for (Queue<Particle> queue : this.particles.values()) {
                cachedCount += queue.size();
            }
            lastCountTick = now;
        }
        if (cachedCount >= config.particleCap) {
            ci.cancel();
        } else {
            cachedCount++;
        }
    }
}

