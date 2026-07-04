# Phase 5.14 - Minimal Stencil and Depth Mask Audit

Date: 2026-06-19

## Scope

This phase audited Minecraft 26.1 stencil/depth support and prototyped the
smallest depth mask compatible with the Phase 5.12 `SubmitNodeCollector` bridge.
No advanced renderer, shader mixin, Sodium, Iris or DimLib integration was
enabled.

## Files Inspected

- `src/main/java/qouteall/imm_ptl/core/render/MyRenderHelper.java`
- `src/main/java/qouteall/imm_ptl/core/render/PortalEntityRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/render/renderer/RendererUsingFrameBuffer.java`
- `src/main/java/qouteall/imm_ptl/core/render/renderer/RendererUsingStencil.java`
- `src/main/java/qouteall/imm_ptl/core/render/renderer/PortalRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/render/pipeline/IPRenderPipelines.java`
- `src/main/java/qouteall/imm_ptl/core/render/SecondaryFrameBuffer.java`
- Minecraft 26.1 `RenderPipeline`, `DepthStencilState`, `ColorTargetState`,
  `CompareOp`, `RenderTarget`, `SubmitNodeCollector` and `SubmitNodeStorage`.

## Audit Result

### Depth

Both targets expose a depth texture view in the controlled run:

- main target depth: available;
- secondary framebuffer depth: available.

Minecraft 26.1 `DepthStencilState` exposes:

- depth comparison operation;
- depth write enable;
- depth bias scale and constant.

This is sufficient for a two-pass depth mask.

### Stencil

The mapped Minecraft 26.1 `DepthStencilState` contains no stencil comparison,
reference, write mask or stencil operation. `RenderTarget` also exposes a depth
attachment but no stencil attachment API usable by `RenderPipeline`.

Usable stencil support through the public render-pipeline path is therefore
unavailable in this profile. The old renderer's raw OpenGL stencil manipulation
cannot be safely mixed into the deferred render graph without restoring the
advanced framebuffer/stencil system, which is forbidden in this phase.

## Prototype

Two minimal pipelines were added:

1. `PORTAL_DEPTH_MASK`
   - portal rectangle in `POSITION_COLOR` format;
   - color writes disabled with `ColorTargetState.WRITE_NONE`;
   - depth test `LESS_THAN_OR_EQUAL`;
   - depth writes enabled.
2. `DRAW_FRAMEBUFFER_IN_AREA_DEPTH_MASKED`
   - existing framebuffer texture and `POSITION_TEX` geometry;
   - depth test `EQUAL`;
   - depth writes disabled.

The mask is submitted at order 0 and the textured quad at order 1. Both use the
same portal-local corners and `PoseStack`, so their generated depth values
match. If either pipeline or the main depth attachment is unavailable, the code
falls back to the unmasked Phase 5.12 framebuffer `RenderType`.

## Runtime Logs

The controlled run confirms:

- main depth available: true;
- secondary depth available: true;
- usable stencil: false;
- minimal depth mask attempted: true;
- minimal depth mask applied: true;
- framebuffer texture available: true;
- textured quad submitted via `SubmitNodeCollector`: true;
- CPU portal clipping from Phase 5.13 remains active;
- native screenshot captured successfully.

## Validation

- `compileJava`: BUILD SUCCESSFUL
- `processResources`: BUILD SUCCESSFUL
- `runClient --quickPlaySingleplayer=Phase50Test`: BUILD SUCCESSFUL
- Destination framebuffer visible: yes
- `SubmitNodeCollector` bridge retained: yes
- Depth mask attempted and applied: yes
- Stencil path available: no, exact API limitation documented
- Native screenshot obtained: yes
- `Buffer already closed`: 0
- `ConcurrentModificationException`: 0
- `Duplicate entity UUID`: 0
- Crash: no
- Cyan fallback retained: yes

Artifacts:

- `compile-phase5.14-26.1.txt`
- `runclient-phase5.14-visual.txt`
- `run/screenshots/phase5.14-minimal-mask.png`

## Visual Result

The framebuffer remains aligned with the portal rectangle and the two-pass mask
does not introduce a crash or GPU buffer error. Occlusion against the existing
main depth buffer is now explicit and deterministic.

The visible reduction is modest. A rectangular depth mask cannot remove blocks,
entities or nested portal content that was already rendered into the secondary
texture. It masks the final quad, not the destination scene itself. The nested
cyan frames therefore remain the dominant artifact in Phase50Test.

## Remaining Limit

Further visual clipping requires one of the larger mechanisms intentionally
excluded from this phase:

- a real stencil attachment and stencil operations integrated into the 26.1
  render graph; or
- a dedicated destination-world shader pipeline consuming a clipping plane.

The prototype is kept because it is small, safe, has an automatic fallback and
provides a useful depth foundation for later work. The old advanced stencil
renderer was not re-enabled.

## Status

Phase 5.14 is complete. The depth-mask prototype is compiled and runtime
validated, while the stencil limitation and limited visual scope are precisely
documented.
