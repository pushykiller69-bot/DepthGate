package com.depthgate.client.mixin;
import com.depthgate.engine.VerticalCullManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Inject(method = "setupTerrain", at = @At("HEAD"))
    private void depthgate_preSetupTerrain(CallbackInfo ci) {
        VerticalCullManager.getInstance().update();
    }
}

