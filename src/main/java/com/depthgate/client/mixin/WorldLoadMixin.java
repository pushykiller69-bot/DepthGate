package com.depthgate.client.mixin;
import com.depthgate.DepthGateMod;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/**
 * Triggers a GC cycle during the world loading screen — the one moment
 * a GC pause is invisible to the player. Clears young gen before terrain
 * generation begins.
 */
@Mixin(LevelLoadingScreen.class)
public abstract class WorldLoadMixin {
    @Unique private boolean gcTriggered = false;

    @Inject(method = "close", at = @At("HEAD"))
    private void depthgate$preWarmGC(CallbackInfo ci) {
        if (!gcTriggered) {
            long before = Runtime.getRuntime().freeMemory();
            System.gc();
            long freed = Runtime.getRuntime().freeMemory() - before;
            DepthGateMod.LOGGER.info("[DepthGate] Pre-warm GC during load screen freed {}MB",
                Math.max(0, freed / (1024 * 1024)));
            gcTriggered = true;
        }
    }
}
