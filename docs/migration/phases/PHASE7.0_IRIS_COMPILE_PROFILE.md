# Phase 7.0 - Iris Compile-Only Profile

Date: 2026-06-25

## Objective

Open a dedicated Iris compile-only profile without changing the default vanilla
profile, without regressing the Sodium non-shader baseline, and without loading
Iris at runtime.

## Baselines

- Vanilla baseline: `5545115e50e2b98b46303d7cf319d5d96f41ba5b`
- Sodium non-shader baseline: `545169c1dc49418f91da8c13b70ff40cb0ebc8a6`

## Gradle Profile

Added:

```properties
enable_iris_compat=false
```

The compile-only Iris profile is enabled with:

```powershell
.\gradlew.bat compileJava processResources --rerun-tasks --console=plain -Penable_iris_compat=true
```

In that profile:

- Iris is added as `compileOnly`.
- Sodium is also added as `compileOnly`, because `MixinIrisSodiumShader`
  references Sodium shader binding classes.
- Iris is not added as `runtimeOnly`.
- `enable_iris=false` remains the runtime switch and stays false.
- `imm_ptl_compat.mixins.json` is still excluded from processed resources unless
  Sodium runtime mixins are explicitly enabled by the existing Sodium profile.

## Files Reactivated For Compilation

Iris compile-only now includes:

- `src/main/java/qouteall/imm_ptl/core/compat/mixin/iris/**`
- `src/main/java/qouteall/imm_ptl/core/compat/iris_compatibility/ExperimentalIrisPortalRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/compat/iris_compatibility/IEIrisNewWorldRenderingPipeline.java`
- `src/main/java/qouteall/imm_ptl/core/compat/iris_compatibility/IEIrisShadowRenderTargets.java`
- `src/main/java/qouteall/imm_ptl/core/compat/iris_compatibility/IPIrisHelper.java`
- `src/main/java/qouteall/imm_ptl/core/compat/iris_compatibility/IrisCompatibilityPortalRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/compat/iris_compatibility/IrisPortalRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/compat/iris_compatibility/ShadowMapSwapper.java`

## Corrective Changes

The legacy Iris renderers depended on the pre-26.1 immediate
framebuffer/stencil path (`bindWrite`, `unbindWrite`, raw texture ids,
`RenderSystem.getProjectionMatrix`, and old stencil helpers). They are now
explicit compile-only facades:

- `ExperimentalIrisPortalRenderer`
- `IrisPortalRenderer`
- `IrisCompatibilityPortalRenderer`

These classes keep their public hooks available for mixins and future runtime
work, but they do not perform Iris portal rendering in Phase 7.0.

`IPIrisHelper` was reduced to safe public 26.1 APIs:

- depth copies use `RenderTarget.copyDepthFrom`;
- legacy color/stencil raw GL texture copying is no-op until Iris runtime work.

`MixinIrisTransformPatcher` now uses:

```java
ShaderType.VERTEX
```

instead of removed legacy `Program.Type.VERTEX`.

## Runtime Status

Iris runtime is not enabled in this phase.

Still not activated:

- Iris runtime;
- shaderpacks;
- DimLib;
- AlternateDimensions dynamic;
- Sodium shader mixins;
- Iris mixins at runtime;
- shader clipping;
- old advanced Iris renderer.

## Validation

Vanilla:

```powershell
.\gradlew.bat compileJava processResources --rerun-tasks --console=plain
```

Result: BUILD SUCCESSFUL.

Sodium compile-only:

```powershell
.\gradlew.bat compileJava processResources --rerun-tasks --console=plain -Penable_sodium_compat=true
```

Result: BUILD SUCCESSFUL.

Iris compile-only:

```powershell
.\gradlew.bat compileJava processResources --rerun-tasks --console=plain -Penable_iris_compat=true
```

Result: BUILD SUCCESSFUL.

## Remaining Iris Work

The next Iris phase should not assume runtime rendering works. The runtime
renderer is intentionally disabled behind no-op facades. The next safe step is
to open Iris runtime mixins progressively, starting with non-rendering or
low-risk probes, then rebuilding the Iris portal renderer on the 26.1 render
graph.

## Artifacts

- `compile-phase7.0-vanilla.txt`
- `compile-phase7.0-sodium.txt`
- `compile-phase7.0-iris.txt`
- `git-diff-phase7.0.txt`
