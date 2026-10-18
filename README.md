# DepthGate

FPS helper for **old hardware**. Client-side Fabric mod for Minecraft **1.21 / 1.21.1** (Java 21, Fabric API).

DepthGate does not replace the renderer. Use it next to Sodium (and Lithium / Distant Horizons if you like).

## Features

1. **Hardware tiering** – reads the GPU string, CPU threads and max heap, picks POTATO / LOW / MEDIUM / HIGH and lowers expensive settings **once** (view/simulation distance, AO, biome blend, entity shadows, clouds, particles, entity distance, graphics mode). It only ever lowers values. MEDIUM and HIGH change nothing. Set `forceTier` to override, `autoTier: false` to disable.
2. **Adaptive view distance** – samples FPS once per second, lowers distance by 1 when the average is clearly under `fpsTarget`, restores it toward *your* setting when FPS is healthy (with exponential back-off, so it does not oscillate). Manual changes are respected. Your own value is restored on disconnect/quit. Going above your setting is opt-in (`allowDistanceBoost`).
3. **Particle cap** – hard limit on live particles (`particleCap`, 0 = unlimited).
4. **GC advisor** – detects the collector and tells you if you are not on Shenandoah/ZGC.
5. **Debug HUD** – FPS, approximate server TPS, ping, coordinates, CPU, GPU and RAM appear beside the vanilla F3 screen.

## Config (`config/depthgate.json`)

```json
{
  "forceTier": "AUTO",
  "autoTier": true,
  "adaptiveDistanceEnabled": true,
  "fpsTarget": 60,
  "minViewDistance": 4,
  "allowDistanceBoost": false,
  "extendedDistanceMax": 4,
  "particleSuppressionEnabled": true,
  "particleCap": 1000,
  "gcWarningChat": true,
  "debugHudEnabled": true
}
```

## GC recommendation

```
-XX:+UseShenandoahGC -XX:+UnlockExperimentalVMOptions -XX:ShenandoahGuaranteedGCInterval=10000
```

(Shenandoah is missing from some Java builds, use Temurin/Adoptium. `-XX:+UseZGC -XX:+ZGenerational` is the alternative.)

## Removed in 1.1 (and why)

| Old feature | Why it is gone |
| :--- | :--- |
| Vertical section culling / skip mesh rebuilds / entity section culling | Cancelling rebuilds left holes in the world when you moved, vanilla (and Sodium) already occlusion-cull hidden sections so the FPS gain was ~0, and it breaks through-wall rendering. |
| Chunk delivery limiter | Dropped chunk packets are never re-sent, giving permanent holes. |
| Light-update throttle | Counted calls, not updates, so it did almost nothing and could stall lighting. |
| Chunk-gen budget, async chunk save | Were never wired up (no-ops). Vanilla already writes chunks on a background IO thread. |
| Hopper skipping | Shifts hopper timing and breaks redstone clocks/sorters. Lithium does this properly. |
| GC pre-warm (`System.gc()`) | Caused a freeze the moment the world appeared. |

## Build

```
./gradlew build
```
Jar: `build/libs/`.

## License

CC0-1.0
