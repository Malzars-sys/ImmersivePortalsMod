# Phase 10.7 - Alpha Texture Experimental Stabilization

Date: 2026-06-30

## Scope

Stabilize the documentation and decision state for `IMM_PTL_FRAMEBUFFER_MASK_MODE=alpha_texture` after the clean-world Phase 10.6 runtime validation.

No renderer code was changed in this phase.

## Repository State

Checked commands:

- `git status --short`
- `git log --oneline -15`

Relevant commits present:

- `404c5000` - Stabilize Iris shaderpack fallback rendering baseline
- `2fddda41` - Add opt-in framebuffer order diagnostics
- `e196758b` - Add opt-in no-depth portal geometry clip diagnostics
- `3f02f96f` - Add opt-in alpha texture framebuffer mask prototype
- `1ab3ae88` - Document alpha texture mask runtime validation

The working tree remains noisy with old generated logs, copies, and deleted historical reports. No renderer source changes were made for Phase 10.7.

## Environment State

Verified:

- `run/config/iris.properties`: `shaderPack=MakeUp-UltraFast-9.5c.zip`
- no `IMM_PTL_*` environment flags active in the Codex shell
- no Phase 10.x `runClient` process active

## Current Runtime Matrix

### MakeUp-UltraFast-9.5c.zip

- `default` / `depth-masked-equal`: stable baseline.
- This remains the default shaderpack baseline.

### ComplementaryReimagined_r5.8.1.zip

- `default`: intermittent or too dark under this portal framebuffer path.
- `lequal`: can be visible, but remains intermittent.
- `no_depth`: robust manual shaderpack-safe fallback, with degraded occlusion.
- `no_depth + alpha_texture`: experimental opt-in mode; reduces some internal cyan frame/diagonal artifacts compared with plain `no_depth`, but is not a true portal silhouette mask.

## Mode Decisions

### Keep Available

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

### Do Not Promote Automatically

- `quad_first`
- `no_mask_reference`
- `portal_quad_only`
- `alpha_texture`
- `lequal`

## alpha_texture Status

`alpha_texture` is retained as an experimental opt-in diagnostic/composition mode.

Properties:

- active only on the non-depth-masked framebuffer path;
- selected explicitly with `IMM_PTL_FRAMEBUFFER_MASK_MODE=alpha_texture`;
- does not change the global default;
- does not replace `no_depth`;
- does not replace `default`;
- must not be enabled automatically based on shaderpack name.

Known benefit:

- reduces some internal portal frame/diagonal artifacts in the Complementary `no_depth` path.

Known limits:

- no separate silhouette mask texture exists yet;
- no real clipping is provided;
- occlusion remains incomplete;
- shaderpack overbright rendering remains possible;
- not suitable as a promoted default.

## Deferred Work

A true mask should be handled in a future phase, if pursued, by designing a real separate portal silhouette texture plus a controlled composition pass. That work is intentionally not started in Phase 10.7.

Still not activated:

- `glStencil*`
- advanced stencil renderer
- shader clipping
- Sodium shader mixins
- Iris legacy renderers
- DimLib
- AlternateDimensions

## Validation

No compilation was rerun because Phase 10.7 changed documentation only.

The previous Phase 10.6 runtime validation remains the source of truth:

- clean Complementary `no_depth` reference: valid
- clean Complementary `no_depth + alpha_texture`: valid
- invalid mask mode fallback: valid
- crash: 0
- `Missing program`: 0
- `Buffer already closed`: 0
- `ConcurrentModificationException`: 0
- `Duplicate entity UUID`: 0
- `UnsupportedOperationException`: 0

## Conclusion

The Phase 10.x baseline is now explicit:

- MakeUp default remains the stable baseline.
- Complementary `no_depth` remains the robust manual fallback.
- `alpha_texture` remains an experimental opt-in mode that can reduce some visual artifacts, but it is not a clipping or silhouette-mask solution.
- A real mask texture and composition path is deferred to a future phase.
