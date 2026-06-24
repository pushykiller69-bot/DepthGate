package com.depthgate.client.mixin;
import com.depthgate.DepthGateConfig;
import com.depthgate.engine.VerticalCullManager;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.ChunkSectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/**
 * Skips rendering entities whose Y section is marked inactive by VerticalCullManager.
 * Big win on iGPU since entity draw calls are expensive on shared RAM.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void depthgate$skipCulledEntities(
            E entity, double yaw, float tickDelta, net.minecraft.client.util.math.MatrixStack matrices,
            net.minecraft.client.render.VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        DepthGateConfig config = DepthGateConfig.get();
        if (!config.entitySectionCullingEnabled) return;
        int entitySectionY = ChunkSectionPos.getSectionCoord((int) entity.getY());
        if (!VerticalCullManager.getInstance().isSectionActive(entitySectionY)) {
            ci.cancel();
        }
    }
}
