package com.depthgate.client;
import com.depthgate.DepthGateConfig;
import com.depthgate.DepthGateMod;
import com.depthgate.engine.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.ChunkSectionPos;
import org.lwjgl.opengl.GL11;
/**
 * Client entrypoint — bridges Minecraft API calls to the pure-Java engine layer.
 * All MC-specific reads happen here; engine classes receive only primitives.
 */
public class DepthGateClient implements ClientModInitializer {
    private boolean gcWarningShown = false;
    private boolean gpuDetected = false;
    private int tickCounter = 0;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;

            DepthGateConfig config = DepthGateConfig.get();

            // One-time GPU detection (needs GL context, so must run on render thread)
            if (!gpuDetected) {
                detectGPU(config);
                gpuDetected = true;
            }

            // One-time GC warning to the player
            if (!gcWarningShown) {
                String warning = GCDetector.getInstance().getWarningMessage();
                if (warning != null) {
                    client.player.sendMessage(
                        net.minecraft.text.Text.literal(warning), false
                    );
                }
                gcWarningShown = true;
            }

            // Vertical culling — read MC state, pass primitives to engine
            if (config.isCurrentlyCullingVerticalSections) {
                int playerY = (int) client.player.getY();
                int playerSectionY = ChunkSectionPos.getSectionCoord(playerY);
                int seaLevelSection = ChunkSectionPos.getSectionCoord(client.world.getSeaLevel());
                VerticalCullManager.getInstance().updateIfMoved(
                    playerSectionY,
                    config.vCullDepth,
                    config.bufferSections,
                    config.cullSurfaceKeep,
                    seaLevelSection
                );
            } else {
                VerticalCullManager.getInstance().markAllActive();
            }

            // Adaptive distance — read MC state, pass primitives, apply result
            if (config.adaptiveDistanceEnabled) {
                int currentFps = client.getCurrentFps();
                int currentDistance = client.options.getViewDistance().getValue();
                float renderRatio = VerticalCullManager.getInstance().getRenderRatio();
                AdaptiveDistanceManager.DistanceAction action = AdaptiveDistanceManager.getInstance().tick(
                    currentFps, currentDistance, renderRatio,
                    config.fpsTarget, config.extendedDistanceMax
                );
                if (action.changed()) {
                    client.options.getViewDistance().setValue(action.newDistance());
                }
            }

            // Chunk delivery limiter — reset once per tick
            if (config.chunkDeliveryLimitEnabled) {
                ChunkDeliveryLimiter.getInstance().resetTick();
            }

            // Update GC stats periodically (once per second ≈ every 20 ticks)
            tickCounter++;
            if (tickCounter % 20 == 0) {
                GCDetector.getInstance().updateStats();
            }
        });

        // Register the debug HUD overlay
        DepthGateHudOverlay.register();
    }

    private void detectGPU(DepthGateConfig config) {
        try {
            String renderer = GL11.glGetString(GL11.GL_RENDERER);
            GPUDetector.getInstance().detect(renderer);
            if (GPUDetector.getInstance().isIntegratedGPU()) {
                DepthGateMod.LOGGER.info("[DepthGate] Detected iGPU: {} — applying potato preset", renderer);
                if (config.igpuAutoPreset) {
                    GPUDetector.IGPUPreset preset = GPUDetector.getInstance().getPotatoPreset();
                    config.vCullDepth = preset.vCullDepth();
                    config.particleCap = preset.particleCap();
                    config.lightUpdateBatchSize = preset.lightUpdateBatchSize();
                    config.hopperSkipTicks = preset.hopperSkipTicks();
                    ChunkDeliveryLimiter.getInstance().setCap(preset.chunkDeliveryCap());
                }
            } else {
                DepthGateMod.LOGGER.info("[DepthGate] Detected GPU: {}", renderer);
                ChunkDeliveryLimiter.getInstance().setCap(config.chunkDeliveryCapDefault);
            }
        } catch (Exception e) {
            DepthGateMod.LOGGER.warn("[DepthGate] GPU detection failed, using defaults", e);
            ChunkDeliveryLimiter.getInstance().setCap(config.chunkDeliveryCapDefault);
        }
    }
}
