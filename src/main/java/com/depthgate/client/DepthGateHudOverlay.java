package com.depthgate.client;

import com.depthgate.DepthGateConfig;
import com.depthgate.engine.GCDetector;
import com.depthgate.engine.ServerTpsTracker;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Locale;

public class DepthGateHudOverlay {
    private static final long UPDATE_INTERVAL_NANOS = 250_000_000L;
    private static final int[] COMPACT_COLORS = {0x55FFFF, 0xFFFFFF, 0xB0B0B0, 0xB0B0B0, 0xB0B0B0};
    private static long lastUpdateNanos;
    private static Snapshot snapshot;

    public static void register() {
        HudRenderCallback.EVENT.register((context, tickCounter) -> renderSupplement(context));
    }

    public static void renderCompact(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        Snapshot current = snapshot(client);
        TextRenderer font = client.textRenderer;
        for (int i = 0; i < current.compactLines.length; i++) {
            context.drawTextWithShadow(font, current.compactLines[i], 2, 2 + i * 10, COMPACT_COLORS[i]);
        }
    }

    private static void renderSupplement(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        DepthGateConfig config = DepthGateConfig.get();
        if (!config.debugHudEnabled || (config.compactDebugHudEnabled && client.getDebugHud() != null
                && client.getDebugHud().shouldShowDebugHud())) return;
        if (client.getDebugHud() == null || !client.getDebugHud().shouldShowDebugHud()) return;

        Snapshot current = snapshot(client);
        int screenWidth = client.getWindow().getScaledWidth();
        TextRenderer font = client.textRenderer;
        for (int i = 0; i < current.supplementLines.length; i++) {
            context.drawTextWithShadow(font, current.supplementLines[i],
                    screenWidth - current.supplementWidths[i] - 2, 2 + i * 10, 0xFFFFFF);
        }
    }

    private static Snapshot snapshot(MinecraftClient client) {
        long now = System.nanoTime();
        if (snapshot != null && now - lastUpdateNanos < UPDATE_INTERVAL_NANOS) return snapshot;
        lastUpdateNanos = now;

        Runtime runtime = Runtime.getRuntime();
        long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long maxMb = runtime.maxMemory() / (1024 * 1024);
        PlayerEntity player = client.player;
        String coordinates = player == null ? "XYZ: --" : String.format(Locale.ROOT,
                "XYZ: %.1f, %.1f, %.1f", player.getX(), player.getY(), player.getZ());
        String ping = "--";
        if (player != null && client.getNetworkHandler() != null) {
            PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(player.getUuid());
            if (entry != null) ping = entry.getLatency() + "ms";
        }
        double tps = ServerTpsTracker.getTps(now);
        String tpsText = Double.isNaN(tps) ? "--" : String.format(Locale.ROOT, "%.1f", tps);
        String cpu = System.getenv("PROCESSOR_IDENTIFIER");
        if (cpu == null || cpu.isBlank()) cpu = runtime.availableProcessors() + " threads";
        String gpu = DepthGateClient.gpu();

        String[] compactLines = {
                "DepthGate | FPS " + client.getCurrentFps() + " | TPS~ " + tpsText + " | Ping " + ping,
                coordinates,
                "CPU: " + shorten(cpu, 42),
                "GPU: " + shorten(gpu, 42),
                "RAM: " + usedMb + " / " + maxMb + " MB"
        };

        GCDetector gc = GCDetector.getInstance();
        int offset = DepthGateClient.distance().getOffset();
        String[] supplementLines = {
                "\u00A76[DepthGate]",
                "Tier: " + DepthGateClient.tier(),
                "GPU: " + gpu,
                "GC: " + gc.getGCTypeName() + (gc.isLowPauseCollector() ? " OK" : " warn")
                        + " (" + gc.getTotalCollections() + " runs, avg " + gc.getLastPauseDurationMs() + "ms)",
                "View distance: " + client.options.getViewDistance().getValue()
                        + (offset == 0 ? "" : " (" + (offset > 0 ? "+" : "") + offset + " vs yours)"),
                "RAM: " + usedMb + "MB / " + maxMb + "MB"
        };
        int[] widths = new int[supplementLines.length];
        for (int i = 0; i < supplementLines.length; i++) {
            widths[i] = client.textRenderer.getWidth(supplementLines[i]);
        }
        snapshot = new Snapshot(compactLines, supplementLines, widths);
        return snapshot;
    }

    private static String shorten(String text, int maxLength) {
        String normalized = text == null ? "Unknown" : text.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength - 3) + "...";
    }

    private record Snapshot(String[] compactLines, String[] supplementLines, int[] supplementWidths) {
    }
}
