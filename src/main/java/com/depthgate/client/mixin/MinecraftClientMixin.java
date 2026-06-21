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
    private void depthgate_restoreOnStop(CallbackInfo ci) {
        AdaptiveDistanceManager.getInstance().restore();
    }
}

