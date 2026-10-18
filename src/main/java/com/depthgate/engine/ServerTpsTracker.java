package com.depthgate.engine;

public final class ServerTpsTracker {
    private static final long[] INTERVALS_NANOS = new long[5];
    private static final long MAX_SAMPLE_AGE_NANOS = 15_000_000_000L;
    private static int sampleCount;
    private static int nextIndex;
    private static long lastUpdateNanos;

    private ServerTpsTracker() {
    }

    public static void recordWorldTimeUpdate(long nowNanos) {
        if (lastUpdateNanos != 0L) {
            long interval = nowNanos - lastUpdateNanos;
            if (interval >= 250_000_000L && interval <= 60_000_000_000L) {
                INTERVALS_NANOS[nextIndex] = interval;
                nextIndex = (nextIndex + 1) % INTERVALS_NANOS.length;
                sampleCount = Math.min(sampleCount + 1, INTERVALS_NANOS.length);
            }
        }
        lastUpdateNanos = nowNanos;
    }

    public static double getTps(long nowNanos) {
        if (sampleCount < 2 || nowNanos - lastUpdateNanos > MAX_SAMPLE_AGE_NANOS) return Double.NaN;
        long totalNanos = 0L;
        for (int i = 0; i < sampleCount; i++) totalNanos += INTERVALS_NANOS[i];
        return Math.min(20.0, 20_000_000_000.0 * sampleCount / totalNanos);
    }

    public static void reset() {
        sampleCount = 0;
        nextIndex = 0;
        lastUpdateNanos = 0L;
    }
}
