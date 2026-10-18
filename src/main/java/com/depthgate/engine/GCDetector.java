package com.depthgate.engine;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Locale;
public class GCDetector {
    private static final GCDetector INSTANCE = new GCDetector();

    private String gcTypeName = "Unknown";
    private boolean lowPauseCollector = false;
    private long lastPauseDurationMs = 0;
    private long totalCollections = 0;

    public static GCDetector getInstance() {
        return INSTANCE;
    }

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

    public String getWarningMessage() {
        if (lowPauseCollector) return null;
        return String.format(
            "\u00A7e[DepthGate]\u00A7r You are using \u00A7c%s\u00A7r. Shenandoah or ZGC greatly reduces GC stutter. " +
            "Add \u00A7b-XX:+UseShenandoahGC\u00A7r to your JVM arguments (if your Java build supports it).",
            gcTypeName
        );
    }
}
