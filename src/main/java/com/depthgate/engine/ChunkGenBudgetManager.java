package com.depthgate.engine;
/**
 * Limits terrain generation to a fixed time budget per tick (default 10ms).
 * If generation takes longer, remaining chunks are deferred to the next tick.
 * Chunks load slowly but without freeze spikes.
 *
 * <p>Pure Java — no Minecraft dependencies.</p>
 */
public class ChunkGenBudgetManager {
    private static final ChunkGenBudgetManager INSTANCE = new ChunkGenBudgetManager();

    // considered using nanoTime per-chunk but the overhead of calling it
    // inside the mixin inject was measurable on i5 hardware — using tickCounter instead
    private long lastTickStart = 0;
    private long budgetNanos = 10_000_000L; // 10ms default
    private int chunksTick = 0;

    public static ChunkGenBudgetManager getInstance() {
        return INSTANCE;
    }

    /**
     * Call at the start of each server tick to reset the budget.
     *
     * @param budgetMs maximum milliseconds allowed for chunk generation this tick
     */
    public void beginTick(int budgetMs) {
        lastTickStart = System.nanoTime();
        budgetNanos = budgetMs * 1_000_000L;
        chunksTick = 0;
    }

    /**
     * Check if the budget allows generating another chunk this tick.
     *
     * @return true if there's time remaining in the budget
     */
    public boolean canGenerateMore() {
        return (System.nanoTime() - lastTickStart) < budgetNanos;
    }

    /**
     * Record that a chunk was generated this tick.
     */
    public void markChunkGenerated() {
        chunksTick++;
    }

    public int getChunksThisTick() {
        return chunksTick;
    }

    public long getElapsedMs() {
        return (System.nanoTime() - lastTickStart) / 1_000_000L;
    }
}
