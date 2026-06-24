package com.depthgate.mixin;
import com.depthgate.DepthGateConfig;
import com.depthgate.engine.ChunkGenBudgetManager;
import net.minecraft.server.world.ServerChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/**
 * Limits chunk generation to a configurable time budget per tick.
 * If the budget is exhausted, remaining chunks are deferred to the next tick.
 * Result: chunks load slowly but without freeze spikes.
 */
@Mixin(ServerChunkManager.class)
public abstract class ChunkGenBudgetMixin {
    @Inject(method = "tick(Ljava/util/function/BooleanSupplier;Z)V", at = @At("HEAD"))
    private void depthgate$beginTickBudget(CallbackInfo ci) {
        DepthGateConfig config = DepthGateConfig.get();
        if (!config.chunkGenBudgetEnabled) return;
        ChunkGenBudgetManager.getInstance().beginTick(config.chunkGenBudgetMs);
    }
}
