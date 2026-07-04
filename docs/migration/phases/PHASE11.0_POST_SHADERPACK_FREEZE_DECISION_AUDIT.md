# Phase 11.0 - Post Shaderpack Freeze Decision Audit

Date: 2026-06-30

## Scope

Choose the next route after the Phase 10.x shaderpack rendering freeze without modifying runtime code.

No renderer code, pipeline, shader, render target, mixin, DimLib, Sodium shader path, or Iris legacy renderer was changed in this phase.

## State Verification

Checked:

- `git status --short`
- `git log --oneline -20`
- `run/config/iris.properties`
- `IMM_PTL_*` environment flags
- active Java `runClient` processes

Confirmed:

- Phase 10.8 freeze commit is present:
  - `5c4b7dcf Freeze Phase 10 shaderpack rendering baseline`
- `run/config/iris.properties` is restored to:
  - `shaderPack=MakeUp-UltraFast-9.5c.zip`
- no `IMM_PTL_*` flag is active in the Codex shell
- no Phase 10.x or Phase 11.x `runClient` process is active

The worktree remains noisy with old logs, copies, screenshots, and generated artifacts, but no relevant renderer source file is modified by Phase 11.0.

## Reports Reviewed

- `PHASE10.8_SHADERPACK_BASELINE_FREEZE.md`
- `PHASE10.7_ALPHA_TEXTURE_EXPERIMENTAL_STABILIZATION.md`
- `PHASE10.6_ALPHA_TEXTURE_RUNTIME_VALIDATION.md`
- `PHASE10.4_RENDER_GRAPH_STENCIL_LIKE_AUDIT.md`
- `PHASE10.3_NO_DEPTH_CPU_GEOMETRIC_CLIP_PROTOTYPE.md`
- `PHASE10.2_COMPLEMENTARY_ORDER_VISUAL_VALIDATION.md`

## Current Baseline

- MakeUp `default / depth-masked-equal`: stable baseline.
- Complementary `default`: intermittent or too dark.
- Complementary `lequal`: visible but intermittent.
- Complementary `no_depth`: robust manual fallback, degraded occlusion.
- Complementary `no_depth + alpha_texture`: experimental opt-in, reduces some internal cyan artifacts, but is not a true silhouette mask.

Still intentionally inactive:

- `RendererUsingStencil`
- `glStencil*`
- global shader clipping
- `MixinRenderSystem_Clipping`
- Sodium shader mixins
- Iris legacy renderers
- DimLib
- AlternateDimensions

## Route A - Real Separate Mask Texture + Dedicated Composition

### Likely Files

- `RendererUsingFrameBuffer.java`
- `IPRenderPipelines.java`
- `SecondaryFrameBuffer.java`
- `MyRenderHelper.java`
- `PortalEntityRenderer.java`
- `PortalRenderState` / portal render state plumbing if mask metadata is added
- shader assets under `assets/immersive_portals/shaders/core/` if a multi-sampler composition shader is required
- Iris reflective pipeline mapping code in `IPRenderPipelines`

### Required Work

- allocate a separate mask texture or render target;
- render the portal silhouette into that mask;
- compose framebuffer color with the mask;
- decide whether composition happens into an intermediate texture or directly in the portal quad path;
- add a safe fallback if the mask target or pipeline is unavailable;
- ensure no per-frame texture registration leaks;
- ensure `GpuBuffer`/`MeshData` ownership remains correct;
- define Iris mappings for any new composition pipeline.

### Compatibility Assessment

Vanilla:

- likely feasible;
- risk is moderate because the render graph path is already understood enough for the framebuffer quad.

Sodium:

- likely feasible if limited to the portal entity submit path and not the chunk renderer;
- risk is moderate because ordering and custom render types can differ with Sodium.

Iris:

- highest risk;
- shaderpack mappings are fragile;
- multi-sampler or custom composition may need a new Iris fallback mapping;
- MakeUp default must not regress;
- Complementary could still remain overbright or postprocessed even after masking.

### Risks

- medium to high implementation complexity;
- medium shaderpack risk;
- medium GPU resource leak risk if mask targets are recreated incorrectly;
- medium performance risk from extra target/passes per visible portal;
- high risk of spending several phases for a result that still does not solve true destination-world clipping;
- possible regression of the stable MakeUp default baseline.

### Tests Needed

- vanilla compile/processResources;
- Sodium compile-only;
- Iris compile-only;
- MakeUp default portal visual test;
- Complementary `no_depth` reference;
- Complementary `no_depth + mask texture`;
- invalid/fallback mode test;
- long run facing portal for leaks/flicker;
- traversal regression;
- reload world regression.

### Verdict

Route A is technically plausible but too large for the immediate next step. It should be opened only after a micro-design phase that defines exact target ownership, pipeline count, Iris mapping strategy, and fallback behavior.

## Route B - Other Work Outside Shaderpack Rendering

### 1. User Documentation for Shaderpack Fallbacks

Interest:

- high user value;
- turns the current experimental matrix into usable instructions.

Risk:

- very low.

Dependencies:

- Phase 10.x reports and current flags.

Tests:

- documentation review only.

Success probability:

- very high.

Priority:

- high.

### 2. Cleanup of Dev Flags

Interest:

- high for maintainability;
- reduces accidental state and confusing test paths.

Risk:

- low to medium if behavior is only documented/centralized;
- higher if flags are removed or renamed.

Dependencies:

- list of `IMM_PTL_*` flags from Phases 9-10.

Tests:

- compile if code changes;
- no runtime needed if documentation-only.

Success probability:

- high if scoped to documentation/inventory first.

Priority:

- high, but avoid removing flags before a release decision.

### 3. Traversal Nether / End

Interest:

- high gameplay value;
- validates portals beyond same-dimension Overworld tests.

Risk:

- medium;
- may expose dimension sync, chunk loading, and lighting issues.

Dependencies:

- current minimal renderer and teleportation path.

Tests:

- Overworld -> Nether;
- Nether -> Overworld;
- save/reload;
- traversal with vanilla, Sodium, Iris no shaderpack, Iris MakeUp.

Success probability:

- medium to high.

Priority:

- high if targeting playable alpha behavior.

### 4. Long-Term Save / Reload Stability

Interest:

- high;
- protects against duplicated portals and stale sync state.

Risk:

- medium.

Dependencies:

- portal storage, entity UUID behavior, client sync.

Tests:

- create portal;
- save/quit/reload repeatedly;
- verify no duplicate UUID;
- verify one client portal;
- verify traversal still works.

Success probability:

- high.

Priority:

- high.

### 5. Wider Sodium Runtime Compatibility

Interest:

- medium to high;
- Sodium is a common runtime dependency.

Risk:

- medium;
- current non-shader path is stable, but deeper Sodium paths may touch chunk rendering.

Dependencies:

- Sodium non-shader baseline.

Tests:

- menu/world/portal/traversal;
- chunk visibility around portal;
- long camera movement.

Success probability:

- medium.

Priority:

- medium after save/reload and Nether/End.

### 6. Commands and User-Facing Tools

Interest:

- medium;
- useful for testing and alpha users.

Risk:

- low.

Dependencies:

- command registration and permissions audit from earlier phases.

Tests:

- command availability in singleplayer survival/creative;
- permission handling;
- command output clarity.

Success probability:

- high.

Priority:

- medium.

### 7. Packaging Alpha

Interest:

- high if the goal is distribution/testing.

Risk:

- low to medium;
- may reveal metadata, dependency, and profile issues.

Dependencies:

- documentation, Gradle profiles, version metadata.

Tests:

- build artifact;
- run clean client with artifact;
- dependency matrix.

Success probability:

- high.

Priority:

- medium to high after gameplay smoke tests.

### 8. DimLib / AlternateDimensions

Interest:

- high for full Immersive Portals feature parity.

Risk:

- high;
- it was intentionally isolated to keep vanilla/Sodium/Iris baselines stable.

Dependencies:

- dimension registry work;
- dynamic dimension sync;
- ClientWorldLoader/ServerTeleportationManager integration.

Tests:

- compile profile;
- runtime menu/world;
- dynamic dimension create/remove;
- global portals.

Success probability:

- medium only with a dedicated staged plan.

Priority:

- low for immediate next phase; should remain isolated until a safe plan is written.

## Recommendation

Recommendation: Route B now, Route A later after preparation.

The shaderpack renderer is now stable enough to freeze. Opening a real mask texture path immediately would touch render targets, composition pipelines, Iris mappings, and GPU resource ownership. That is the right kind of work for a dedicated Phase 11.x design/prototype, but not the best immediate continuation after a successful freeze.

Recommended Phase 11.1:

- document user-facing shaderpack fallback settings and stabilize the dev-flag matrix;
- keep it documentation/inventory-first unless a small bug is discovered;
- do not change renderer behavior.

Recommended Phase 11.2 or later:

- Nether/End traversal regression;
- save/reload long-run stability;
- then consider a formal design phase for a real mask texture if shaderpack rendering remains the top priority.

## Decision

Do not start Route A implementation immediately.

Proceed with Route B first:

1. user documentation for shaderpack fallback modes;
2. dev flag inventory/cleanup plan;
3. gameplay/runtime regressions outside shaderpack rendering.

Route A should return as a later explicit design phase, not as an opportunistic patch.
