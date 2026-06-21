package com.depthgate;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class DepthGateMod implements ModInitializer {
	public static final String MOD_ID = "depthgate";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	@Override
	public void onInitialize() {
		DepthGateConfig.load();
		LOGGER.info("[DepthGate] Initialized — Vertical Section Culling + Extended Distance + FPS Leak Fixes");
	}
}

