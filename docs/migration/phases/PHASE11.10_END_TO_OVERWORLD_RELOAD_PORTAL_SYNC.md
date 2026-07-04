# Phase 11.10 - End -> Overworld Reload Portal Sync

## Scope

This phase diagnosed the Phase 11.9 `End -> Overworld` reload failure where
`/imm_ptl_client_debug test_minimal_portal_traversal` returned `No nearby
portal`.

No renderer, shaderpack, Sodium, Iris, DimLib, AlternateDimensions, global
chunk sync, or advanced chunk tracking code was changed.

## Initial Findings

The Phase 11.9 failure was not a chunk packet regression:

- `Network Protocol Error`: 0
- `Error deserializing chunk packet`: 0
- crash: 0

The client command returned `No nearby portal`, which means
`IPMcHelper.getNearbyPortals(player, 64)` found no portal in the client player's
current world.

## Instrumentation Added

Two dev-only/debug changes were added.

### Delayed Save For Minimal Test Portal

File:

- `src/main/java/qouteall/imm_ptl/core/commands/PortalDebugCommands.java`

New opt-in flags:

- `IMM_PTL_SAVE_AFTER_MINIMAL_TEST_PORTAL=true`
- `IMM_PTL_SAVE_AFTER_MINIMAL_TEST_PORTAL_DELAY_TICKS=<ticks>`

When enabled, the dev minimal portal command schedules:

```text
server.saveAllChunks(true, true, false)
```

after the configured delay. This is intentionally limited to the development
test portal harness.

### No Nearby Portal Diagnostics

File:

- `src/main/java/qouteall/imm_ptl/core/commands/ClientDebugCommand.java`

When `test_minimal_portal_traversal` finds no portal, it logs:

- client player dimension;
- client player position;
- portal counts per loaded client world;
- nearest portal in the current client world when available.

The diagnostic is tolerant of `ClientWorldLoader` not being initialized yet and
falls back to the current client level instead of failing the command.

## Reproduction And Diagnosis

World:

- `Phase1110EndToOverworldReloadDebug`

Direction:

- `minecraft:the_end -> minecraft:overworld`

The decisive failing diagnostic showed:

```text
Minimal traversal found no nearby portal
client player dimension: minecraft:the_end
client world minecraft:the_end entityListPortals=0 renderingPortals=0 nearestInCurrentWorld=none
```

This proves the player was in the End on the client, but the normal portal
entity was not present client-side. It was not a distance issue.

The stable run used a delayed forced save after creation:

```text
IMM_PTL_SAVE_AFTER_MINIMAL_TEST_PORTAL=true
IMM_PTL_SAVE_AFTER_MINIMAL_TEST_PORTAL_DELAY_TICKS=100
IMM_PTL_AUTO_MINIMAL_TRAVERSAL_DELAY_TICKS=260
```

That order gives the portal time to sync, then forces the server save before
the scripted run is terminated.

## Final Runtime Validation

Log:

- `runclient-phase11.10-end-to-overworld-reload.txt`

Key final lines:

```text
Created minimal test portal ... in minecraft:the_end targeting ... in minecraft:overworld
Minimal traversal debug command selected portal Portal{411,...}
Client Changed Dimension from minecraft:the_end to minecraft:overworld
Client Teleported Statically
Saving all chunks after minimal test portal creation ...
ThreadedAnvilChunkStorage: All dimensions are saved
Phase 11.10 reload positioning into minecraft:the_end
Phase 11.10 reload source position verified in minecraft:the_end
Minimal traversal debug command selected portal Portal{75,...}
Client Changed Dimension from minecraft:the_end to minecraft:overworld
Client Teleported Statically
```

Counters:

- `Client Teleported Statically`: 2
- `Client Changed Dimension`: 2
- `No nearby portal`: 0
- `Network Protocol Error`: 0
- `Error deserializing chunk packet`: 0
- `Duplicate entity UUID`: 0
- `ConcurrentModificationException`: 0
- `Buffer already closed`: 0
- `Missing program`: 0
- `UnsupportedOperationException`: 0
- client command error: 0

## Mini Regression

Because the fix touches the common debug harness, a quick
`Overworld -> End` reload check was run.

Log:

- `runclient-phase11.10-overworld-to-end-reload.txt`

Result:

- `Client Teleported Statically`: 1
- `Client Changed Dimension`: 1
- `No nearby portal`: 0
- `Network Protocol Error`: 0
- `Error deserializing chunk packet`: 0

## Compile Validation

Log:

- `compile-phase11.10-26.1.txt`

Result:

- `compileJava processResources`: BUILD SUCCESSFUL

## Cause

The Phase 11.9 `End -> Overworld` reload failure was caused by the automated
test harness terminating the run without a reliable save point for the newly
created normal portal entity in the End.

The portal could be created and traversed, but on reload the client had no
portal entity in `minecraft:the_end`, causing `No nearby portal`.

## Fix

The runtime portal system was not changed.

The fix is a development-harness-only save hook for minimal test portals:

- opt-in via `IMM_PTL_SAVE_AFTER_MINIMAL_TEST_PORTAL=true`;
- delayed via `IMM_PTL_SAVE_AFTER_MINIMAL_TEST_PORTAL_DELAY_TICKS`;
- preserves normal runtime behavior by default.

## Conclusion

- End -> Overworld first traversal: OK.
- End -> Overworld reload without recreating portal: OK.
- `No nearby portal`: absent in final run.
- `Network Protocol Error`: absent.
- `Error deserializing chunk packet`: absent.
- Renderer/Sodium/Iris/DimLib untouched.

Recommended next phase: return to the broader dimension reload regression matrix
or start a focused long-term save/reload soak test using the delayed save flag
for automated clean-world portal creation.

