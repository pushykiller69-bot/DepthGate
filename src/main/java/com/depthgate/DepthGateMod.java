package com.depthgate;
import com.depthgate.engine.AsyncChunkIOManager;
import com.depthgate.engine.GCDetector;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class DepthGateMod implements ModInitializer {
	public static final String MOD_ID = "depthgate";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	@Override
	public void onInitialize() {
		DepthGateConfig.load();
		DepthGateConfig config = DepthGateConfig.get();

		// Detect GC type early — this is the single highest-impact user-facing change
		GCDetector.getInstance().detect();
		if (!GCDetector.getInstance().isLowPauseCollector()) {
			LOGGER.warn("[DepthGate] Detected GC: {}. For best performance, switch to Shenandoah (-XX:+UseShenandoahGC). " +
					"This alone can reduce GC pauses from 50-200ms to under 5ms.", GCDetector.getInstance().getGCTypeName());
		} else {
			LOGGER.info("[DepthGate] Detected GC: {} — low-pause collector, excellent.", GCDetector.getInstance().getGCTypeName());
		}

		// Start async chunk save thread if enabled
		if (config.asyncChunkSaveEnabled) {
			AsyncChunkIOManager.getInstance().start(config.asyncSaveQueueLimit);
			LOGGER.info("[DepthGate] Async chunk save thread started (queue limit: {})", config.asyncSaveQueueLimit);
		}

		LOGGER.info("[DepthGate] Initialized — Vertical Culling + Adaptive Distance + FPS Optimization");
	}
}
