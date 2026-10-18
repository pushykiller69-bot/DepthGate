package com.depthgate.engine;

public class AdaptiveDistanceManager {
    public record Action(int newDistance, boolean changed) {}

    private static final int WINDOW = 8;
    private static final int MIN_SAMPLES = 5;
    private static final long SAMPLE_MS = 1000;
    private static final long REDUCE_COOLDOWN_MS = 8_000;
    private static final long RESTORE_BASE_MS = 45_000;
    private static final long RESTORE_MAX_MS = 10 * 60_000;

    private final int[] samples = new int[WINDOW];
    private int count = 0;
    private int idx = 0;
    private long lastSampleMs = Long.MIN_VALUE;
    private long lastChangeMs = Long.MIN_VALUE;
    private long lastRestoreMs = Long.MIN_VALUE;
    private long restoreBackoffMs = RESTORE_BASE_MS;

    private int userDistance = -1;
    private int applied = -1;

    public Action tick(long nowMs, int fps, int currentDistance, int fpsTarget,
                       int minDistance, boolean allowBoost, int maxBoost) {
        if (userDistance == -1) {
            userDistance = currentDistance;
        }
        if (applied != -1 && currentDistance != applied) {
            userDistance = currentDistance;
            applied = currentDistance;
            clearSamples();
            lastChangeMs = nowMs;
            return new Action(currentDistance, false);
        }
        if (lastSampleMs == Long.MIN_VALUE || nowMs - lastSampleMs >= SAMPLE_MS) {
            lastSampleMs = nowMs;
            if (fps > 0) {
                samples[idx] = fps;
                idx = (idx + 1) % WINDOW;
                if (count < WINDOW) count++;
            }
        }
        if (count < MIN_SAMPLES) return new Action(currentDistance, false);

        int sum = 0, min = Integer.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            sum += samples[i];
            min = Math.min(min, samples[i]);
        }
        double avg = (double) sum / count;
        long sinceChange = lastChangeMs == Long.MIN_VALUE ? Long.MAX_VALUE : nowMs - lastChangeMs;
        int floor = Math.max(2, minDistance);

        if (avg < fpsTarget * 0.9 && currentDistance > floor && sinceChange >= REDUCE_COOLDOWN_MS) {
            if (lastRestoreMs != Long.MIN_VALUE && nowMs - lastRestoreMs < 20_000) {
                restoreBackoffMs = Math.min(RESTORE_MAX_MS, restoreBackoffMs * 2);
            }
            return commit(nowMs, currentDistance - 1, currentDistance);
        }

        boolean healthy = avg >= fpsTarget * 1.25 && min >= fpsTarget;
        boolean steady = avg >= fpsTarget && sinceChange >= restoreBackoffMs;
        if ((healthy && sinceChange >= REDUCE_COOLDOWN_MS) || steady) {
            int ceiling = allowBoost ? userDistance + Math.max(0, maxBoost) : userDistance;
            if (currentDistance < ceiling) {
                lastRestoreMs = nowMs;
                return commit(nowMs, currentDistance + 1, currentDistance);
            }
        }
        return new Action(currentDistance, false);
    }

    private Action commit(long nowMs, int newDistance, int currentDistance) {
        applied = newDistance;
        lastChangeMs = nowMs;
        clearSamples();
        return new Action(newDistance, newDistance != currentDistance);
    }

    private void clearSamples() {
        count = 0;
        idx = 0;
        lastSampleMs = Long.MIN_VALUE;
    }

    public int getUserDistance() { return userDistance; }

    public int getOffset() { return (applied == -1 || userDistance == -1) ? 0 : applied - userDistance; }

    public void reset() {
        userDistance = -1;
        applied = -1;
        lastChangeMs = Long.MIN_VALUE;
        lastRestoreMs = Long.MIN_VALUE;
        restoreBackoffMs = RESTORE_BASE_MS;
        clearSamples();
    }
}
