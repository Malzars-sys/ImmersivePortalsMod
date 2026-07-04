# Phase 12.0 - Alpha Vanilla Package Audit

## Scope

Phase 12.0 audits the minimal vanilla alpha package after the final
four-direction dimension reload matrix from Phase 11.11.

The renderer, Sodium, Iris, DimLib, AlternateDimensions, shaderpack fallbacks,
global chunk sync and advanced chunk tracking were not reopened.

## Git And Baseline

- Phase 11.11 commit present:
  - `e60bf169 Document final vanilla dimension reload matrix`
- Previous harness commit present:
  - `928e23f7 Stabilize End reload portal test harness`
- `run/config/iris.properties`:
  - `shaderPack=MakeUp-UltraFast-9.5c.zip`
- Global `IMM_PTL_*` environment flags:
  - none detected before the audit
- Active Phase 12 runClient processes:
  - none left running after the smoke attempt

## Packaging Fixes Applied

Two packaging-only blockers were found and fixed.

1. Gradle 9 `test` behavior

`build` initially failed because test sources exist but no tests were
discovered:

```text
There are test sources present and no filters are applied, but the test task
did not discover any tests to execute.
```

Fix:

- added `test { failOnNoDiscoveredTests = false }` to `build.gradle`.
- this keeps test compilation active but does not block release packaging when
  the old test sources contain no discoverable runnable tests.

2. Obsolete access widener entries

`validateAccessWidener` failed on removed or isolated 26.1 symbols:

- `GameRules.register(...)`
- `GameRules.BooleanValue.create(boolean)`
- `Program.Type.getGlType()`
- `RegistryDataLoader.Loader`

Fix:

- removed only those stale entries from `src/main/resources/imm_ptl.accesswidener`.
- no runtime renderer, Sodium, Iris, DimLib or chunk tracking code was changed.

## Validation

Commands run:

```powershell
.\gradlew.bat clean compileJava processResources --console=plain
.\gradlew.bat build --console=plain
```

Results:

- `compileJava`: BUILD SUCCESSFUL
- `processResources`: BUILD SUCCESSFUL
- `test`: passed with no-discovered-tests allowed
- `validateAccessWidener`: passed
- `build`: BUILD SUCCESSFUL

Logs:

- `compile-phase12.0-alpha-package.txt`
- `build-phase12.0-alpha-package.txt`

## Jar

Produced jar:

```text
build/libs/immersive-portals-7.0.0-alpha.1-mc26.1-fabric.jar
```

Jar size:

```text
2,872,379 bytes
```

Generated metadata verified:

- `fabric.mod.json`: present
- `imm_ptl.mixins.json`: present
- `imm_ptl.accesswidener`: present
- `imm_ptl_compat.mixins.json`: absent in vanilla generated resources
- `dimlib` dependency: absent from generated `fabric.mod.json`
- required dependencies:
  - `fabricloader >=0.19.3`
  - `fabric-api >=0.145.1`
  - `minecraft 26.1`

Jar content checks:

- `run/`: absent
- `build/`: absent
- `run/saves`: absent
- screenshots: absent
- runtime logs: absent
- shaderpacks: absent
- Sodium jar dependency: absent
- Iris jar dependency: absent
- DimLib jar dependency: absent
- nested jar present:
  - `META-INF/jars/cloth-config-fabric-26.1.154.jar`

The jar still contains internal compatibility facade classes for optional
Sodium/Iris detection, but no Sodium or Iris dependency is embedded or required
by the vanilla profile.

## Generated Vanilla Profile

The generated vanilla `fabric.mod.json` does not list:

- `dimlib`
- `imm_ptl_compat.mixins.json`

The generated vanilla `imm_ptl.mixins.json` keeps the minimal runtime facade
mixins needed by the stable renderer and traversal path:

- `client.render.MixinGameRenderer`
- `client.render.MixinCamera`
- `client.render.MixinLevelRenderer`
- `client.particle.MixinParticleEngine`
- minimal chunk/entity support retained by the Phase 11 baseline

The old heavy render/shader mixins remain excluded from generated vanilla
resources:

- `MixinRenderSystem_Clipping`
- `MixinRenderSystem_Fog`
- `MixinGameRenderer_Shaders`
- `MixinProgram`
- `MixinShaderInstance`
- Iris/Sodium runtime compat mixins

## Smoke Test

A dev-environment vanilla smoke run was attempted with:

```powershell
.\gradlew.bat runClient --console=plain --args=--quickPlaySingleplayer=Phase120AlphaSmoke
```

The client loaded the vanilla profile and reached runtime initialization with
only the expected dev dependencies:

- Fabric Loader `0.19.3`
- Minecraft `26.1`
- Fabric API `0.145.1+26.1`
- Immersive Portals `7.0.0-alpha.1`
- Mod Menu / Cloth Config

No Sodium, Iris or DimLib runtime mod was loaded.

The automated portal smoke was not accepted as final evidence because the
`run` profile/world state was contaminated by previous Phase 11 datapack
repositioning. The test was stopped and no code was changed for that issue.

Gameplay confidence still comes from the clean Phase 11.11 four-direction
matrix:

- Overworld -> Nether reload: OK
- Nether -> Overworld reload: OK
- Overworld -> End reload: OK
- End -> Overworld reload: OK

Recommended before public upload: install the produced jar into an external
clean Fabric instance and repeat a short Overworld -> Overworld smoke test.

## Dev Flags And Commands

Development flags remain opt-in and are not required for normal play:

- `IMM_PTL_AUTO_VISIBLE_TEST_PORTAL`
- `IMM_PTL_AUTO_MINIMAL_TRAVERSAL_TEST`
- `IMM_PTL_AUTO_DIMENSION_TEST_SOURCE`
- `IMM_PTL_AUTO_DIMENSION_TEST_PORTAL`
- `IMM_PTL_SAVE_AFTER_MINIMAL_TEST_PORTAL`
- `IMM_PTL_SAVE_AFTER_MINIMAL_TEST_PORTAL_DELAY_TICKS`
- shaderpack/framebuffer diagnostic flags from Phase 9/10

Development/debug commands remain validation tools, not polished public
gameplay UI:

- `/imm_ptl_debug create_minimal_test_portal`
- `/imm_ptl_debug prepare_dimension_test`
- `/imm_ptl_client_debug test_minimal_portal_traversal`
- `/imm_ptl_client_debug report_loaded_portals`

## Known Limits

- Advanced recursive rendering remains partial.
- General shader/stencil clipping is not restored.
- Shaderpack behavior is not part of the vanilla alpha target.
- Complementary still needs manual fallback modes for best-known behavior.
- Sodium/Iris runtime paths are not part of the vanilla alpha package target.
- DimLib and AlternateDimensions remain isolated.
- Global advanced chunk tracking remains isolated.
- Debug commands exist for validation but are not a final user-facing workflow.

## Conclusion

The minimal vanilla alpha package is buildable and internally clean:

- jar produced: yes
- jar path:
  - `build/libs/immersive-portals-7.0.0-alpha.1-mc26.1-fabric.jar`
- vanilla build: BUILD SUCCESSFUL
- access widener validation: passed
- generated metadata: clean for vanilla
- temporary files embedded: no
- Sodium/Iris/DimLib required by vanilla package: no

Status: alpha vanilla package candidate is ready for an external clean-instance
smoke test before upload.

Recommended next phase: Phase 12.1 external alpha smoke test / release
checklist, using the produced jar in a clean Fabric instance with only Fabric
API and optional Mod Menu.
