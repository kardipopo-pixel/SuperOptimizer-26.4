# SuperOptimizer 26.4

Client-side Fabric optimization mod prototype for Minecraft 26.4 Snapshot 2 / `26.4-alpha.2`.

## Important status

This repository is being rebuilt from a clean baseline after the previous experimental JAR crashed during Mixin preparation. The old `MinecraftClientTickMixin` was invalid and is intentionally **not included** in this baseline. No performance or runtime compatibility claims are made until CI compiles against the real Minecraft/Fabric dependencies and the client is tested.

## Goals

- Preserve shaderpack visuals: no shader files or shader formulas are modified.
- Be conservative: if visibility is uncertain, render the object.
- Keep expensive world queries off the render thread only when they can be done safely from snapshots.
- Provide Russian configuration/UI in a later, separately tested step.
- Never bundle or copy Sodium, Iris, FerriteCore, or Entity Culling code.

## Build

Use Java 25. Run `gradle build` with Gradle 9.x, or use the GitHub Actions workflow. If the selected Minecraft/Fabric artifact coordinates are unavailable, CI should fail rather than producing a fake JAR.

## Current limitations

The current baseline intentionally contains no mixins and no active culling logic. First goal is a clean, reproducible Fabric build for the target version; optimizations will be added only after target mappings and runtime hooks are verified.
