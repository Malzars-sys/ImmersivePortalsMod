# Phase 5.12 - SubmitNode Framebuffer Quad

Date: 2026-06-19

## Scope

This phase replaced the immediate framebuffer portal quad with a minimal
Minecraft 26.1 render-graph submission. Sodium, Iris, DimLib, shader mixins,
fog mixins, clipping mixins, advanced stencil and advanced framebuffer
rendering remain disabled.

## Files Inspected

- `src/main/java/qouteall/imm_ptl/core/render/PortalEntityRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/render/MyRenderHelper.java`
- `src/main/java/qouteall/imm_ptl/core/render/renderer/PortalRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/render/renderer/RendererUsingFrameBuffer.java`
- `src/main/java/qouteall/imm_ptl/core/render/pipeline/IPRenderPipelines.java`
- `src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinGameRenderer.java`
- Minecraft 26.1 `RenderType`, `RenderSetup`, `AbstractTexture`,
  `TextureManager`, `SubmitNodeCollector` and `OrderedSubmitNodeCollector`
  through the local mapped Minecraft jar.

## Files Modified

- `src/main/java/qouteall/imm_ptl/core/render/PortalEntityRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/render/MyRenderHelper.java`
- `src/main/java/qouteall/imm_ptl/core/render/renderer/PortalRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/render/renderer/RendererUsingFrameBuffer.java`
- `src/main/java/qouteall/imm_ptl/core/render/pipeline/IPRenderPipelines.java`
- `src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinGameRenderer.java`
- `MIGRATION_PLAN_26.1.md`

## Implementation

Minecraft 26.1 `RenderSetup` cannot bind a `GpuTextureView` directly. Its
texture binding resolves an `Identifier` through `TextureManager`.

The minimal bridge therefore uses three small pieces:

1. `FramebufferTextureAlias` extends `AbstractTexture` and exposes the current
   color texture and texture view of the secondary framebuffer.
2. The alias is registered under
   `imm_ptl:minimal_portal_framebuffer`. Its `close()` method only drops the
   references and never closes the framebuffer-owned GPU resources.
3. A lazy `RenderType` uses the existing
   `DRAW_FRAMEBUFFER_IN_AREA` pipeline and resolves `Sampler0` through that
   texture identifier.

`PortalEntityRenderer.submit` now passes its `SubmitNodeCollector` to the
minimal framebuffer renderer. `MyRenderHelper.submitPortalAreaWithFramebuffer`
submits the six `POSITION_TEX` vertices in portal-local coordinates using the
same `PoseStack` as the cyan frame.

The two-stage architecture is preserved:

- `GameRenderer.renderLevel` HEAD renders the destination into the secondary
  framebuffer without entity-list reentrance.
- `PortalEntityRenderer.submit` submits only the textured quad into the vanilla
  render graph.
- It never calls `GameRenderer.renderLevel` from entity submission.

The opt-in screenshot is captured at the TAIL of the outer `renderLevel`, after
the submitted geometry has been composed.

## Runtime Logs

The controlled run confirms:

- `Queued minimal recursive portal from PortalEntityRenderer`
- `Rendering minimal recursive portal from GameRenderer renderLevel hook`
- `Minimal recursive portal framebuffer texture available: true`
- `Minimal recursive portal textured quad submitted via SubmitNodeCollector: true`
- `Captured minimal recursive portal screenshot: Saved screenshot as phase5.12-submit-node-quad.png`

## Validation

- `compileJava`: BUILD SUCCESSFUL
- `processResources`: BUILD SUCCESSFUL
- `runClient --quickPlaySingleplayer=Phase50Test`: BUILD SUCCESSFUL
- Portal collected: yes
- Destination framebuffer rendered: yes
- Dynamic framebuffer texture available: yes, `854x480`
- Textured quad submitted through `SubmitNodeCollector`: yes
- Native Minecraft screenshot obtained: yes
- `Buffer already closed`: 0
- `ConcurrentModificationException`: 0
- `Duplicate entity UUID`: 0
- Crash: no
- Cyan fallback frame retained: yes

Artifacts:

- `compile-phase5.12-26.1.txt`
- `runclient-phase5.12-visual.txt`
- `run/screenshots/phase5.12-submit-node-quad.png`

## Visual Result

The Phase 5.11 slanted framebuffer strip on the screen edge is gone. The
destination texture is now placed with the portal geometry in the center of
the view and follows the portal-local rectangle.

The capture still shows expected minimal-rendering artifacts:

- cyan fallback/debug frames remain visible;
- several nested frames are visible because Phase50Test contains older test
  portals and recursive content is limited rather than clipped;
- no advanced stencil mask;
- no front clipping plane;
- depth composition is not final;
- vanilla fog fallback remains active;
- one recursive destination render only.

These artifacts are outside the Phase 5.12 bridge scope. The major placement
failure is fixed: the framebuffer quad now participates in the vanilla entity
render graph instead of using an immediate `RenderPass`.

## Status

Phase 5.12 is complete. The minimal framebuffer-to-`SubmitNodeCollector` bridge
is compiled and validated at runtime, the portal texture is substantially
better placed, and all restricted integrations remain untouched.
