# Immersive Portals 26.1 Developer Flags

This document inventories the current `IMM_PTL_*` environment flags used by the 1.21.6 / Fabric API 26.1 port.

These flags are intentionally not config options. They are runtime/test switches for controlled validation.

## Categories

- User/tester: acceptable to document for manual fallback testing.
- Debug/dev: useful for automation, screenshots, diagnostics, or test worlds.
- Experimental dangerous: useful only for controlled experiments; do not recommend as normal settings.
- Legacy alias: kept for compatibility with previous test scripts.

## User / Tester Flags

### IMM_PTL_FRAMEBUFFER_DEPTH_MODE

- Values:
  - `default`, `depth_mask`, `depth_masked`, `equal`
  - `no_depth`, `none`, `disabled`, `off`
  - `lequal`, `less_equal`, `less_or_equal`
  - `always`, `always_depth`
- Default: `default`
- Category: user/tester plus experimental values
- Introduced: Phase 9.2
- Effect:
  - selects the portal framebuffer depth behavior.
- Recommended use:
  - `no_depth` can be used as a manual shaderpack fallback for Complementary.
- Risks:
  - `no_depth` degrades occlusion;
  - `lequal` is intermittent under Complementary;
  - `always` is diagnostic only.
- Status:
  - kept;
  - `default` remains normal behavior;
  - `no_depth` is a manual fallback;
  - `lequal` and `always` are not promoted.

### IMM_PTL_FRAMEBUFFER_MASK_MODE

- Values:
  - `off`, `false`, `disabled`, `none`
  - `alpha_texture`, `alpha`, `texture_mask`
- Default: `off`
- Category: experimental dangerous
- Introduced: Phase 10.5
- Effect:
  - selects an experimental alpha-aware portal framebuffer composition path when the framebuffer is non-depth-masked.
- Recommended use:
  - only with `IMM_PTL_FRAMEBUFFER_DEPTH_MODE=no_depth` for controlled Complementary comparisons.
- Risks:
  - not a true silhouette mask;
  - does not solve general clipping;
  - can still produce overbright or degraded output.
- Status:
  - kept as opt-in;
  - not promoted;
  - must not be auto-selected by shaderpack.

## Debug / Dev Flags

### IMM_PTL_AUTO_VISIBLE_TEST_PORTAL

- Values:
  - `true`
- Default: disabled
- Category: debug/dev
- Introduced: Phase 4.9 / 5.x validation flow
- Effect:
  - in a development environment, sends `imm_ptl_debug create_visible_test_portal` after the client world loads.
- Risks:
  - creates test portals in the active world;
  - can pollute visual validation if reused with old worlds.
- Status:
  - kept for automation;
  - not for normal users.

### IMM_PTL_AUTO_MINIMAL_TRAVERSAL_TEST

- Values:
  - `true`
- Default: disabled
- Category: debug/dev
- Introduced: Phase 4.7 / 5.x validation flow
- Effect:
  - in a development environment, sends `imm_ptl_client_debug test_minimal_portal_traversal` after a delay.
- Risks:
  - moves the player;
  - can disrupt visual tests if enabled too early.
- Status:
  - kept for regression tests;
  - not for normal users.

### IMM_PTL_CAPTURE_MINIMAL_RECURSIVE_PORTAL

- Values:
  - `true`
- Default: disabled
- Category: debug/dev
- Introduced: Phase 5.10 / visual validation flow
- Effect:
  - captures a native Minecraft screenshot after the minimal recursive portal render path has produced a frame.
- Risks:
  - can create screenshots in `run/screenshots`;
  - capture timing may not match human visual observation.
- Status:
  - kept for visual validation.

### IMM_PTL_MINIMAL_RECURSIVE_PORTAL_SCREENSHOT

- Values:
  - filename, sanitized to the filename component
- Default:
  - `imm_ptl-minimal-recursive-portal.png`
- Category: debug/dev
- Introduced: Phase 5.10 / visual validation flow
- Effect:
  - names the screenshot produced by `IMM_PTL_CAPTURE_MINIMAL_RECURSIVE_PORTAL=true`.
- Risks:
  - invalid names fall back to the default filename;
  - should not be used to write outside the screenshot directory.
- Status:
  - kept for visual validation.

### IMM_PTL_LOG_COUNTER

- Values:
  - not an environment flag; internal static field name
- Default:
  - internal `CountDownInt(20)`
- Category: internal debug implementation detail
- Introduced:
  - pre-existing collision logging guard
- Effect:
  - limits repeated collision warning logs.
- Risks:
  - none as a user flag because it is not read from the environment.
- Status:
  - do not document as user-configurable.

## Experimental / Diagnostic Flags

### IMM_PTL_FRAMEBUFFER_ORDER_MODE

- Values:
  - `default`
  - `mask_first`, `mask_first_explicit`, `depth_first`
  - `quad_first`, `framebuffer_first`, `texture_first`
  - `no_mask`, `no_mask_reference`, `quad_only`
- Default: `default`
- Category: experimental dangerous
- Introduced: Phase 10.1
- Effect:
  - changes submission order or disables the depth mask for framebuffer-order diagnostics.
- Risks:
  - `quad_first` is unstable/intermittent;
  - `no_mask_reference` degrades occlusion;
  - not a real fix for shaderpack clipping.
- Status:
  - kept for diagnostics;
  - do not promote.

### IMM_PTL_PORTAL_CPU_CLIP_MODE

- Values:
  - `off`, `false`, `disabled`, `none`
  - `portal_quad_only`, `quad_only`, `bounds`
  - `conservative_plane`, `plane`
  - `debug_bounds`, `debug`
- Default: `off`
- Category: experimental dangerous
- Introduced: Phase 10.3
- Effect:
  - applies or logs limited CPU/geometric portal bounds checks for the non-depth-masked path.
- Risks:
  - does not clip already-rendered framebuffer content;
  - can reject portals incorrectly if expanded later;
  - mainly diagnostic.
- Status:
  - kept for diagnostics;
  - do not promote.

### IMM_PTL_NO_DEPTH_GEOMETRIC_CLIP

- Values:
  - `true`
- Default: disabled
- Category: legacy alias
- Introduced: Phase 10.3
- Effect:
  - legacy alias for `IMM_PTL_PORTAL_CPU_CLIP_MODE=portal_quad_only`.
- Risks:
  - less explicit than the newer flag;
  - should not be recommended for new tests.
- Status:
  - kept as legacy alias;
  - do not promote.

### IMM_PTL_FORCE_FRAMEBUFFER_NO_DEPTH_MASK

- Values:
  - `true`
- Default: disabled
- Category: legacy alias
- Introduced: Phase 9.1
- Effect:
  - legacy alias that selects the non-depth-masked framebuffer path when `IMM_PTL_FRAMEBUFFER_DEPTH_MODE` is not set.
- Recommended replacement:
  - `IMM_PTL_FRAMEBUFFER_DEPTH_MODE=no_depth`
- Risks:
  - degrades occlusion like `no_depth`;
  - less explicit than the newer mode flag.
- Status:
  - kept for compatibility with older test scripts;
  - do not promote.

## Quick Matrix

| Flag | Category | Default | Status |
| --- | --- | --- | --- |
| `IMM_PTL_FRAMEBUFFER_DEPTH_MODE` | user/tester + experimental | `default` | kept |
| `IMM_PTL_FRAMEBUFFER_MASK_MODE` | experimental dangerous | `off` | opt-in only |
| `IMM_PTL_FRAMEBUFFER_ORDER_MODE` | experimental dangerous | `default` | diagnostics only |
| `IMM_PTL_PORTAL_CPU_CLIP_MODE` | experimental dangerous | `off` | diagnostics only |
| `IMM_PTL_NO_DEPTH_GEOMETRIC_CLIP` | legacy alias | disabled | kept, not promoted |
| `IMM_PTL_FORCE_FRAMEBUFFER_NO_DEPTH_MASK` | legacy alias | disabled | kept, not promoted |
| `IMM_PTL_AUTO_VISIBLE_TEST_PORTAL` | debug/dev | disabled | automation only |
| `IMM_PTL_AUTO_MINIMAL_TRAVERSAL_TEST` | debug/dev | disabled | automation only |
| `IMM_PTL_CAPTURE_MINIMAL_RECURSIVE_PORTAL` | debug/dev | disabled | visual validation |
| `IMM_PTL_MINIMAL_RECURSIVE_PORTAL_SCREENSHOT` | debug/dev | default filename | visual validation |

## Reset to Baseline

In PowerShell:

```powershell
Remove-Item Env:IMM_PTL_FRAMEBUFFER_DEPTH_MODE -ErrorAction SilentlyContinue
Remove-Item Env:IMM_PTL_FRAMEBUFFER_MASK_MODE -ErrorAction SilentlyContinue
Remove-Item Env:IMM_PTL_FRAMEBUFFER_ORDER_MODE -ErrorAction SilentlyContinue
Remove-Item Env:IMM_PTL_PORTAL_CPU_CLIP_MODE -ErrorAction SilentlyContinue
Remove-Item Env:IMM_PTL_NO_DEPTH_GEOMETRIC_CLIP -ErrorAction SilentlyContinue
Remove-Item Env:IMM_PTL_FORCE_FRAMEBUFFER_NO_DEPTH_MASK -ErrorAction SilentlyContinue
Remove-Item Env:IMM_PTL_AUTO_VISIBLE_TEST_PORTAL -ErrorAction SilentlyContinue
Remove-Item Env:IMM_PTL_AUTO_MINIMAL_TRAVERSAL_TEST -ErrorAction SilentlyContinue
Remove-Item Env:IMM_PTL_CAPTURE_MINIMAL_RECURSIVE_PORTAL -ErrorAction SilentlyContinue
Remove-Item Env:IMM_PTL_MINIMAL_RECURSIVE_PORTAL_SCREENSHOT -ErrorAction SilentlyContinue
```

Restore:

```properties
shaderPack=MakeUp-UltraFast-9.5c.zip
```
