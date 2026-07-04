# Phase 11.11 - Final Vanilla Dimension Reload Matrix

## Scope

Phase 11.11 replays the four real vanilla dimension directions after the
Phase 11.10 End reload harness fix.

No runtime code was changed in this phase. The renderer, Sodium, Iris, DimLib,
AlternateDimensions, shaderpack fallbacks, `ImmPtlClientChunkMap`, chunk sync
and advanced chunk tracking were not touched.

## Baseline

- Latest expected commit present: `928e23f7 Stabilize End reload portal test harness`
- `run/config/iris.properties`: `shaderPack=MakeUp-UltraFast-9.5c.zip`
- Global `IMM_PTL_*` flags before testing: none detected
- Active Phase 11.x runClient process before final log review: none detected
- Compile baseline: Phase 11.10 `compileJava processResources` was
  `BUILD SUCCESSFUL`

The Phase 11.11 work used only the debug/dev test harness and clean dedicated
worlds. The harness used `IMM_PTL_SAVE_AFTER_MINIMAL_TEST_PORTAL=true` for
creation runs so the newly created normal portal is saved before the automated
client shutdown.

## Worlds And Logs

- `Phase1111OverworldToNetherReload`
  - `runclient-phase11.11-overworld-to-nether-reload.txt`
  - `runclient-phase11.11-overworld-to-nether-reload-stderr.txt`
- `Phase1111NetherToOverworldReload`
  - `runclient-phase11.11-nether-to-overworld-reload.txt`
  - `runclient-phase11.11-nether-to-overworld-reload-stderr.txt`
- `Phase1111OverworldToEndReload`
  - `runclient-phase11.11-overworld-to-end-reload.txt`
  - `runclient-phase11.11-overworld-to-end-reload-stderr.txt`
- `Phase1111EndToOverworldReload`
  - `runclient-phase11.11-end-to-overworld-reload.txt`
  - `runclient-phase11.11-end-to-overworld-reload-stderr.txt`

## Matrix

| Direction | First traversal | Reload traversal | Result |
| --- | --- | --- | --- |
| Overworld -> Nether | `Client Teleported Statically` | `Client Teleported Statically` | OK |
| Nether -> Overworld | `Client Teleported Statically` | `Client Teleported Statically` | OK |
| Overworld -> End | `Client Teleported Statically` | `Client Teleported Statically` | OK |
| End -> Overworld | `Client Teleported Statically` | `Client Teleported Statically` | OK |

## Important Harness Notes

The non-Overworld source dimensions needed a longer traversal delay than the
initial recommendation. Earlier failed attempts were timing artifacts:

- `Nether -> Overworld` with the short delay ran the traversal before the portal
  had finished creating/syncing.
- `End -> Overworld` with an overly long delay could let the player drift/fall
  before traversal.
- The final `End -> Overworld` run used the Phase 11.10 save-after-create
  harness and validated both creation traversal and reload traversal.

These timing adjustments did not require runtime changes.

## Negative Signal Check

Final logs for all four directions:

- `No nearby portal`: 0
- `Network Protocol Error`: 0
- `Error deserializing chunk packet`: 0
- `Ignoring incompatible vanilla chunk packet after world switch`: 0
- `Duplicate entity UUID`: 0
- `ConcurrentModificationException`: 0
- `Buffer already closed`: 0
- `Missing program`: 0
- `UnsupportedOperationException`: 0
- crash: 0

The string `fabric-crash-report-info` appears in mod loading output, but no
runtime crash was observed.

## Conclusion

The final vanilla 4-direction reload matrix is green:

- Overworld -> Nether reload: OK
- Nether -> Overworld reload: OK
- Overworld -> End reload: OK
- End -> Overworld reload: OK

The minimal vanilla alpha path is recommended from the dimension traversal and
reload perspective, with this known caveat: automated interdimensional tests
should keep using the stabilized debug/dev harness save flag and tuned traversal
delays for Nether/End source dimensions.

Recommended next phase: package/alpha readiness audit, focused on tracked files,
dev flags, release notes, and avoiding accidental inclusion of `run/`, `build/`,
logs, screenshots, or temporary worlds.
