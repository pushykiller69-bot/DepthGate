package com.depthgate.client.mixin;
import com.depthgate.engine.VerticalCullManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.ChunkSectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Inject(method = "setupTerrain", at = @At("HEAD"))
    private void depthgate$onSetupTerrain(CallbackInfo ci) {
        // Culling update moved to DepthGateClient tick event — single call per tick
    }

    /**
     * Cancel mesh rebuilds for sections that are vertically culled.
     * This is the biggest iGPU bandwidth win — prevents uploading mesh data
     * to shared RAM for sections the player can't see anyway.
     */
    @Inject(method = "scheduleBlockRender", at = @At("HEAD"), cancellable = true)
    private void dg$skipRebuild(int x, int y, int z, CallbackInfo ci) {
        if (!VerticalCullManager.getInstance().isSectionActive(y)) {
            ci.cancel();
        }
    }
}
