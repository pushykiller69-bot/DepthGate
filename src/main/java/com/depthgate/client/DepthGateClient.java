package com.depthgate.client;
import com.depthgate.engine.VerticalCullManager;
import com.depthgate.engine.AdaptiveDistanceManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
public class DepthGateClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                VerticalCullManager.getInstance().update();
                AdaptiveDistanceManager.getInstance().tick();
            }
        });
    }
}

