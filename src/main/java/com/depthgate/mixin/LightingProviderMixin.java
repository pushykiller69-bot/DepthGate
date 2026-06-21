package com.depthgate.mixin;
import com.depthgate.DepthGateConfig;
import net.minecraft.world.chunk.light.LightingProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LightingProvider.class)
public abstract class LightingProviderMixin {
    @Unique private int updatesThisTick = 0;
    @Unique private long lastTickTime = 0;
    @Inject(method = "doLightUpdates", at = @At("HEAD"))
    private void depthgate_throttleLightUpdates(CallbackInfoReturnable<Integer> cir) {
        DepthGateConfig config = DepthGateConfig.get();
        if (!config.lightOptEnabled) return;
        long now = System.nanoTime();
        if (now - lastTickTime > 45_000_000L) {
            updatesThisTick = 0;
            lastTickTime = now;
        }
        updatesThisTick++;
    }
}

