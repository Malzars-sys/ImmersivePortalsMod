# Phase 5.11 - Framebuffer Quad Orientation

Date: 2026-06-18

## Scope

Phase 5.11 focused only on the visual mapping of the minimal recursive
framebuffer quad. Sodium, Iris, DimLib, shader mixins, fog mixins, clipping
mixins, advanced stencil and advanced framebuffer rendering were not re-enabled.

## Files Inspected

- `src/main/java/qouteall/imm_ptl/core/render/renderer/RendererUsingFrameBuffer.java`
- `src/main/java/qouteall/imm_ptl/core/render/MyRenderHelper.java`
- `src/main/java/qouteall/imm_ptl/core/render/PortalEntityRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/render/renderer/PortalRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/render/SecondaryFrameBuffer.java`
- `src/main/java/qouteall/imm_ptl/core/render/pipeline/IPRenderPipelines.java`
- `src/main/java/qouteall/imm_ptl/core/render/context_management/RenderStates.java`
- `net.minecraft.client.renderer.rendertype.RenderTypes` via `javap`
- `net.minecraft.client.renderer.SubmitNodeCollector` via `javap`

## Files Modified

- `src/main/java/qouteall/imm_ptl/core/render/MyRenderHelper.java`
- `src/main/java/qouteall/imm_ptl/core/render/PortalEntityRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/render/renderer/PortalRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/render/renderer/RendererUsingFrameBuffer.java`
- `MIGRATION_PLAN_26.1.md`

## Changes

- Flipped the framebuffer quad V coordinates. The top portal vertices now map to
  `v=1` and bottom vertices to `v=0`, matching the OpenGL framebuffer texture
  orientation more closely.
- Added a minimal entity-pose overload for framebuffer quad drawing. The
  immediate blit path can now use the same local portal corners as the cyan
  debug frame.
- Routed minimal portal entity rendering directly through
  `rendererUsingFrameBuffer` when `useMinimalRecursivePortalRendering` is active,
  instead of depending on the global renderer pointer.
- Split the minimal recursive path into two stages:
  - render destination content from the non-reentrant `GameRenderer.renderLevel`
    hook;
  - attempt the framebuffer quad blit later from `PortalEntityRenderer`.
- Added one-shot diagnostics for framebuffer size, window size and transformed
  quad vertices.
- Added `IMM_PTL_MINIMAL_RECURSIVE_PORTAL_SCREENSHOT` so the opt-in screenshot
  filename can be changed per phase.

## Validation

- `compileJava processResources`: BUILD SUCCESSFUL
- `runClient --quickPlaySingleplayer=Phase50Test`: BUILD SUCCESSFUL
- Portal created automatically by dev test command: yes
- Portal collected by `PortalEntityRenderer`: yes
- Destination framebuffer rendered from `GameRenderer.renderLevel`: yes
- Deferred framebuffer blit from `PortalEntityRenderer`: yes
- Framebuffer blit succeeded: yes
- Native Minecraft screenshot obtained: yes
- `Buffer already closed`: 0
- `ConcurrentModificationException`: 0
- `Duplicate entity UUID`: 0
- Crash: no

Artifacts:

- Compile log: `compile-phase5.11-26.1.txt`
- Runtime log: `runclient-phase5.11-visual.txt`
- Screenshot: `run/screenshots/phase5.11-framebuffer-quad.png`

## Visual Result

The framebuffer texture is still visible, but the quad is not correctly framed
inside the portal rectangle. Compared with Phase 5.10:

- the framebuffer V orientation is no longer the only obvious issue;
- the image no longer appears as the same small centered rectangle;
- the textured quad is projected as a large slanted strip near the screen edge;
- the outside of the capture is still black because the screenshot is captured
  immediately after the custom blit, before a fully composed final frame;
- clipping, depth and stencil remain intentionally incomplete.

The logged framebuffer and window sizes are identical in the validation run:
`fb=854x480 window=854x480`. This rules out secondary framebuffer size mismatch
as the primary cause of the remaining bad cadrage.

## Current Blocker

The remaining cadrage issue comes from the draw path, not from framebuffer size
or a closed GPU buffer.

`IPRenderPipelines.drawTexturedMesh(...)` creates an immediate `RenderPass`.
Even when called from `PortalEntityRenderer` with coordinates transformed from
the entity `PoseStack`, that immediate pass is not submitted as real vanilla
entity geometry through `SubmitNodeCollector`. It therefore does not participate
in the same render graph, ordering and matrix state as the cyan frame.

`RenderTypes` in 26.1 exposes texture-based entity render types only for
resource-location textures. It does not directly expose a render type that can
sample an arbitrary `GpuTextureView` from the secondary framebuffer. A correct
next micro-step needs either:

- a render-graph compatible custom submit node for the framebuffer texture; or
- a small 26.1 `RenderType`/pipeline bridge that can be submitted through
  `SubmitNodeCollector` while binding the secondary framebuffer texture.

Until that exists, the cyan fallback remains the reliable visual reference and
the minimal recursive texture remains diagnostic-only.

## Status

Phase 5.11 is complete as a controlled mapping audit. The runtime path remains
stable, the UV flip and diagnostics are in place, and the next blocker is now
precisely identified: the framebuffer quad must move from an immediate
`RenderPass` into the vanilla entity render graph before cadrage can be fixed
properly.
