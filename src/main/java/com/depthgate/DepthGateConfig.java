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
    public boolean verticalCullingEnabled = true;
    public int verticalRange = 3;
    public int bufferSections = 2;
    public boolean alwaysKeepSurface = true;
    public boolean adaptiveDistanceEnabled = true;
    public int extendedDistanceMax = 8;
    public int fpsTarget = 60;
    public boolean hopperOptEnabled = true;
    public int hopperSkipTicks = 8;
    public boolean particleSuppressionEnabled = true;
    public int particleCap = 1500;
    public boolean lightOptEnabled = true;
    public int lightUpdateBatchSize = 64;
    public boolean biomeBlendOptEnabled = true;
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

