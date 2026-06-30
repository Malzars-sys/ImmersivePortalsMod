# Shaderpack Fallbacks for Minecraft 26.1

This document describes the current Immersive Portals shaderpack fallback modes for the 1.21.6 / Fabric API 26.1 port.

The defaults are intentionally conservative. No fallback is selected automatically based on shaderpack name.

## Current Baseline

### MakeUp-UltraFast-9.5c.zip

MakeUp is the current stable shaderpack baseline.

Recommended setup:

- keep `shaderPack=MakeUp-UltraFast-9.5c.zip` in `run/config/iris.properties`;
- do not set any `IMM_PTL_FRAMEBUFFER_*` fallback flag.

Expected mode:

- `default` / `depth-masked-equal`

Known status:

- stable baseline;
- portal framebuffer visible;
- depth mask behavior preserved better than the manual fallback modes.

## ComplementaryReimagined_r5.8.1.zip

Complementary is partially compatible with the current minimal portal renderer.

Known modes:

- `default`: can be intermittent or too dark;
- `lequal`: can be visible, but remains intermittent;
- `no_depth`: robust manual fallback, but occlusion is degraded;
- `no_depth + alpha_texture`: experimental, can reduce some internal cyan artifacts, but is not a true portal mask.

## Recipes

### MakeUp Stable

Use this when testing the stable shaderpack baseline.

```powershell
# run/config/iris.properties
shaderPack=MakeUp-UltraFast-9.5c.zip

# Do not set:
# IMM_PTL_FRAMEBUFFER_DEPTH_MODE
# IMM_PTL_FRAMEBUFFER_MASK_MODE
```

### Complementary Robust Fallback

Use this when Complementary makes the portal too dark or intermittent.

```powershell
$env:IMM_PTL_FRAMEBUFFER_DEPTH_MODE = "no_depth"
```

```properties
# run/config/iris.properties
shaderPack=ComplementaryReimagined_r5.8.1.zip
```

Tradeoff:

- more robust visibility;
- degraded occlusion;
- not a true clipping solution.

### Complementary Experimental Fallback

Use this only for experiments comparing the alpha-aware composition path.

```powershell
$env:IMM_PTL_FRAMEBUFFER_DEPTH_MODE = "no_depth"
$env:IMM_PTL_FRAMEBUFFER_MASK_MODE = "alpha_texture"
```

```properties
# run/config/iris.properties
shaderPack=ComplementaryReimagined_r5.8.1.zip
```

Tradeoff:

- can reduce some internal cyan frame/diagonal artifacts;
- still not a true portal silhouette mask;
- still has degraded occlusion;
- not recommended as a normal user default.

### Return to Default

In PowerShell:

```powershell
Remove-Item Env:IMM_PTL_FRAMEBUFFER_DEPTH_MODE -ErrorAction SilentlyContinue
Remove-Item Env:IMM_PTL_FRAMEBUFFER_MASK_MODE -ErrorAction SilentlyContinue
```

Restore the stable shaderpack baseline if needed:

```properties
# run/config/iris.properties
shaderPack=MakeUp-UltraFast-9.5c.zip
```

## Modes Not Recommended as Normal User Settings

These modes are kept for diagnostics or controlled experiments:

- `IMM_PTL_FRAMEBUFFER_DEPTH_MODE=lequal`
- `IMM_PTL_FRAMEBUFFER_DEPTH_MODE=always`
- `IMM_PTL_FRAMEBUFFER_ORDER_MODE=quad_first`
- `IMM_PTL_FRAMEBUFFER_ORDER_MODE=no_mask_reference`
- `IMM_PTL_PORTAL_CPU_CLIP_MODE=portal_quad_only`
- `IMM_PTL_FRAMEBUFFER_MASK_MODE=alpha_texture`

## Important Notes

- `no_depth` is not automatically enabled for Complementary.
- `alpha_texture` is not automatically enabled for Complementary.
- The current renderer does not use `glStencil*`.
- The current renderer does not enable global shader clipping.
- The current renderer does not restore the old Iris portal renderers.
- DimLib and dynamic AlternateDimensions remain isolated.

## Troubleshooting Checklist

If a shaderpack portal is invisible or flickers:

1. Confirm the mod is running with the expected shaderpack in `run/config/iris.properties`.
2. Test MakeUp with no fallback flags.
3. For Complementary, try `IMM_PTL_FRAMEBUFFER_DEPTH_MODE=no_depth`.
4. If comparing artifacts, optionally add `IMM_PTL_FRAMEBUFFER_MASK_MODE=alpha_texture`.
5. Remove all `IMM_PTL_*` flags before returning to baseline testing.
