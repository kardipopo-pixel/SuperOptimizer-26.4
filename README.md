# SuperOptimizer 26.4

**Minecraft target:** `26.4-alpha.2` (the version string reported by the user's Fabric Loader for Snapshot 2)  
**Loader:** Fabric Loader `0.19.5`  
**Java:** 25

## Current status

This repository is a clean rebuild after the earlier experimental JAR failed during Mixin preparation because `MinecraftClientTickMixin` was missing a valid `@Mixin` annotation. The fragile mixin has been removed completely.

The current baseline deliberately contains **no mixins and no renderer hooks**. It only verifies that Fabric can load a client-only mod built against the configured target dependencies. No FPS improvement is claimed yet. Optimization features will be introduced one at a time after a successful build and runtime test.

## Build locally

Install a Java 25 JDK and Gradle 9.2.1, then run:

```sh
gradle --no-daemon clean build
```

The JAR is written to `build/libs/`. GitHub Actions runs the same build on pushes and pull requests and uploads the JAR as an artifact if compilation succeeds.

## Design requirements

- Do not modify shaderpacks or shader formulas. Preserve the original Super Duper Vanilla visuals.
- Conservative visibility behavior: if visibility is uncertain, do not cull.
- Never access mutable Minecraft world state from worker threads.
- Keep CPU work bounded and avoid unbounded queues or per-frame object churn.
- Do not bundle or copy Sodium, Iris, FerriteCore, or Entity Culling code.
- Russian-language settings UI and optimization features come after baseline runtime validation.

## Important Vulkan/Iris note

This project does not claim to provide Iris-on-Vulkan support. It does not embed Iris or alter the selected graphics backend. Compatibility must be verified against the exact Minecraft snapshot and installed mod versions.

## Build status

A successful CI artifact means compilation succeeded against the declared dependency coordinates. It does **not** by itself prove that the mod launches in a full client or improves FPS; runtime testing is still required.
