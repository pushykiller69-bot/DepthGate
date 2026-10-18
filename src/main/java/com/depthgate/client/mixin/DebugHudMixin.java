package com.depthgate.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.ArrayList;
import java.util.List;

@Mixin(DebugHud.class)
public abstract class DebugHudMixin {
    @Unique
    private static final long DEPTHGATE_UPDATE_INTERVAL_NANOS = 50_000_000L;

    @Shadow
    @Final
    private MinecraftClient client;

    @Unique private ClientWorld depthgate$world;
    @Unique private List<String> depthgate$leftText = List.of();
    @Unique private List<String> depthgate$rightText = List.of();
    @Unique private long depthgate$leftUpdatedAt;
    @Unique private long depthgate$rightUpdatedAt;
    @Unique private Entity depthgate$raycastEntity;
    @Unique private HitResult depthgate$blockHit;
    @Unique private HitResult depthgate$fluidHit;
    @Unique private long depthgate$raycastUpdatedAt;

    @Invoker("getLeftText")
    protected abstract List<String> depthgate$invokeGetLeftText();

    @Invoker("getRightText")
    protected abstract List<String> depthgate$invokeGetRightText();

    @Redirect(
            method = "drawLeftText(Lnet/minecraft/client/gui/DrawContext;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/DebugHud;getLeftText()Ljava/util/List;")
    )
    private List<String> depthgate$reuseLeftText(DebugHud hud) {
        depthgate$resetForWorldChange();
        long now = System.nanoTime();
        if (depthgate$leftText.isEmpty() || now - depthgate$leftUpdatedAt >= DEPTHGATE_UPDATE_INTERVAL_NANOS) {
            depthgate$leftText = List.copyOf(depthgate$invokeGetLeftText());
            depthgate$leftUpdatedAt = now;
        }
        return new ArrayList<>(depthgate$leftText);
    }

    @Redirect(
            method = "drawRightText(Lnet/minecraft/client/gui/DrawContext;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/DebugHud;getRightText()Ljava/util/List;")
    )
    private List<String> depthgate$reuseRightText(DebugHud hud) {
        depthgate$resetForWorldChange();
        long now = System.nanoTime();
        if (depthgate$rightText.isEmpty() || now - depthgate$rightUpdatedAt >= DEPTHGATE_UPDATE_INTERVAL_NANOS) {
            depthgate$rightText = List.copyOf(depthgate$invokeGetRightText());
            depthgate$rightUpdatedAt = now;
        }
        return new ArrayList<>(depthgate$rightText);
    }

    @Redirect(
            method = "render(Lnet/minecraft/client/gui/DrawContext;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;raycast(DFZ)Lnet/minecraft/util/hit/HitResult;"),
            require = 2
    )
    private HitResult depthgate$reuseRaycast(Entity entity, double maxDistance, float tickDelta, boolean includeFluids) {
        long now = System.nanoTime();
        if (entity != depthgate$raycastEntity || depthgate$blockHit == null
                || now - depthgate$raycastUpdatedAt >= DEPTHGATE_UPDATE_INTERVAL_NANOS) {
            depthgate$raycastEntity = entity;
            depthgate$blockHit = entity.raycast(maxDistance, tickDelta, false);
            depthgate$fluidHit = entity.raycast(maxDistance, tickDelta, true);
            depthgate$raycastUpdatedAt = now;
        }
        return includeFluids ? depthgate$fluidHit : depthgate$blockHit;
    }

    @Unique
    private void depthgate$resetForWorldChange() {
        if (depthgate$world != client.world) {
            depthgate$world = client.world;
            depthgate$leftText = List.of();
            depthgate$rightText = List.of();
            depthgate$leftUpdatedAt = 0L;
            depthgate$rightUpdatedAt = 0L;
            depthgate$raycastEntity = null;
            depthgate$blockHit = null;
            depthgate$fluidHit = null;
            depthgate$raycastUpdatedAt = 0L;
        }
    }
}
