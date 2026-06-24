package com.depthgate.engine;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Locale;
/**
 * Detects the active garbage collector and warns the user if they're not running
 * a low-pause collector (Shenandoah or ZGC). Switching to Shenandoah reduces
 * GC pause from 50–200ms down to under 5ms — more impact than any code change.
 *
 * <p>Pure Java — uses only {@code java.lang.management}.</p>
 */
public class GCDetector {
    private static final GCDetector INSTANCE = new GCDetector();

    private String gcTypeName = "Unknown";
    private boolean lowPauseCollector = false;
    private long lastPauseDurationMs = 0;
    private long totalCollections = 0;

    public static GCDetector getInstance() {
        return INSTANCE;
    }

    /**
     * Reads the active GC beans and determines the collector type.
     * Call once during mod initialization.
     */
    public void detect() {
        List<GarbageCollectorMXBean> beans = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean bean : beans) {
            String name = bean.getName().toLowerCase(Locale.ROOT);
            totalCollections += bean.getCollectionCount();

            if (name.contains("shenandoah")) {
                gcTypeName = "Shenandoah";
                lowPauseCollector = true;
                break;
            } else if (name.contains("zgc") || name.contains("z garbage")) {
                gcTypeName = "ZGC";
                lowPauseCollector = true;
                break;
            } else if (name.contains("g1")) {
                gcTypeName = "G1GC";
            } else if (name.contains("parallel")) {
                gcTypeName = "Parallel GC";
            } else if (name.contains("serial") || name.contains("copy") || name.contains("marksweep")) {
                gcTypeName = "Serial/CMS GC";
            }
        }
    }

    /**
     * Updates the last pause duration from GC beans.
     * Call periodically (e.g. once per second) to keep stats fresh.
     */
    public void updateStats() {
        List<GarbageCollectorMXBean> beans = ManagementFactory.getGarbageCollectorMXBeans();
        long totalTime = 0;
        long totalCount = 0;
        for (GarbageCollectorMXBean bean : beans) {
            totalTime += bean.getCollectionTime();
            totalCount += bean.getCollectionCount();
        }
        if (totalCount > 0) {
            lastPauseDurationMs = totalTime / totalCount;
        }
        totalCollections = totalCount;
    }

    public boolean isLowPauseCollector() {
        return lowPauseCollector;
    }

    public String getGCTypeName() {
        return gcTypeName;
    }

    public long getLastPauseDurationMs() {
        return lastPauseDurationMs;
    }

    public long getTotalCollections() {
        return totalCollections;
    }

    /**
     * Returns a user-friendly warning message, or null if no warning needed.
     */
    public String getWarningMessage() {
        if (lowPauseCollector) return null;
        return String.format(
            "§e[DepthGate]§r You are using §c%s§r. Switch to §aShenandoah§r for up to §a40x§r lower GC pauses. " +
            "Add §b-XX:+UseShenandoahGC§r to your JVM arguments.",
            gcTypeName
        );
    }
}
