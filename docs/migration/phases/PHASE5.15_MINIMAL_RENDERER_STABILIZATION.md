# Phase 5.15 - Minimal Vanilla Renderer Stabilization

Date: 2026-06-20

## Scope

This phase consolidated the vanilla minimal recursive renderer built during
Phases 5.0 through 5.14. It did not add shader clipping, advanced stencil,
multiple recursion, Sodium, Iris, DimLib or dynamic alternate dimensions.

## Files Audited

- `RendererUsingFrameBuffer.java`
- `PortalEntityRenderer.java`
- `PortalRenderer.java`
- `MyRenderHelper.java`
- `MyGameRenderer.java`
- `IPRenderPipelines.java`
- `FrontClipping.java`
- `MixinGameRenderer.java`
- `MixinCamera.java`
- `MixinLevelRenderer.java`
- `MixinParticleEngine.java`
- `IPModEntryClient.java`
- `ClientDebugCommand.java`
- `PortalDebugCommands.java`
- `build.gradle`
- `src/main/resources/imm_ptl.mixins.json`

## Consolidated Runtime Paths

### Vanilla minimal renderer

- `GameRenderer.renderLevel` HEAD renders one same-dimension destination view
  into the secondary framebuffer.
- `PortalEntityRenderer.submit` submits the framebuffer rectangle through
  `SubmitNodeCollector`.
- The depth-only rectangle and the `EQUAL` textured pass remain enabled when a
  main depth attachment is available.
- The Phase 5.13 CPU portal-entity prefilter remains scoped to destination
  framebuffer rendering.
- No world rendering is invoked from entity submission.

### Cyan fallback

The cyan portal frame is always submitted independently. Missing camera,
renderer facade, lightmap, framebuffer texture, depth-mask pipeline or submit
node support returns to this frame without crashing.

### Legacy/advanced path

The immediate framebuffer draw helper remains only for legacy compatibility
renderers. It is explicitly documented and is not used by the vanilla minimal
path. Advanced shader, clipping and stencil hooks remain isolated.

### Development tools

Server development commands remain guarded by
`FabricLoader.isDevelopmentEnvironment()`. Automatic tests are also registered
only in a development environment and require explicit environment flags.

## Development Flags

- `IMM_PTL_AUTO_VISIBLE_TEST_PORTAL=true`
  - creates the visible test portal after the client world becomes ready;
  - development environment only.
- `IMM_PTL_AUTO_MINIMAL_TRAVERSAL_TEST=true`
  - new deterministic stabilization driver;
  - invokes the existing client traversal command after the visible portal has
    been created;
  - development environment only.
- `IMM_PTL_CAPTURE_MINIMAL_RECURSIVE_PORTAL=true`
  - enables one screenshot per process;
  - otherwise performs no capture work.
- `IMM_PTL_MINIMAL_RECURSIVE_PORTAL_SCREENSHOT=<filename>`
  - controls only the basename;
  - directory components are stripped;
  - invalid paths fall back to `imm_ptl-minimal-recursive-portal.png`.

The opt-in capture waits three seconds after its first eligible frame. This
allows the visible test portal and camera placement to settle before capture.

## Logging Audit

- Renderer facade availability: logged once.
- First queued portal: logged once.
- First recursive render: logged once instead of up to ten times.
- Texture, depth-mask and SubmitNode status: logged once.
- CPU clipping attempt, plan, application and first culled portal: logged once.
- Fallthrough reasons: one-shot or `LimitedLogger` guarded.
- No unguarded per-frame renderer log remains in the minimal path.

## GPU Resource Audit

- `SecondaryFrameBuffer` owns one `TextureTarget` and resizes it only when the
  main target size changes.
- `FramebufferTextureAlias` is registered once in `TextureManager`.
- The alias never closes framebuffer-owned `GpuTexture` or `GpuTextureView`
  resources; `close()` only clears references.
- Immediate Mojang `GpuBuffer` objects returned by
  `uploadImmediateVertexBuffer` and `uploadImmediateIndexBuffer` are not closed
  by Immersive Portals.
- Immediate compatibility draws close their caller-owned `MeshData` exactly
  once.
- Submit-node geometry allocates no persistent texture or render type per
  frame; pipelines and render types are cached.

## Traversal Test Fix

The existing development command `test_minimal_portal_traversal` placed the
player behind the portal and moved toward the positive portal normal. The
rectangular portal ray trace accepts only `local z > 0` to `local z < 0`.

The test driver was corrected to start in front of the portal and move opposite
the normal. This changes only the development test. The final run records two
`Client Teleported Statically` events.

## Validation

### Build

- `compileJava`: BUILD SUCCESSFUL
- `processResources`: BUILD SUCCESSFUL
- `git diff --check`: no whitespace error

### Menu run

- Minecraft window/menu reached: yes
- Development auto command without flags: absent
- Capture without flag: absent
- Normal close: BUILD SUCCESSFUL

### Phase50Test render and traversal run

- Portal and destination framebuffer rendered: yes
- Dynamic framebuffer texture available: yes
- `SubmitNodeCollector` textured quad submitted: yes
- Depth mask applied: yes
- Native screenshot obtained: yes
- Visible test portal created: yes
- Deterministic traversal command executed: yes
- Overworld to Overworld traversal: yes
- `Client Teleported Statically`: observed twice
- Several seconds of portal rendering before and after traversal: yes
- Normal close/save: BUILD SUCCESSFUL

### Reload run

- Phase50Test reopened without auto-creation/capture flags: yes
- Previously saved portal collected by the client renderer: yes
- Normal close: BUILD SUCCESSFUL

### Regression scan

- `Buffer already closed`: 0
- `ConcurrentModificationException`: 0
- `Duplicate entity UUID`: 0
- game crash/reported exception: 0
- crash on close: 0

The test does not provide a pixel-perfect automated camera sweep from out of
view back into view. It does cover menu, world load, automatic camera placement,
sustained visible rendering, traversal, save/close and a separate reload.

## Compatibility Isolation

The vanilla build profile still excludes Sodium and Iris compatibility sources,
`MixinRenderSystem_Clipping`, `MixinRenderSystem_Fog` and
`MixinGameRenderer_Shaders`. No DimLib or AlternateDimensions work was started.

## Artifacts

- `compile-phase5.15-26.1.txt`
- `runclient-phase5.15-menu.txt`
- `runclient-phase5.15-visual.txt`
- `run/screenshots/phase5.15-stable-minimal-renderer.png`
- `git-diff-phase5.15.txt`

## Stable Minimal Status

The vanilla minimal renderer is now consolidated and reproducible. It provides
one same-dimension destination render, a render-graph framebuffer quad, an
optional depth mask, CPU portal filtering and a cyan fallback. Full scene
clipping, advanced stencil, multiple recursion and compatibility renderers
remain explicit future work.
