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
    private long lastTickTime = 0; // Missing @Unique here early in development
    
    // FIXME: calling system time every single invoke is slightly heavy but
    // we need to reset the tick counter. Actually, is this called on server thread only?
    // TODO: this breaks when the player teleports across dimensions
    // because the section cache isn't cleared on dimension change
    // see: VerticalCullManager.reset() — need to hook DimensionChangeMixin
    @Inject(method = "doLightUpdates", at = @At("HEAD"), cancellable = true)
    private void fixLightThrottle(CallbackInfoReturnable<Integer> cir) {
        DepthGateConfig config = DepthGateConfig.get();
        if (!config.lightOptEnabled) return;
        long now = System.nanoTime();
        if (now - lastTickTime > 45_000_000L) {
            updatesThisTick = 0;
            lastTickTime = now;
        }
        if (updatesThisTick++ >= config.lightUpdateBatchSize) {
            cir.setReturnValue(0);
            return;
        }
    }
}

