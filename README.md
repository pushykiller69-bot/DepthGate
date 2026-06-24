<div align="center">
  <h1>🌌 DepthGate</h1>
  <p><strong>The ultimate vertical culling and rendering optimization mod for Minecraft Fabric.</strong></p>
</div>

## 📌 Overview
**DepthGate** fundamentally changes how Minecraft handles chunk data and rendering. Instead of loading the entire 384-block-tall vertical chunk column into memory at all times, DepthGate intelligently culls and unloads vertical chunk sections (16x16x16) that are far above or below you. 

By saving up to **60% of vertical processing budget**, DepthGate reinvests that memory to push your horizontal render distance further without sacrificing FPS.

## 🛠️ Requirements
* **Minecraft Version:** 1.21.1
* **Mod Loader:** Fabric (>=0.19.3)
* **Java Version:** Java 21+
* **Dependencies:** Fabric API

## 🚀 Features

### 1. Vertical Section Culling (Client-Side)
Vanilla Minecraft always loads 24 vertical sections per chunk (from `Y=-64` to `Y=320`). DepthGate detects your Y-level and only loads the sections immediately relevant to you, plus the surface sections required for accurate skylight propagation. Surface band is derived from world sea level, so it works correctly in superflat and custom worlds.

### 2. Adaptive Distance Boost (Client-Side)
Because Vertical Culling frees up so much memory and CPU overhead, DepthGate dynamically increases your horizontal render distance when your FPS is stable, allowing you to see further than your vanilla settings. Uses frame time variance smoothing to prevent oscillation — only adjusts when both average FPS and frame stability are above threshold.

### 3. Server-Side Hopper Fix (Server-Side)
In vanilla, empty hoppers scan for items above them every single tick, causing massive tick lag on large servers. DepthGate intercepts empty hoppers and forces them to only process every N ticks, with staggering by position to prevent load spikes.

### 4. Light Engine Throttling (Server-Side)
Prevents massive FPS and TPS lag spikes caused by large TNT explosions or rapid block breaking by hard-capping the amount of light updates allowed per tick.

### 5. Particle Hard-Cap (Client-Side)
Silently discards new particles if the on-screen count exceeds the configured cap to prevent stuttering in chaotic environments.

### 6. Async Chunk Save Queue (Server-Side)
Pushes chunk saves to a background daemon thread at low priority. Eliminates disk I/O stalls completely, especially on SATA systems. Includes configurable queue depth with backpressure fallback.

### 7. Chunk Generation Time Budget (Server-Side)
Limits terrain generation to a configurable time budget per tick (default 10ms). If generation needs more time, remaining chunks are deferred to the next tick. Chunks load slowly but without freeze spikes.

### 8. GC Detection & Warning
Detects the active garbage collector at startup. If you're not running Shenandoah or ZGC, warns you prominently — switching to Shenandoah reduces GC pause from 50–200ms down to under 5ms, more impact than any code change.

### 9. Skip Mesh Upload for Culled Sections (Client-Side)
Cancels mesh rebuilds for vertically culled sections, preventing the chunk builder from uploading their mesh to shared RAM. Biggest bandwidth win for iGPU users.

### 10. Chunk Delivery Rate Limiter (Client-Side)
Caps chunk delivery at 2 per tick on detected iGPU, 6 per tick otherwise. Prevents bandwidth spike when chunks finish generating all at once.

### 11. GC Pre-Warm on World Load
Calls `System.gc()` during the loading screen transition — the one moment a GC pause is invisible to the player. Clears young gen before terrain generation begins.

### 12. iGPU Auto-Detection Preset (Client-Side)
Reads `GL_RENDERER` at startup. If it detects Intel UHD/HD/Iris Xe or AMD Vega, applies a conservative "potato preset" automatically for optimal performance on shared RAM.

### 13. Entity Section Culling (Client-Side)
Skips rendering entities whose Y section is marked inactive by the vertical cull manager. Big win on iGPU since entity draw calls are expensive on shared RAM.

### 14. Debug HUD Overlay (Client-Side)
Press F3 to see DepthGate stats: current GC type, last pause duration, GPU info, active section count, distance boost, and RAM usage.

## 🔧 GC Recommendation
For best performance, add these JVM arguments to your Minecraft launcher:
```
-XX:+UseShenandoahGC -XX:+UnlockExperimentalVMOptions -XX:ShenandoahGuaranteedGCInterval=10000
```
This alone can reduce GC pauses from 50–200ms to under 5ms.

## ⚙️ Configuration
All features are fully customizable. The config file will generate at `.minecraft/config/depthgate.json` after your first launch.

```json
{
  "verticalCullingEnabled": true,
  "verticalRange": 3,
  "bufferSections": 2,
  "alwaysKeepSurface": true,
  "adaptiveDistanceEnabled": true,
  "extendedDistanceMax": 8,
  "fpsTarget": 60,
  "hopperOptEnabled": true,
  "hopperSkipTicks": 8,
  "particleSuppressionEnabled": true,
  "particleCap": 1500,
  "lightOptEnabled": true,
  "lightUpdateBatchSize": 64,
  "chunkGenBudgetEnabled": true,
  "chunkGenBudgetMs": 10,
  "asyncChunkSaveEnabled": true,
  "asyncSaveQueueLimit": 256,
  "chunkDeliveryLimitEnabled": true,
  "chunkDeliveryCapIGPU": 2,
  "chunkDeliveryCapDefault": 6,
  "entitySectionCullingEnabled": true,
  "debugHudEnabled": true,
  "igpuAutoPreset": true
}
```

## 🤝 Compatibility
DepthGate is designed to run *alongside* other major optimization mods. It intercepts data before it reaches the GPU pipeline.
* **Sodium:** Fully Compatible
* **Lithium:** Fully Compatible (Note: Lithium also has minor hopper optimizations, they work fine together)
* **Distant Horizons:** Compatible (DepthGate handles the close-range real chunks, DH handles the far LODs)

Tested on:
- Fabric 1.20.4 ✓
- Fabric 1.20.1 ✓  
- 1.21 — not yet (soon)

## ⚠️ Known Issues
- Extended distance boost feels too aggressive between 28-32 fps, 
  still tuning the curve
- Biome color cache has occasional misses at chunk borders (investigating)
- **Do not set lightUpdateBatchSize below 8** — causes visual artifacts,
  haven't found the root cause yet
- Entity culling occasionally causes a single-frame pop-in for entities at section boundaries when flying vertically at high speed. Working on interpolation-based fix.

## 🏗️ Building from Source
1. Clone the repository
2. Run `./gradlew build`
3. Retrieve the compiled `.jar` from `/build/libs/`

## 📄 License
This project is licensed under the CC0-1.0 License. You are free to fork, modify, and distribute it.
