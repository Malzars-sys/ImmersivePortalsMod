# Phase 11.9 - Dimension Reload Regression After Chunk Fix

## Scope

This phase re-ran the short four-direction reload regression after the Phase 11.8
`ImmPtlClientChunkMap` stale vanilla chunk packet fix.

No renderer, shaderpack, Sodium, Iris, DimLib, AlternateDimensions, chunk sync
global, or advanced chunk tracking code was changed.

## Baseline

- Latest compile baseline: Phase 11.8 `compileJava processResources`: BUILD SUCCESSFUL.
- Phase 11.8 fix active:
  - stale non-redirected vanilla chunk packets after world switch are ignored
    without inserting incomplete chunks;
  - redirected Immersive Portals chunk packets remain strict.
- `run/config/iris.properties`: `shaderPack=MakeUp-UltraFast-9.5c.zip`.
- No global `IMM_PTL_*` flags were active before the tests.
- No Phase 11.x `runClient` process remained active after the tests.

## Test Worlds

- `Phase119OverworldToNetherReload`
- `Phase119NetherToOverworldReload`
- `Phase119OverworldToEndReload`
- `Phase119EndToOverworldReload`

For the first three directions, the test used the dedicated Phase 11.9 worlds:
first traversal, then reload traversal without recreating the portal. The
Nether -> Overworld case received an additional reload pass.

For End -> Overworld, the first clean attempt was affected by End-specific test
harness instability: the initial run can traverse when the portal is selected,
but reload attempts repeatedly reached `No nearby portal` rather than a network
or chunk-packet failure. This is recorded as not revalidated in Phase 11.9 and
should be handled by a dedicated Phase 11.10 if End reload coverage is required
before continuing.

## Results

| Direction | First traversal | Reload traversal | Extra reload | Result |
| --- | --- | --- | --- | --- |
| Overworld -> Nether | OK | OK | n/a | OK |
| Nether -> Overworld | OK | OK | OK | OK |
| Overworld -> End | OK | OK | n/a | OK |
| End -> Overworld | Inconclusive in Phase119 harness | Not OK: `No nearby portal` | n/a | Needs targeted harness/debug phase |

## Log Counters

### Overworld -> Nether

- `Client Teleported Statically`: 2
- `Client Changed Dimension`: 2
- `Network Protocol Error`: 0
- `Error deserializing chunk packet`: 0
- `Ignoring incompatible vanilla chunk packet after world switch`: 0
- `No nearby portal`: 0
- `Duplicate entity UUID`: 0
- `ConcurrentModificationException`: 0
- `Buffer already closed`: 0
- `Missing program`: 0
- `UnsupportedOperationException`: 0

### Nether -> Overworld

- `Client Teleported Statically`: 3
- `Client Changed Dimension`: 3
- `Network Protocol Error`: 0
- `Error deserializing chunk packet`: 0
- `Ignoring incompatible vanilla chunk packet after world switch`: 0
- `No nearby portal`: 0
- `Duplicate entity UUID`: 0
- `ConcurrentModificationException`: 0
- `Buffer already closed`: 0
- `Missing program`: 0
- `UnsupportedOperationException`: 0

This is the direction fixed in Phase 11.8. It stayed stable across the creation
run, reload run, and one extra reload run.

### Overworld -> End

- `Client Teleported Statically`: 2
- `Client Changed Dimension`: 2
- `Network Protocol Error`: 0
- `Error deserializing chunk packet`: 0
- `Ignoring incompatible vanilla chunk packet after world switch`: 0
- `No nearby portal`: 0
- `Duplicate entity UUID`: 0
- `ConcurrentModificationException`: 0
- `Buffer already closed`: 0
- `Missing program`: 0
- `UnsupportedOperationException`: 0

### End -> Overworld

- `Network Protocol Error`: 0
- `Error deserializing chunk packet`: 0
- `ImmPtlClientChunkMap`: 0
- `Duplicate entity UUID`: 0
- `ConcurrentModificationException`: 0
- `Buffer already closed`: 0
- `Missing program`: 0
- `UnsupportedOperationException`: 0
- `No nearby portal`: observed during reload attempts.

The observed failure mode is portal selection/synchronization in the automated
End reload harness, not the Phase 11.8 chunk packet failure.

## Non-Blocking Log Noise

The logs still include known non-blocking noise:

- `IPModInfoChecking` remote info fetch 404;
- Realms auth parse messages;
- reload-position datapack metadata fallback warnings from older generated
  pack metadata in some worlds.

None of these produced a crash, `Network Protocol Error`, or chunk packet decode
failure.

## Files Produced

- `runclient-phase11.9-overworld-to-nether-reload.txt`
- `runclient-phase11.9-nether-to-overworld-reload.txt`
- `runclient-phase11.9-overworld-to-end-reload.txt`
- `runclient-phase11.9-end-to-overworld-reload.txt`

No compile was rerun because no runtime code was changed in Phase 11.9. The
Phase 11.8 compile baseline remains the active compile validation.

## Conclusion

- Overworld -> Nether reload: OK.
- Nether -> Overworld reload: OK, including an extra reload pass.
- Overworld -> End reload: OK.
- End -> Overworld reload: not revalidated by the Phase119 automated harness;
  it fails with `No nearby portal`, without network/chunk packet errors.
- `Network Protocol Error`: absent.
- `Error deserializing chunk packet`: absent.

The Phase 11.8 `ImmPtlClientChunkMap` fix does not regress the three validated
directions and keeps Nether -> Overworld stable after reload. The remaining
End -> Overworld issue should be isolated as a Phase 11.10 test harness or
client-side portal reload/sync investigation.

