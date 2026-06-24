package com.depthgate.client.mixin;
import com.depthgate.engine.AdaptiveDistanceManager;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Inject(method = "stop", at = @At("HEAD"))
    private void depthgate$restoreOnStop(CallbackInfo ci) {
        int originalDistance = AdaptiveDistanceManager.getInstance().getOriginalRenderDistance();
        if (originalDistance != -1) {
            MinecraftClient client = (MinecraftClient) (Object) this;
            if (client.options != null) {
                client.options.getViewDistance().setValue(originalDistance);
            }
        }
        AdaptiveDistanceManager.getInstance().reset();
    }
}
