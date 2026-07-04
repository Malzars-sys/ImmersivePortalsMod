# Phase 12.1 - External Alpha Vanilla Smoke Test

## Scope

Phase 12.1 validates the alpha vanilla jar outside the development `run/`
profile, using a separate clean instance directory.

No runtime code was changed. The renderer, Sodium, Iris, DimLib,
AlternateDimensions, shaderpack fallbacks, `ImmPtlClientChunkMap`, global chunk
sync and advanced chunk tracking were not touched.

## Baseline

- Phase 12.0 commit present:
  - `de59ac04 Prepare minimal vanilla alpha package`
- Jar under test:
  - `build/libs/immersive-portals-7.0.0-alpha.1-mc26.1-fabric.jar`
- External instance:
  - `C:\Users\simeo\Downloads\ImmersivePortalsAlphaSmoke`
- External world:
  - `Phase121ExternalAlphaSmoke`
- Global `IMM_PTL_*` flags after the test:
  - none detected
- Active external/runClient process after the test:
  - none detected

## External Instance Setup

The external instance was created outside the repository and outside the dev
`run/` directory.

Installed/runtime components:

- Minecraft `26.1`
- Fabric Loader `0.19.3`
- Fabric API `0.145.1+26.1`
- Immersive Portals `7.0.0-alpha.1`
- Cloth Config bundled inside the Immersive Portals jar

Mods folder:

```text
C:\Users\simeo\Downloads\ImmersivePortalsAlphaSmoke\mods\fabric-api-0.145.1+26.1.jar
C:\Users\simeo\Downloads\ImmersivePortalsAlphaSmoke\mods\immersive-portals-7.0.0-alpha.1-mc26.1-fabric.jar
```

Explicitly absent:

- Sodium
- Iris
- DimLib
- shaderpacks
- old Phase 11 worlds
- Phase 11 datapacks
- repository `run/` artifacts

## Launch Method

The official `.minecraft` installation did not already contain a Fabric 26.1
profile. To keep the test reproducible, the external instance was prepared
directly:

1. Downloaded/installed Minecraft `26.1` into the external instance.
2. Installed Fabric Loader `0.19.3` into the external instance.
3. Generated a clean singleplayer-compatible world using the vanilla 26.1
   server jar, then copied it into the external instance `saves/`.
4. Launched Fabric/Knot directly with the external instance as `gameDir`.

Launch artifacts:

- `phase12.1-external-launch-command.json`
- `phase12.1-external-launch-command-fixed.json`
- `phase12.1-external-launch-command.txt`
- `install-minecraft-26.1-phase12.1.txt`
- `fabric-install-external-phase12.1.txt`
- `server-phase12.1-generate-world.txt`

## Result

External launch:

- Minecraft started: yes
- Fabric Loader loaded: yes, `0.19.3`
- Minecraft version: `26.1`
- Mods loaded: 50
- Immersive Portals loaded: yes, `7.0.0-alpha.1`
- Fabric API loaded: yes, `0.145.1+26.1`
- Sodium loaded: no
- Iris loaded: no
- DimLib loaded: no
- Shaderpack loaded: no
- Menu/runtime initialization reached: yes
- External clean world reached: yes
- Player joined world: yes

Key log evidence:

```text
Loading Minecraft 26.1 with Fabric Loader 0.19.3
Loading 50 mods:
    - fabric-api 0.145.1+26.1
    - immersive_portals 7.0.0-alpha.1
    - minecraft 26.1
Dimensional Threading is not present
Gravity API is not present
Reloading ResourceManager: vanilla, cloth-config, fabric-api, ..., immersive_portals
PlayerAlphaSmoke joined the game
```

Expected offline-login warnings were observed because the direct launch used a
dummy offline token:

- `Failed to fetch user properties` / 401
- Realms authentication warning

These did not block the client or world load.

## Negative Signal Check

External smoke log:

- `Network Protocol Error`: 0
- `Error deserializing chunk packet`: 0
- `Duplicate entity UUID`: 0
- `ConcurrentModificationException`: 0
- `Buffer already closed`: 0
- `Missing program`: 0
- `UnsupportedOperationException`: 0
- `Mixin apply failed`: 0
- `AccessWidener` / access widener failure: 0
- `fabric.mod.json` error: 0
- `ClassNotFoundException`: 0 after launch command correction
- `NoClassDefFoundError`: 0

## Portal Smoke

The minimal portal dev smoke was not executed in the external instance.

Reason:

- `/imm_ptl_debug` server commands are intentionally registered only when
  `FabricLoader.getInstance().isDevelopmentEnvironment()` is true.
- the automatic portal test hooks are also gated behind development environment
  checks.
- the external jar test is a production-style environment, so the dev harness is
  correctly unavailable.

Relevant code:

- `PortalDebugCommands.register(...)` returns early outside development
  environment.
- `IPModEntryClient` only registers automatic portal test ticks in development
  environment.

Therefore Phase 12.1 validates external jar loading and clean world entry. Portal
gameplay confidence remains covered by the Phase 11.11 vanilla matrix.

## Logs

- `runclient-phase12.1-external-alpha-smoke.txt`
- `runclient-phase12.1-external-alpha-smoke-stderr.txt`
- `server-phase12.1-generate-world.txt`
- `server-phase12.1-generate-world-stderr.txt`
- `fabric-install-external-phase12.1.txt`
- `install-minecraft-26.1-phase12.1.txt`

The direct launch wrote Minecraft logs to captured stdout rather than
`logs/latest.log`.

## Conclusion

External clean instance result:

- jar charged OK: yes
- menu/runtime initialized OK: yes
- clean world loaded OK: yes
- no Sodium/Iris/DimLib loaded: yes
- no mixin/access widener/mod metadata blocker: yes
- minimal portal dev command available externally: no, by design
- portal traversal proof source: Phase 11.11 matrix

Status:

- alpha vanilla is locally publishable as a minimal jar candidate from a
  packaging and external-launch perspective.

Recommended next phase:

- Phase 12.2 release checklist / publication prep:
  - confirm tracked files to commit;
  - decide whether logs stay untracked;
  - update release notes and known limitations;
  - optionally add a non-dev user-facing minimal portal test command or document
    that portal gameplay validation remains internal/dev-only for alpha QA.
