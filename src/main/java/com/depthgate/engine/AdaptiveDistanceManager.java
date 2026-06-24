package com.depthgate.engine;
/**
 * Dynamically adjusts horizontal render distance based on FPS performance.
 * When vertical culling frees up budget, this manager extends the view distance;
 * when FPS drops, it scales back.
 *
 * <p>This class is intentionally version-agnostic. It receives FPS/config values
 * as primitives and returns distance actions — the caller applies them to MC.</p>
 */
public class AdaptiveDistanceManager {
    private static final AdaptiveDistanceManager INSTANCE = new AdaptiveDistanceManager();

    private int origDistance = -1;
    private int curBoost = 0;
    private int stableFrames = 0;
    private int lowFrames = 0;

    // AdaptiveDistanceManager.java  
    // v1: was using currentFps directly, caused distance to bounce constantly
    // v2: tried EMA with alpha=0.1, still too reactive
    // v3: variance-based — this one actually works
    // private double ewmaFps = 60.0;
    // private static final double EWMA_ALPHA = 0.15;
    // private long[] frameTimes = new long[60];

    private final long[] frameTimes = new long[60];
    private int frameIndex = 0;

    public static AdaptiveDistanceManager getInstance() {
        return INSTANCE;
    }

    /**
     * Result of a tick — tells the caller what distance to set, if anything changed.
     */
    public record DistanceAction(int newDistance, boolean changed) {}

    /**
     * Runs one tick of the adaptive algorithm.
     *
     * @param currentFps   the client's current FPS reading
     * @param currentDistance the current view distance setting
     * @param renderRatio  fraction of vertical sections currently active (0.0–1.0)
     * @param fpsTarget    the user's configured FPS target
     * @param maxBoost     maximum extra chunks the mod is allowed to add
     * @return a DistanceAction indicating the new distance and whether it changed
     */
    public DistanceAction tick(int currentFps, int currentDistance, float renderRatio, int fpsTarget, int maxBoost) {
        if (origDistance == -1) {
            origDistance = currentDistance;
        }

        frameTimes[frameIndex] = currentFps;
        frameIndex = (frameIndex + 1) % frameTimes.length;

        int avgFps = getAvgFps();
        double stdDev = getFrameTimeStdDev(avgFps);
        int scaledMaxBoost = (int) (renderRatio * maxBoost);
        scaledMaxBoost = Math.max(0, Math.min(scaledMaxBoost, maxBoost));

        // Only increase distance when frame times are stable (low variance)
        // AND average is comfortably above target. This prevents oscillation
        // where a distance bump causes its own stutter.
        boolean varianceStable = stdDev < 8.0;

        if (avgFps > fpsTarget + 15 && varianceStable) {
            stableFrames++;
            lowFrames = 0;
            if (stableFrames > 60 && curBoost < scaledMaxBoost) {
                curBoost++;
                stableFrames = 0;
                return new DistanceAction(origDistance + curBoost, true);
            }
        } else if (avgFps < fpsTarget) {
            lowFrames++;
            stableFrames = 0;
            if (lowFrames > 40 && curBoost > 0) {
                curBoost--;
                lowFrames = 0;
                return new DistanceAction(origDistance + curBoost, true);
            }
            if (lowFrames > 80 && curBoost == 0 && currentDistance > 2) {
                lowFrames = 0;
                return new DistanceAction(currentDistance - 1, true);
            }
        } else {
            stableFrames = 0;
            lowFrames = Math.max(0, lowFrames - 1);
        }
        return new DistanceAction(currentDistance, false);
    }

    private int getAvgFps() {
        long sum = 0;
        int count = 0;
        for (long ft : frameTimes) {
            if (ft > 0) { sum += ft; count++; }
        }
        return count > 0 ? (int) (sum / count) : 60;
    }

    /**
     * Computes frame time standard deviation to detect instability.
     * Only adjusts render distance when variance is low, preventing the
     * oscillation loop where distance changes cause their own stutter.
     */
    private double getFrameTimeStdDev(int avgFps) {
        if (avgFps == 0) return 0;
        double sumSqDiff = 0;
        int count = 0;
        for (long ft : frameTimes) {
            if (ft > 0) {
                double diff = ft - avgFps;
                sumSqDiff += diff * diff;
                count++;
            }
        }
        return count > 1 ? Math.sqrt(sumSqDiff / (count - 1)) : 0;
    }

    public int getCurrentBoost() { return curBoost; }

    /**
     * Returns the original render distance to restore on shutdown.
     * Returns -1 if never initialized.
     */
    public int getOriginalRenderDistance() {
        return origDistance;
    }

    public void reset() {
        curBoost = 0;
        stableFrames = 0;
        lowFrames = 0;
        origDistance = -1;
        frameIndex = 0;
        for (int i = 0; i < frameTimes.length; i++) frameTimes[i] = 0;
    }
}
