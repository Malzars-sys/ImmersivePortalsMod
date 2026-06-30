# Phase 10.8 - Shaderpack Baseline Freeze

Date: 2026-06-30

## Scope

Freeze the Phase 10.x shaderpack rendering baseline before opening a riskier rendering path.

No runtime code was modified in this phase.

## Current Commit Before Freeze

Current head before the Phase 10.8 freeze commit:

- `19fed37f` - Document alpha texture experimental status

Relevant Phase 10.x commits present:

- `404c5000` - Stabilize Iris shaderpack fallback rendering baseline
- `2fddda41` - Add opt-in framebuffer order diagnostics
- `e196758b` - Add opt-in no-depth portal geometry clip diagnostics
- `3f02f96f` - Add opt-in alpha texture framebuffer mask prototype
- `1ab3ae88` - Document alpha texture mask runtime validation
- `19fed37f` - Document alpha texture experimental status

## Final Shaderpack Matrix

### MakeUp-UltraFast-9.5c.zip

- `default` / `depth-masked-equal`: stable shaderpack baseline.

### ComplementaryReimagined_r5.8.1.zip

- `default`: intermittent or too dark.
- `lequal`: visible but intermittent.
- `no_depth`: robust manual shaderpack-safe fallback, with degraded occlusion.
- `no_depth + alpha_texture`: experimental opt-in; reduces some internal cyan frame/diagonal artifacts, but is not a true silhouette mask.

## Modes Kept Available

`IMM_PTL_FRAMEBUFFER_DEPTH_MODE`:

- `default`
- `no_depth`
- `lequal`
- `always`

`IMM_PTL_FRAMEBUFFER_ORDER_MODE`:

- `default`
- `mask_first_explicit`
- `quad_first`
- `no_mask_reference`

`IMM_PTL_PORTAL_CPU_CLIP_MODE`:

- `off`
- `portal_quad_only`
- `conservative_plane`
- `debug_bounds`

`IMM_PTL_FRAMEBUFFER_MASK_MODE`:

- `off`
- `alpha_texture`

## Modes Not Promoted

The following modes stay explicit diagnostics or manual fallbacks:

- `quad_first`
- `no_mask_reference`
- `portal_quad_only`
- `alpha_texture`
- `lequal`

Important decisions:

- `default` remains unchanged.
- `no_depth` is not promoted automatically.
- `alpha_texture` is not promoted automatically.
- no shaderpack-name based automatic mode switching is introduced.

## Environment and Process State

Verified:

- `run/config/iris.properties`: `shaderPack=MakeUp-UltraFast-9.5c.zip`
- no `IMM_PTL_*` flags active in the Codex shell
- no Phase 10.x `runClient` process active

## Files Included in Freeze

The freeze commit should include:

- `MIGRATION_PLAN_26.1.md`
- `PHASE10.2_COMPLEMENTARY_ORDER_VISUAL_VALIDATION.md`
- `PHASE10.8_SHADERPACK_BASELINE_FREEZE.md`

Earlier Phase 10.x reports are already tracked:

- `PHASE10.0_ADVANCED_RENDERING_CLIPPING_OCCLUSION_AUDIT.md`
- `PHASE10.1_FRAMEBUFFER_ORDER_DEPTH_MICRO_AUDIT.md`
- `PHASE10.3_NO_DEPTH_CPU_GEOMETRIC_CLIP_PROTOTYPE.md`
- `PHASE10.4_RENDER_GRAPH_STENCIL_LIKE_AUDIT.md`
- `PHASE10.5_ALPHA_TEXTURE_MASK_PROTOTYPE.md`
- `PHASE10.6_ALPHA_TEXTURE_RUNTIME_VALIDATION.md`
- `PHASE10.7_ALPHA_TEXTURE_EXPERIMENTAL_STABILIZATION.md`

## Files Excluded

Not included:

- `run/`
- `build/`
- shaderpack `.zip` files
- screenshots/captures
- runtime logs
- compile logs
- copied temporary Markdown files
- historical generated artifacts

## Remaining Risks

- Complementary default remains intermittent or too dark.
- `lequal` remains intermittent.
- `no_depth` remains robust but has degraded occlusion.
- `alpha_texture` is not a real mask; it only changes the composition path.
- General clipping remains incomplete.
- No public RenderPipeline stencil path has been found.
- A true silhouette mask would need a separate mask texture and dedicated composition pass.

Still not activated:

- `RendererUsingStencil`
- `glStencil*`
- global shader clipping
- `MixinRenderSystem_Clipping`
- Sodium shader mixins
- legacy Iris renderers
- DimLib
- AlternateDimensions

## Validation

No compilation was rerun because Phase 10.8 changes documentation only and no runtime code has changed since the already validated Phase 10.5/10.7 baseline.

## Recommended Next Phase

Two reasonable paths remain:

1. Phase 11.0: build a real separate portal silhouette mask texture plus a dedicated composition pass.
2. Freeze shaderpack rendering for now and move to another migration family outside shaderpack rendering.

The renderer baseline is stable enough to freeze before either path.
