package com.depthgate.client.mixin;
import com.depthgate.DepthGateConfig;
import com.depthgate.engine.ChunkDeliveryLimiter;
import net.minecraft.client.world.ClientChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/**
 * Rate-limits chunk delivery to prevent bandwidth spikes when many chunks
 * finish generating at once. Caps at 2/tick for iGPU, 6/tick for discrete.
 */
@Mixin(ClientChunkManager.class)
public abstract class ChunkDeliveryMixin {
    @Inject(method = "loadChunkFromPacket", at = @At("HEAD"), cancellable = true)
    private void depthgate$limitChunkDelivery(int x, int z, CallbackInfo ci) {
        DepthGateConfig config = DepthGateConfig.get();
        if (!config.chunkDeliveryLimitEnabled) return;
        ChunkDeliveryLimiter limiter = ChunkDeliveryLimiter.getInstance();
        if (!limiter.canDeliverChunk()) {
            ci.cancel();
            return;
        }
        limiter.onChunkDelivered();
    }
}
