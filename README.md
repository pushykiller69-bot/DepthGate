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
Vanilla Minecraft always loads 24 vertical sections per chunk (from `Y=-64` to `Y=320`). DepthGate detects your Y-level and only loads the sections immediately relevant to you, plus the surface sections required for accurate skylight propagation. 

### 2. Adaptive Distance Boost (Client-Side)
Because Vertical Culling frees up so much memory and CPU overhead, DepthGate dynamically increases your horizontal render distance when your FPS is stable, allowing you to see further than your vanilla settings. If your FPS drops, it smoothly scales the distance back down to maintain performance.

### 3. Server-Side Hopper Fix (Server-Side)
In vanilla, empty hoppers scan for items above them every single tick, causing massive tick lag on large servers. DepthGate intercepts empty hoppers and forces them to only process every 8 ticks, drastically reducing CPU load on mob farms and storage systems.

### 4. Light Engine Throttling (Server-Side)
Prevents massive FPS and TPS lag spikes caused by large TNT explosions or rapid block breaking by hard-capping the amount of light updates allowed per tick.

### 5. Particle Hard-Cap (Client-Side)
Silently discards new particles if the on-screen count exceeds 1500 to prevent stuttering in chaotic environments.

## ⚙️ Configuration
All features are fully customizable. The config file will generate at `.minecraft/config/depthgate.json` after your first launch.

```json
{
  "verticalCullingEnabled": true,
  "verticalRange": 3,
  "extendedDistanceMax": 8,
  "fpsTarget": 60,
  "hopperOptEnabled": true,
  "hopperSkipTicks": 8,
  "particleSuppressionEnabled": true,
  "particleCap": 1500,
  "lightOptEnabled": true
}
```

## 🤝 Compatibility
DepthGate is designed to run *alongside* other major optimization mods. It intercepts data before it reaches the GPU pipeline.
* **Sodium:** Fully Compatible
* **Lithium:** Fully Compatible (Note: Lithium also has minor hopper optimizations, they work fine together)
* **Distant Horizons:** Compatible (DepthGate handles the close-range real chunks, DH handles the far LODs)

## 🏗️ Building from Source
1. Clone the repository
2. Run `./gradlew build`
3. Retrieve the compiled `.jar` from `/build/libs/`

## 📄 License
This project is licensed under the CC0-1.0 License. You are free to fork, modify, and distribute it.
