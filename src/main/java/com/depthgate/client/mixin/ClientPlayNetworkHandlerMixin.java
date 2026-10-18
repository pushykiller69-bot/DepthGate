package com.depthgate.client.mixin;

import com.depthgate.engine.ServerTpsTracker;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Inject(
            method = "onWorldTimeUpdate(Lnet/minecraft/network/packet/s2c/play/WorldTimeUpdateS2CPacket;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;setTime(J)V", shift = At.Shift.AFTER)
    )
    private void depthgate$recordWorldTimeUpdate(WorldTimeUpdateS2CPacket packet, CallbackInfo ci) {
        ServerTpsTracker.recordWorldTimeUpdate(System.nanoTime());
    }
}
