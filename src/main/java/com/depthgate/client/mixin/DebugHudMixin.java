package com.depthgate.client.mixin;

import com.depthgate.DepthGateConfig;
import com.depthgate.client.DepthGateHudOverlay;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.DebugHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DebugHud.class)
public abstract class DebugHudMixin {
    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;)V", at = @At("HEAD"), cancellable = true)
    private void depthgate$renderCompactDebugHud(DrawContext context, CallbackInfo ci) {
        DebugHud debugHud = (DebugHud) (Object) this;
        DepthGateConfig config = DepthGateConfig.get();
        if (!config.compactDebugHudEnabled || !config.debugHudEnabled || !debugHud.shouldShowDebugHud()) return;
        DepthGateHudOverlay.renderCompact(context);
        ci.cancel();
    }
}
