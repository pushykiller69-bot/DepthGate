package com.depthgate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class DepthGateConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static DepthGateConfig INSTANCE;

    /** AUTO detects hardware. Otherwise POTATO, LOW, MEDIUM or HIGH. MEDIUM/HIGH never change your settings. */
    public String forceTier = "AUTO";
    /** Detect hardware and apply the matching tier once (re-applied only if the tier changes). */
    public boolean autoTier = true;
    /** Tier whose settings were last applied. Managed by the mod. */
    public String appliedTier = "";

    /** Adjust render distance to maintain the target FPS. */
    public boolean adaptiveDistanceEnabled = true;
    /** FPS target for adaptive view distance. */
    public int fpsTarget = 60;
    /** Lowest view distance adaptive mode can use. */
    public int minViewDistance = 4;
    /** Allow adaptive mode to exceed your saved view distance. */
    public boolean allowDistanceBoost = false;
    /** Maximum blocks above your saved view distance when boosting is enabled. */
    public int extendedDistanceMax = 4;

    /** Limit active particles; set to 0 for no limit. */
    public boolean particleSuppressionEnabled = true;
    public int particleCap = 1000;

    /** Show a chat warning when a high-pause garbage collector is detected. */
    public boolean gcWarningChat = true;
    /** Show DepthGate statistics beside the F3 debug screen. */
    public boolean debugHudEnabled = true;

    public static DepthGateConfig get() {
        if (INSTANCE == null) load();
        return INSTANCE;
    }

    public static synchronized void load() {
        Path file = path();
        DepthGateConfig loaded = null;
        if (Files.exists(file)) {
            try {
                loaded = GSON.fromJson(Files.readString(file), DepthGateConfig.class);
            } catch (Exception e) {
                DepthGateMod.LOGGER.error("[DepthGate] Config unreadable, backing up and resetting to defaults", e);
                try {
                    Files.copy(file, file.resolveSibling("depthgate.json.bak"), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException backupError) {
                    DepthGateMod.LOGGER.error("[DepthGate] Failed to back up unreadable config", backupError);
                }
            }
        }
        INSTANCE = loaded != null ? loaded : new DepthGateConfig();
        INSTANCE.validate();
        save();
    }

    public static synchronized void save() {
        if (INSTANCE == null) return;
        try {
            Path file = path();
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(INSTANCE));
        } catch (IOException e) {
            DepthGateMod.LOGGER.error("[DepthGate] Failed to save config", e);
        }
    }

    private void validate() {
        if (forceTier == null) forceTier = "AUTO";
        if (appliedTier == null) appliedTier = "";
        fpsTarget = clamp(fpsTarget, 20, 240);
        minViewDistance = clamp(minViewDistance, 2, 16);
        extendedDistanceMax = clamp(extendedDistanceMax, 0, 16);
        particleCap = clamp(particleCap, 0, 20000);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("depthgate.json");
    }
}
