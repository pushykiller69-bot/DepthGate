package com.depthgate.client;

import com.depthgate.DepthGateConfig;
import com.depthgate.DepthGateMod;
import com.depthgate.engine.AdaptiveDistanceManager;
import com.depthgate.engine.GCDetector;
import com.depthgate.engine.PerformanceTier;
import com.depthgate.engine.ServerTpsTracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.ParticlesMode;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import org.lwjgl.opengl.GL11;

public class DepthGateClient implements ClientModInitializer {
    private static final int JOIN_GRACE_TICKS = 20 * 20;

    private static final AdaptiveDistanceManager DISTANCE = new AdaptiveDistanceManager();
    private static volatile PerformanceTier activeTier = PerformanceTier.MEDIUM;
    private static volatile String gpuName = "Unknown";

    private boolean tierChecked = false;
    private boolean gcWarningShown = false;
    private ClientWorld lastWorld = null;
    private int ticksInWorld = 0;
    private int tickCounter = 0;

    public static AdaptiveDistanceManager distance() { return DISTANCE; }
    public static PerformanceTier tier() { return activeTier; }
    public static String gpu() { return gpuName; }

    @Override
    public void onInitializeClient() {
        DepthGateConfig.load();
        GCDetector gc = GCDetector.getInstance();
        gc.detect();
        if (gc.isLowPauseCollector()) {
            DepthGateMod.LOGGER.info("[DepthGate] GC: {} (low-pause)", gc.getGCTypeName());
        } else {
            DepthGateMod.LOGGER.warn("[DepthGate] GC: {}. Shenandoah or ZGC reduces stutter.", gc.getGCTypeName());
        }

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ServerTpsTracker.reset();
            restoreDistance(client);
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(this::restoreDistance);
        DepthGateHudOverlay.register();
    }

    private void onTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        DepthGateConfig config = DepthGateConfig.get();

        if (client.world != lastWorld) {
            lastWorld = client.world;
            ticksInWorld = 0;
        }
        ticksInWorld++;

        if (!tierChecked) {
            tierChecked = true;
            detectAndApplyTier(client, config);
        }

        if (!gcWarningShown) {
            gcWarningShown = true;
            String warning = GCDetector.getInstance().getWarningMessage();
            if (warning != null && config.gcWarningChat) {
                client.player.sendMessage(Text.literal(warning), false);
            }
        }

        boolean joinRampRunning = Boolean.getBoolean("netsmooth.rampActive");
        if (config.adaptiveDistanceEnabled && ticksInWorld > JOIN_GRACE_TICKS && !joinRampRunning
                && client.isWindowFocused() && !client.isPaused()) {
            SimpleOption<Integer> view = client.options.getViewDistance();
            AdaptiveDistanceManager.Action action = DISTANCE.tick(
                    System.currentTimeMillis(), client.getCurrentFps(), view.getValue(),
                    config.fpsTarget, config.minViewDistance,
                    config.allowDistanceBoost, config.extendedDistanceMax);
            if (action.changed()) {
                view.setValue(action.newDistance());
            }
        }

        if (++tickCounter % 20 == 0) {
            GCDetector.getInstance().updateStats();
        }
    }

    private void detectAndApplyTier(MinecraftClient client, DepthGateConfig config) {
        String renderer = null;
        try {
            renderer = GL11.glGetString(GL11.GL_RENDERER);
        } catch (Throwable t) {
            DepthGateMod.LOGGER.warn("[DepthGate] Could not read GL renderer", t);
        }
        gpuName = renderer == null ? "Unknown" : renderer;

        int cores = Runtime.getRuntime().availableProcessors();
        long heapMb = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        PerformanceTier forced = PerformanceTier.parse(config.forceTier);
        PerformanceTier tier = forced != null ? forced : PerformanceTier.compute(renderer, cores, heapMb);
        activeTier = tier;
        DepthGateMod.LOGGER.info("[DepthGate] GPU: {} | cores: {} | heap: {}MB -> tier {}{}",
                gpuName, cores, heapMb, tier, forced != null ? " (forced)" : "");

        boolean allowed = forced != null || config.autoTier;
        if (allowed && !tier.name().equals(config.appliedTier)) {
            applyTier(client, tier, config);
            config.appliedTier = tier.name();
            DepthGateConfig.save();
        }
    }

    private void applyTier(MinecraftClient client, PerformanceTier tier, DepthGateConfig config) {
        GameOptions o = client.options;
        switch (tier) {
            case POTATO -> {
                capInt(o.getViewDistance(), 6);
                capInt(o.getSimulationDistance(), 5);
                o.getGraphicsMode().setValue(GraphicsMode.FAST);
                o.getAo().setValue(false);
                o.getBiomeBlendRadius().setValue(0);
                o.getEntityShadows().setValue(false);
                o.getCloudRenderMode().setValue(CloudRenderMode.OFF);
                o.getParticles().setValue(ParticlesMode.MINIMAL);
                capDouble(o.getEntityDistanceScaling(), 0.75);
                if (config.particleCap == 0 || config.particleCap > 400) config.particleCap = 400;
            }
            case LOW -> {
                capInt(o.getViewDistance(), 8);
                capInt(o.getSimulationDistance(), 6);
                if (o.getGraphicsMode().getValue() == GraphicsMode.FABULOUS) {
                    o.getGraphicsMode().setValue(GraphicsMode.FAST);
                }
                o.getAo().setValue(false);
                capInt(o.getBiomeBlendRadius(), 1);
                o.getEntityShadows().setValue(false);
                if (o.getCloudRenderMode().getValue() == CloudRenderMode.FANCY) {
                    o.getCloudRenderMode().setValue(CloudRenderMode.FAST);
                }
                if (o.getParticles().getValue() == ParticlesMode.ALL) {
                    o.getParticles().setValue(ParticlesMode.DECREASED);
                }
                capDouble(o.getEntityDistanceScaling(), 1.0);
                if (config.particleCap == 0 || config.particleCap > 700) config.particleCap = 700;
            }
            default -> { }
        }
        o.write();
        DepthGateMod.LOGGER.info("[DepthGate] Applied {} settings", tier);
    }

    private static void capInt(SimpleOption<Integer> opt, int max) {
        if (opt.getValue() > max) opt.setValue(max);
    }

    private static void capDouble(SimpleOption<Double> opt, double max) {
        if (opt.getValue() > max) opt.setValue(max);
    }

    private void restoreDistance(MinecraftClient client) {
        int user = DISTANCE.getUserDistance();
        if (user != -1 && client.options != null) {
            SimpleOption<Integer> view = client.options.getViewDistance();
            if (view.getValue() != user) {
                view.setValue(user);
                client.options.write();
            }
        }
        DISTANCE.reset();
        lastWorld = null;
    }
}
