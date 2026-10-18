package com.depthgate.client;

import com.depthgate.DepthGateConfig;
import com.depthgate.engine.GCDetector;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public class DepthGateHudOverlay {
    private static final String CODES = "\u00A7.";

    public static void register() {
        HudRenderCallback.EVENT.register((context, tickCounter) -> render(context));
    }

    private static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        DepthGateConfig config = DepthGateConfig.get();
        if (!config.debugHudEnabled) return;
        if (client.getDebugHud() == null || !client.getDebugHud().shouldShowDebugHud()) return;

        TextRenderer font = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();
        GCDetector gc = GCDetector.getInstance();
        int offset = DepthGateClient.distance().getOffset();
        Runtime rt = Runtime.getRuntime();
        long usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);

        String[] lines = {
            "\u00A76[DepthGate]",
            "Tier: " + DepthGateClient.tier(),
            "GPU: " + DepthGateClient.gpu(),
            "GC: " + gc.getGCTypeName() + (gc.isLowPauseCollector() ? " \u00A7aOK" : " \u00A7cwarn")
                    + " (" + gc.getTotalCollections() + " runs, avg " + gc.getLastPauseDurationMs() + "ms)",
            "View distance: " + client.options.getViewDistance().getValue()
                    + (offset == 0 ? "" : " (" + (offset > 0 ? "+" : "") + offset + " vs yours)"),
            "RAM: " + usedMb + "MB / " + (rt.maxMemory() / (1024 * 1024)) + "MB",
        };

        int y = 2;
        for (String line : lines) {
            int width = font.getWidth(line.replaceAll(CODES, ""));
            context.drawTextWithShadow(font, line, screenWidth - width - 2, y, 0xFFFFFF);
            y += 10;
        }
    }
}
