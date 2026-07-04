# Phase 11.1 - Shaderpack Docs and Flags Inventory

Date: 2026-06-30

## Scope

Create user-facing shaderpack fallback documentation and a developer inventory of `IMM_PTL_*` flags without changing runtime behavior.

No Java, mixin, shader, pipeline, render target, Gradle, DimLib, Sodium shader, or Iris legacy renderer code was modified.

## State Verification

Checked:

- `git status --short`
- `git log --oneline -20`
- `run/config/iris.properties`
- active `IMM_PTL_*` environment flags
- active Java `runClient` processes

Confirmed:

- Phase 10.8 freeze commit is present:
  - `5c4b7dcf Freeze Phase 10 shaderpack rendering baseline`
- Phase 11.0 decision commit is present:
  - `cf52c2b9 Document post shaderpack freeze decision audit`
- `run/config/iris.properties` is:
  - `shaderPack=MakeUp-UltraFast-9.5c.zip`
- no `IMM_PTL_*` environment flag is active in the Codex shell
- no Phase 10.x / 11.x `runClient` process is active

The working tree remains noisy with old generated logs and copied files. These are unrelated to the Phase 11.1 documentation work.

## Inventory Method

Searched:

- `src/main/java`
- `src/main/resources`
- Phase 9-11 reports
- `MIGRATION_PLAN_26.1.md`

Primary source of truth for active environment flags:

- `RendererUsingFrameBuffer.java`
- `IPModEntryClient.java`

Additional note:

- `IMM_PTL_LOG_COUNTER` appears in `MixinEntity.java`, but it is an internal static field name, not an environment flag.

## Flags Inventoried

Environment flags:

- `IMM_PTL_FRAMEBUFFER_DEPTH_MODE`
- `IMM_PTL_FRAMEBUFFER_ORDER_MODE`
- `IMM_PTL_PORTAL_CPU_CLIP_MODE`
- `IMM_PTL_NO_DEPTH_GEOMETRIC_CLIP`
- `IMM_PTL_FRAMEBUFFER_MASK_MODE`
- `IMM_PTL_FORCE_FRAMEBUFFER_NO_DEPTH_MASK`
- `IMM_PTL_AUTO_VISIBLE_TEST_PORTAL`
- `IMM_PTL_AUTO_MINIMAL_TRAVERSAL_TEST`
- `IMM_PTL_CAPTURE_MINIMAL_RECURSIVE_PORTAL`
- `IMM_PTL_MINIMAL_RECURSIVE_PORTAL_SCREENSHOT`

Internal non-env identifier:

- `IMM_PTL_LOG_COUNTER`

## Documentation Created

### User/Testers

Created:

- `SHADERPACK_FALLBACKS_26.1.md`

It documents:

- MakeUp as the stable shaderpack baseline;
- Complementary manual fallback behavior;
- `default` as normal behavior;
- `no_depth` as robust manual fallback with degraded occlusion;
- `alpha_texture` as experimental and incomplete;
- `lequal` as intermittent and not recommended;
- reset instructions for returning to default.

### Developers

Created:

- `IMM_PTL_DEV_FLAGS_26.1.md`

It documents each flag with:

- known values;
- default;
- category;
- phase of introduction;
- effect;
- risks;
- status.

## Classification Summary

User/tester:

- `IMM_PTL_FRAMEBUFFER_DEPTH_MODE=no_depth` for manual Complementary fallback.

Debug/dev:

- `IMM_PTL_AUTO_VISIBLE_TEST_PORTAL`
- `IMM_PTL_AUTO_MINIMAL_TRAVERSAL_TEST`
- `IMM_PTL_CAPTURE_MINIMAL_RECURSIVE_PORTAL`
- `IMM_PTL_MINIMAL_RECURSIVE_PORTAL_SCREENSHOT`

Experimental dangerous:

- `IMM_PTL_FRAMEBUFFER_ORDER_MODE`
- `IMM_PTL_PORTAL_CPU_CLIP_MODE`
- `IMM_PTL_FRAMEBUFFER_MASK_MODE=alpha_texture`
- `IMM_PTL_FRAMEBUFFER_DEPTH_MODE=lequal`
- `IMM_PTL_FRAMEBUFFER_DEPTH_MODE=always`

Legacy aliases:

- `IMM_PTL_FORCE_FRAMEBUFFER_NO_DEPTH_MASK`
- `IMM_PTL_NO_DEPTH_GEOMETRIC_CLIP`

Internal only:

- `IMM_PTL_LOG_COUNTER`

## Runtime Behavior

Unchanged.

No flags were renamed, removed, or promoted. The default remains:

- no framebuffer fallback flag;
- MakeUp shaderpack restored in `run/config/iris.properties`;
- no automatic shaderpack-specific mode selection.

## Validation

No compilation was run because only Markdown files were changed.

No `runClient` was launched.

## Conclusion

Phase 11.1 converts the Phase 9-10 experimental state into usable documentation:

- testers can use `no_depth` for Complementary fallback experiments;
- developers have a clear flag inventory;
- risky modes remain explicitly marked as diagnostic or experimental;
- the Phase 10.8 shaderpack baseline remains untouched.
