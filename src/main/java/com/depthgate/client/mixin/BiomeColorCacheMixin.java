package com.depthgate.client.mixin;
import com.depthgate.DepthGateConfig;
import net.minecraft.world.biome.source.BiomeAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(BiomeAccess.class)
public abstract class BiomeColorCacheMixin {
    @Inject(method = "getBiome", at = @At("HEAD"))
    private void depthgate_biomeOptHint(CallbackInfoReturnable<?> cir) {
    }
}

