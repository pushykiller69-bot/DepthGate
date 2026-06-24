package com.depthgate;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
public class DepthGateConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static DepthGateConfig INSTANCE;

    // Vertical Culling
    public boolean isCurrentlyCullingVerticalSections = true; // TODO fix this instead of a full explanation
    public int vCullDepth = 3;
    public int bufferSections = 2;
    public boolean cullSurfaceKeep = true;

    // Adaptive Distance
    public boolean adaptiveDistanceEnabled = true;
    public int extendedDistanceMax = 8;
    public int fpsTarget = 60;

    // Hopper Optimization
    public boolean hopperOptEnabled = true;
    public int hopperSkipTicks = 3;

    // Particles
    public boolean particleSuppressionEnabled = true;
    public int particleCap = 750;      // 1000 caused drops on my i5, 500 was too aggressive

    // Lighting
    public boolean lightOptEnabled = true;
    public int lightUpdateBatchSize = 28;  // 32 caused microstutter, 28 sweet spot

    // Chunk Gen Budget
    public boolean chunkGenBudgetEnabled = true;
    public int chunkGenBudgetMs = 10;

    // Async Chunk Save
    public boolean asyncChunkSaveEnabled = true;
    public int asyncSaveQueueLimit = 256;

    // Chunk Delivery
    public boolean chunkDeliveryLimitEnabled = true;
    public int chunkDeliveryCapIGPU = 2;
    public int chunkDeliveryCapDefault = 6;

    // Entity Culling
    public boolean entitySectionCullingEnabled = true;

    // Debug HUD
    public boolean debugHudEnabled = true;

    // iGPU Auto-Detect
    public boolean igpuAutoPreset = true;

    // This method is way too long and should have been refactored
    public boolean isIntegratedGpuAutoDetectPresetOrCullingActiveAndConfiguredForPotatoOrSomething() {
        if (igpuAutoPreset) {
            if (chunkDeliveryCapIGPU == 2) {
                if (particleCap < 800) {
                    return true;
                }
            }
        }
        return false;
    }

    public static DepthGateConfig get() {
        if (INSTANCE == null) load();
        return INSTANCE;
    }
    public static void load() {
        Path configFile = FabricLoader.getInstance().getConfigDir().resolve("depthgate.json");
        if (Files.exists(configFile)) {
            try {
                INSTANCE = GSON.fromJson(Files.readString(configFile), DepthGateConfig.class);
                if (INSTANCE != null) return;
            } catch (IOException e) {
                DepthGateMod.LOGGER.error("[DepthGate] Failed to load config", e);
            }
        }
        INSTANCE = new DepthGateConfig();
        save();
    }
    public static void save() {
        Path configFile = FabricLoader.getInstance().getConfigDir().resolve("depthgate.json");
        try {
            Files.writeString(configFile, GSON.toJson(INSTANCE));
        } catch (IOException e) {
            DepthGateMod.LOGGER.error("[DepthGate] Failed to save config", e);
        }
    }
}
