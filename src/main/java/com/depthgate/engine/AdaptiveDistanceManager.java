package com.depthgate.engine;
import com.depthgate.DepthGateConfig;
import net.minecraft.client.MinecraftClient;
public class AdaptiveDistanceManager {
    private static final AdaptiveDistanceManager INSTANCE = new AdaptiveDistanceManager();
    private int originalRenderDistance = -1;
    private int currentBoost = 0;
    private int stableFrames = 0;
    private int lowFrames = 0;
    private final long[] frameTimes = new long[60];
    private int frameIndex = 0;
    public static AdaptiveDistanceManager getInstance() {
        return INSTANCE;
    }
    public void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options == null) return;
        DepthGateConfig config = DepthGateConfig.get();
        if (!config.adaptiveDistanceEnabled) return;
        int currentDistance = client.options.getViewDistance().getValue();
        if (originalRenderDistance == -1) {
            originalRenderDistance = currentDistance;
        }
        int fps = client.getCurrentFps();
        frameTimes[frameIndex] = fps;
        frameIndex = (frameIndex + 1) % frameTimes.length;
        int avgFps = getAvgFps();
        int fpsTarget = config.fpsTarget;
        int maxBoost = config.extendedDistanceMax;
        float renderRatio = VerticalCullManager.getInstance().getRenderRatio();
        int scaledMaxBoost = (int) ((1.0f - renderRatio) * maxBoost);
        scaledMaxBoost = Math.max(0, Math.min(scaledMaxBoost, maxBoost));
        if (avgFps > fpsTarget + 20) {
            stableFrames++;
            lowFrames = 0;
            if (stableFrames > 60 && currentBoost < scaledMaxBoost) {
                currentBoost++;
                stableFrames = 0;
                client.options.getViewDistance().setValue(originalRenderDistance + currentBoost);
            }
        } else if (avgFps < fpsTarget) {
            lowFrames++;
            stableFrames = 0;
            if (lowFrames > 40 && currentBoost > 0) {
                currentBoost--;
                lowFrames = 0;
                client.options.getViewDistance().setValue(originalRenderDistance + currentBoost);
            }
            if (lowFrames > 80 && currentBoost == 0 && currentDistance > 2) {
                client.options.getViewDistance().setValue(currentDistance - 1);
                lowFrames = 0;
            }
        } else {
            stableFrames = 0;
            lowFrames = Math.max(0, lowFrames - 1);
        }
    }
    private int getAvgFps() {
        long sum = 0;
        int count = 0;
        for (long ft : frameTimes) {
            if (ft > 0) { sum += ft; count++; }
        }
        return count > 0 ? (int) (sum / count) : 60;
    }
    public int getCurrentBoost() { return currentBoost; }
    public void restore() {
        if (originalRenderDistance != -1) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.options != null) {
                client.options.getViewDistance().setValue(originalRenderDistance);
            }
        }
        currentBoost = 0;
    }
}

