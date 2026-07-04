# Phase 10.6 - Alpha Texture Runtime Validation

Date: 2026-06-30

## Scope

Validate the opt-in `IMM_PTL_FRAMEBUFFER_MASK_MODE=alpha_texture` runtime path without changing renderer code, pipelines, Iris mappings, Sodium shader mixins, DimLib, or the default global framebuffer behavior.

The first MakeUp run used `Phase106MakeUpDefaultManual`, but that world was not retained for visual comparison after discovering that old portal attempts could pollute the field of view. The reliable comparison runs below use clean worlds created from `Phase80IrisShaderpackWorldTest`, which had zero portal string hits before launch.

## Clean World Preparation

Created clean worlds:

- `Phase106ComplementaryNoDepthReferenceClean`
- `Phase106ComplementaryNoDepthAlphaTextureClean`
- `Phase106InvalidMaskModeClean`

Pre-launch scan result for each clean world:

- `portal-string-hits=0`

This avoids comparing against old saved portal entities.

## Test A - MakeUp Default

Status: not used as final visual comparison.

Log file:

- `runclient-phase10.6-makeup-default-manual.txt`

Observed before clean-world reset:

- shaderpack: `MakeUp-UltraFast-9.5c.zip`
- depth mode: default, `depth-masked-equal`
- mask mode: `off`
- framebuffer texture: available, `854x480`
- SubmitNodeCollector quad: submitted
- automatic screenshot: `run/screenshots/phase10.6-makeup-default.png`
- crash: no
- `Buffer already closed`: 0 in checked log segment
- `ConcurrentModificationException`: 0 in checked log segment
- `Duplicate entity UUID`: 0 in checked log segment

This run is useful as a smoke test only, not as the clean visual reference.

## Test B - Complementary no_depth Reference

World:

- `Phase106ComplementaryNoDepthReferenceClean`

Shaderpack:

- `ComplementaryReimagined_r5.8.1.zip`

Flags:

- `IMM_PTL_FRAMEBUFFER_DEPTH_MODE=no_depth`
- `IMM_PTL_FRAMEBUFFER_MASK_MODE=off`
- `IMM_PTL_AUTO_VISIBLE_TEST_PORTAL=true`
- `IMM_PTL_CAPTURE_MINIMAL_RECURSIVE_PORTAL=true`

Log file:

- `runclient-phase10.6-complementary-no-depth-reference-clean.txt`

Result:

- clean pre-launch world: yes
- auto-created portal: yes
- relevant client portal: `Portal{229,...}`
- shaderpack active: yes
- framebuffer texture: available, `854x480`
- pipeline mode: `non-depth-masked`
- depth mask: intentionally disabled
- SubmitNodeCollector quad: submitted
- screenshot: `run/screenshots/phase10.6-complementary-no-depth-reference.png`
- crash: no
- `Missing program`: 0
- `Buffer already closed`: 0
- `ConcurrentModificationException`: 0
- `Duplicate entity UUID`: 0
- `UnsupportedOperationException`: 0

Visual note:

- one portal is visible in the capture;
- no old portal is visible in the field;
- the framebuffer content is very bright;
- cyan portal frame and internal diagonals remain visible.

## Test C - Complementary no_depth + alpha_texture

World:

- `Phase106ComplementaryNoDepthAlphaTextureClean`

Shaderpack:

- `ComplementaryReimagined_r5.8.1.zip`

Flags:

- `IMM_PTL_FRAMEBUFFER_DEPTH_MODE=no_depth`
- `IMM_PTL_FRAMEBUFFER_MASK_MODE=alpha_texture`
- `IMM_PTL_AUTO_VISIBLE_TEST_PORTAL=true`
- `IMM_PTL_CAPTURE_MINIMAL_RECURSIVE_PORTAL=true`

Log file:

- `runclient-phase10.6-complementary-no-depth-alpha-texture-clean.txt`

Result:

- clean pre-launch world: yes
- auto-created portal: yes
- relevant client portal: `Portal{221,...}`
- shaderpack active: yes
- framebuffer mask mode: `alpha_texture`
- alpha texture composition attempted: yes
- alpha texture composition pipeline available: yes
- framebuffer texture: available, `854x480`
- pipeline mode: `non-depth-masked-alpha-texture`
- depth mask: intentionally disabled
- SubmitNodeCollector quad: submitted
- screenshot: `run/screenshots/phase10.6-complementary-no-depth-alpha-texture.png`
- crash: no
- `Missing program`: 0
- `Buffer already closed`: 0
- `ConcurrentModificationException`: 0
- `Duplicate entity UUID`: 0
- `UnsupportedOperationException`: 0

Visual note:

- one portal is visible in the capture;
- no old portal is visible in the field;
- the framebuffer content remains very bright;
- the internal cyan diagonals/frame artifacts are reduced compared with the `no_depth` reference;
- this does not prove a full mask yet, because the prototype does not create a separate silhouette texture.

## Test D - Invalid Mask Mode

World:

- `Phase106InvalidMaskModeClean`

Shaderpack:

- `MakeUp-UltraFast-9.5c.zip`

Flags:

- `IMM_PTL_FRAMEBUFFER_MASK_MODE=banana`
- `IMM_PTL_AUTO_VISIBLE_TEST_PORTAL=true`
- `IMM_PTL_CAPTURE_MINIMAL_RECURSIVE_PORTAL=true`

Log file:

- `runclient-phase10.6-invalid-mask-mode-clean.txt`

Result:

- clean pre-launch world: yes
- auto-created portal: yes
- relevant client portal: `Portal{234,...}`
- warning observed: `Unknown IMM_PTL_FRAMEBUFFER_MASK_MODE 'banana'; falling back to off`
- fallback mode: `off`
- framebuffer texture: available, `854x480`
- SubmitNodeCollector quad: submitted
- screenshot: `run/screenshots/phase10.6-invalid-mask-mode.png`
- crash: no
- `Missing program`: 0
- `Buffer already closed`: 0
- `ConcurrentModificationException`: 0
- `Duplicate entity UUID`: 0
- `UnsupportedOperationException`: 0

## Final State

- `run/config/iris.properties` restored to `shaderPack=MakeUp-UltraFast-9.5c.zip`
- no `IMM_PTL_*` environment flags left active in the Codex shell
- no Phase 10.6 `runClient` process left running
- no renderer code changed in Phase 10.6

## Conclusion

`alpha_texture` is runtime-safe under Complementary `no_depth` and successfully selects the new `non-depth-masked-alpha-texture` pipeline. The clean-world captures show a visible difference from plain `no_depth`: the internal cyan portal-frame artifacts are reduced, but the portal view remains overbright and the prototype still does not provide true clipping.

Recommendation for Phase 10.7:

- keep `alpha_texture` opt-in only;
- do not promote it as default;
- if pursuing this direction, implement a real separate portal silhouette mask texture plus composition pass;
- otherwise keep `no_depth` as the known shaderpack-safe manual fallback and move to a different occlusion/clipping strategy.
