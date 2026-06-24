package com.depthgate.client;
import com.depthgate.DepthGateConfig;
import com.depthgate.engine.*;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
/**
 * Renders DepthGate debug info in the F3 overlay — GC type, last pause duration,
 * estimated iGPU-stolen RAM, active section count, and adaptive distance boost.
 */
public class DepthGateHudOverlay {
    public static void register() {
        HudRenderCallback.EVENT.register((drawContext, renderTickCounter) -> {
            render(drawContext, renderTickCounter);
        });
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        DepthGateConfig config = DepthGateConfig.get();
        if (!config.debugHudEnabled) return;
        if (client.getDebugHud() == null || !client.getDebugHud().shouldShowDebugHud()) return;

        TextRenderer textRenderer = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();

        GCDetector gc = GCDetector.getInstance();
        VerticalCullManager cull = VerticalCullManager.getInstance();
        AdaptiveDistanceManager dist = AdaptiveDistanceManager.getInstance();
        GPUDetector gpu = GPUDetector.getInstance();

        String[] lines = {
            "§6[DepthGate]",
            "GC: " + gc.getGCTypeName() + (gc.isLowPauseCollector() ? " §a✓" : " §c⚠"),
            "GC Avg Pause: " + gc.getLastPauseDurationMs() + "ms",
            "GC Collections: " + gc.getTotalCollections(),
            "GPU: " + gpu.getRendererName(),
            "iGPU: " + (gpu.isIntegratedGPU() ? "§cYes" : "§aNo"),
            "Sections: " + cull.getActiveCount() + "/" + cull.getTotalSections() +
                " (skipped " + cull.getSkippedCount() + ")",
            "Distance Boost: +" + dist.getCurrentBoost(),
            "RAM Free: " + (Runtime.getRuntime().freeMemory() / (1024 * 1024)) + "MB / " +
                (Runtime.getRuntime().maxMemory() / (1024 * 1024)) + "MB",
        };

        int y = 2;
        for (String line : lines) {
            int width = textRenderer.getWidth(line.replaceAll("§.", ""));
            int x = screenWidth - width - 2;
            context.drawTextWithShadow(textRenderer, line, x, y, 0xFFFFFF);
            y += 10;
        }
    }
}
