# Phase 5.13 - Minimal Vanilla Front Clipping

Date: 2026-06-19

## Scope

This phase investigated and added the smallest safe vanilla clipping fallback
for the minimal recursive portal renderer. Sodium, Iris, DimLib, shader mixins,
advanced stencil, advanced clipping and the complete framebuffer renderer
remain disabled.

## Files Inspected

- `src/main/java/qouteall/imm_ptl/core/render/FrontClipping.java`
- `src/main/java/qouteall/imm_ptl/core/render/MyGameRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/render/PortalEntityRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/render/renderer/PortalRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/render/renderer/RendererUsingFrameBuffer.java`
- `src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinRenderSystem_Clipping.java`
- `src/main/java/qouteall/imm_ptl/core/render/pipeline/IPRenderPipelines.java`
- `src/main/java/qouteall/imm_ptl/core/render/context_management/PortalRendering.java`
- `src/main/java/qouteall/imm_ptl/core/portal/Portal.java`
- `src/main/java/qouteall/imm_ptl/core/portal/shape/RectangularPortalShape.java`
- Minecraft 26.1 render pipeline and render type APIs.

## Existing Clipping State

The portal already exposes its destination-side clipping plane through
`Portal.getInnerClipping()`. `PortalRendering.getActiveClippingPlane()` also
transforms or inherits this plane for the active portal layer.

The old `FrontClipping` path calculated the plane equation and enabled
`GL_CLIP_PLANE0`. Its effective clipping nevertheless depended on the removed
`iportal_ClippingEquation` uniform being injected into every active shader by
`MixinRenderSystem_Clipping` and shader transformations.

Minecraft 26.1 uses core render pipelines and vanilla shaders that do not write
`gl_ClipDistance` and do not expose a global clipping-plane uniform. Enabling
the legacy OpenGL clip-plane flag alone cannot clip arbitrary vanilla terrain,
entities or particles. A true global GPU plane therefore requires a dedicated
pipeline/shader migration and is intentionally outside this micro-phase.

## Minimal Fallback

`FrontClipping` now provides a scoped CPU portal-entity prefilter:

1. After the destination portal layer is pushed, the active inner clipping
   plane is calculated.
2. The plane remains active only while the destination world is rendered into
   the secondary framebuffer.
3. `PortalEntityRenderer.submit` checks the portal center and four world-space
   corners.
4. A portal entity is skipped only when every tested point is behind the active
   plane.
5. The CPU plane is cleared in `finally` before the portal layer is popped.

The adjustment keeps geometry very close to the plane on the visible side to
avoid unstable edge culling.

This fallback intentionally does not reject blocks, ordinary entities,
particles or weather. It removes only wholly clipped portal entities, which
reduces some absurd nested cyan frames without pretending to provide full
scene clipping.

## Runtime Logs

The controlled run confirms:

- `Minimal destination clipping attempted: true`
- `Minimal destination clipping plane calculated: true`
- `Minimal destination clipping applied: CPU portal-entity prefilter only`
- `Minimal destination clipping culled portal entity 76 behind the active plane`
- framebuffer texture available: true
- textured quad submitted through `SubmitNodeCollector`: true
- native screenshot captured successfully

## Validation

- `compileJava`: BUILD SUCCESSFUL
- `processResources`: BUILD SUCCESSFUL
- `runClient --quickPlaySingleplayer=Phase50Test`: BUILD SUCCESSFUL
- Portal collected: yes
- Destination framebuffer rendered: yes
- Dynamic framebuffer texture available: yes
- `SubmitNodeCollector` textured quad retained: yes
- Minimal clipping attempted: yes
- Active clipping plane calculated: yes
- Portal entity actually culled by the CPU plane: yes
- Native screenshot obtained: yes
- `Buffer already closed`: 0
- `ConcurrentModificationException`: 0
- `Duplicate entity UUID`: 0
- Crash: no
- Cyan fallback retained: yes

Artifacts:

- `compile-phase5.13-26.1.txt`
- `runclient-phase5.13-visual.txt`
- `run/screenshots/phase5.13-minimal-clipping.png`

## Visual Result

The Phase 5.12 framebuffer placement remains stable. One portal entity behind
the active plane was removed in the validation run, so the CPU fallback is
active and observable.

The visual reduction is intentionally modest. Several nested cyan frames and
unclipped destination blocks remain because they lie on the retained side or
are not portal entities. The screenshot therefore confirms both the useful
effect and the exact limit of this fallback.

## Remaining Technical Limit

Full destination clipping cannot be implemented with a global fixed-function
OpenGL switch in Minecraft 26.1. It needs one of the following future steps:

- a dedicated portal world pipeline whose shaders consume a clipping-plane
  uniform and discard the clipped half-space; or
- a stencil/depth mask around the portal view.

Both options are larger rendering migrations and remain prohibited in this
phase. `MixinRenderSystem_Clipping` was not re-enabled.

## Status

Phase 5.13 is complete with a safe CPU fallback and a precisely documented GPU
limit. The framebuffer bridge remains stable and no restricted integration was
touched.
