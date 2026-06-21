package com.depthgate.engine;
import com.depthgate.DepthGateConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.ChunkSectionPos;
import java.util.BitSet;
public class VerticalCullManager {
    private static final VerticalCullManager INSTANCE = new VerticalCullManager();
    private static final int MIN_SECTION = -4;
    private static final int MAX_SECTION = 19;
    private static final int TOTAL_SECTIONS = MAX_SECTION - MIN_SECTION + 1;
    private final BitSet activeSections = new BitSet(TOTAL_SECTIONS);
    private int lastPlayerSectionY = Integer.MIN_VALUE;
    private int activeCount = 0;
    public static VerticalCullManager getInstance() {
        return INSTANCE;
    }
    public void update() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        DepthGateConfig config = DepthGateConfig.get();
        if (!config.verticalCullingEnabled) {
            if (activeCount != TOTAL_SECTIONS) markAllActive();
            return;
        }
        int playerY = (int) client.player.getY();
        int playerSectionY = ChunkSectionPos.getSectionCoord(playerY);
        if (playerSectionY == lastPlayerSectionY) return;
        lastPlayerSectionY = playerSectionY;
        recalculate(playerSectionY, config.verticalRange, config.bufferSections, config.alwaysKeepSurface);
    }
    private void recalculate(int playerSection, int range, int buffer, boolean keepSurface) {
        activeSections.clear();
        activeCount = 0;
        int lowerBound = Math.max(MIN_SECTION, playerSection - range - buffer);
        int upperBound = Math.min(MAX_SECTION, playerSection + range);
        for (int i = MIN_SECTION; i <= MAX_SECTION; i++) {
            boolean inPlayerZone = (i >= lowerBound && i <= upperBound);
            boolean inSurfaceBand = keepSurface && (i >= 3 && i <= 6);
            boolean isPlayerSection = (i == playerSection);
            if (inPlayerZone || inSurfaceBand || isPlayerSection) {
                activeSections.set(i - MIN_SECTION);
                activeCount++;
            }
        }
    }
    private void markAllActive() {
        activeSections.set(0, TOTAL_SECTIONS);
        activeCount = TOTAL_SECTIONS;
    }
    public boolean isSectionActive(int sectionY) {
        if (activeCount == TOTAL_SECTIONS) return true;
        int idx = sectionY - MIN_SECTION;
        if (idx < 0 || idx >= TOTAL_SECTIONS) return true;
        return activeSections.get(idx);
    }
    public int getSkippedCount() {
        return TOTAL_SECTIONS - activeCount;
    }
    public float getRenderRatio() {
        return (float) activeCount / TOTAL_SECTIONS;
    }
}

