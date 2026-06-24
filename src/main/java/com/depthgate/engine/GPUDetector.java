package com.depthgate.engine;
import java.util.Locale;
/**
 * Detects integrated GPUs by parsing the GL_RENDERER string.
 * When an iGPU is found, applies a "potato preset" that's tuned for
 * shared RAM constraints.
 *
 * <p>Pure Java — the GL string is passed in by the caller.</p>
 */
public class GPUDetector {
    private static final GPUDetector INSTANCE = new GPUDetector();

    private boolean integratedGPU = false;
    private String rendererName = "Unknown";

    public static GPUDetector getInstance() {
        return INSTANCE;
    }

    /**
     * Analyze the GL_RENDERER string for known iGPU signatures.
     *
     * @param glRenderer the value from GL11.glGetString(GL_RENDERER)
     */
    public void detect(String glRenderer) {
        if (glRenderer == null) return;
        rendererName = glRenderer;
        String lower = glRenderer.toLowerCase(Locale.ROOT);
        integratedGPU = lower.contains("intel")
                || lower.contains("uhd")
                || lower.contains("hd graphics")
                || lower.contains("iris xe")
                || lower.contains("vega")       // AMD APU
                || lower.contains("radeon(tm)"); // AMD APU variant
    }

    public boolean isIntegratedGPU() {
        return integratedGPU;
    }

    public String getRendererName() {
        return rendererName;
    }

    /**
     * Returns recommended config overrides for iGPU systems.
     */
    public record IGPUPreset(
        int vCullDepth,
        int particleCap,
        int lightUpdateBatchSize,
        int hopperSkipTicks,
        int chunkDeliveryCap
    ) {}

    public IGPUPreset getPotatoPreset() {
        return new IGPUPreset(2, 300, 32, 16, 2);
    }
}
