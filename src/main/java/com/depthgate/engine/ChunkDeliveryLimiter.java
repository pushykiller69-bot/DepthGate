package com.depthgate.engine;
/**
 * Rate-limits chunk delivery to the client to prevent bandwidth spikes when
 * many chunks finish generating at once. Especially important on iGPU where
 * shared RAM bandwidth is limited.
 *
 * <p>Pure Java — no Minecraft dependencies.</p>
 */
public class ChunkDeliveryLimiter {
    private static final ChunkDeliveryLimiter INSTANCE = new ChunkDeliveryLimiter();

    private int deliveredThisTick = 0;
    private int capPerTick = 6;

    public static ChunkDeliveryLimiter getInstance() {
        return INSTANCE;
    }

    /**
     * Set the delivery cap for this session.
     *
     * @param cap max chunks per tick (2 for iGPU, 6 for discrete)
     */
    public void setCap(int cap) {
        this.capPerTick = cap;
    }

    /**
     * Reset the per-tick counter. Call at the start of each client tick.
     */
    public void resetTick() {
        deliveredThisTick = 0;
    }

    /**
     * Check if another chunk can be delivered this tick.
     */
    public boolean canDeliverChunk() {
        return deliveredThisTick < capPerTick;
    }

    /**
     * Record that a chunk was delivered.
     */
    public void onChunkDelivered() {
        deliveredThisTick++;
    }

    public int getDeliveredThisTick() {
        return deliveredThisTick;
    }

    public int getCapPerTick() {
        return capPerTick;
    }
}
