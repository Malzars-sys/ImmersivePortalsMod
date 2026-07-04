# Phase 11.5 - Reliable Nether / End Test Harness

## Objective

Create a reliable development harness that can create a minimal test portal in a requested source dimension, then validate real interdimensional traversal without reopening rendering, shaderpack, Sodium, Iris, DimLib, or AlternateDimensions work.

## Files Inspected

- `src/main/java/qouteall/imm_ptl/core/platform_specific/IPModEntryClient.java`
- `src/main/java/qouteall/imm_ptl/core/commands/PortalDebugCommands.java`
- `src/main/java/qouteall/imm_ptl/core/commands/ClientDebugCommand.java`
- `src/main/java/qouteall/imm_ptl/core/teleportation/ServerTeleportationManager.java`
- `runclient-phase11.4-nether-to-overworld.txt`
- `run/config/iris.properties`

## Initial Diagnosis

The Phase 11.4 Nether -> Overworld run was inconclusive because the test harness relied on external datapack positioning and then sent:

```text
imm_ptl_debug create_dimension_test_portal minecraft:overworld
```

from the client tick hook.

The player was not reliably in `minecraft:the_nether` at the moment the server command executed, so the command created the portal in `minecraft:overworld`.

The visible symptom was:

```text
Created minimal test portal ... in minecraft:overworld
No nearby portal.
```

## Harness Fix

Added a source-aware development command:

```text
/imm_ptl_debug prepare_dimension_test <source_dimension> <destination_dimension>
```

The command:

- prepares a small platform in the source dimension;
- teleports the player to the requested source dimension using vanilla `ServerPlayer.teleportTo(...)`;
- waits 40 server ticks;
- verifies the player's real server dimension;
- creates the minimal test portal in that source dimension;
- logs the actual source and destination.

Added a client automation flag:

```text
IMM_PTL_AUTO_DIMENSION_TEST_SOURCE=<dimension>
```

When this is present together with:

```text
IMM_PTL_AUTO_DIMENSION_TEST_PORTAL=<dimension>
```

the dev automation sends:

```text
imm_ptl_debug prepare_dimension_test <source> <destination>
```

Without the source flag, the old `create_dimension_test_portal <destination>` path is preserved.

## Important Implementation Note

The first prototype used `ServerTeleportationManager.forceTeleportPlayer(...)` to place the player in the source dimension. That was rejected because it woke `ImmPtlChunkTracking.immediatelyUpdateForPlayer(...)`, which requires chunk-map mixins still isolated in the vanilla profile.

The final harness uses vanilla `ServerPlayer.teleportTo(...)` for setup only. The actual portal traversal still uses the Immersive Portals path.

## Validation

### Compile

Command:

```text
.\gradlew.bat compileJava processResources --rerun-tasks --console=plain
```

Result:

- BUILD SUCCESSFUL

Log:

- `compile-phase11.5-26.1.txt`

### A - Overworld -> Overworld Control

World:

- `Phase115OverworldControl`

Result:

- portal created: yes;
- `Client Teleported Statically`: yes;
- crash: no.

Log:

- `runclient-phase11.5-overworld-control.txt`

### B - Overworld -> Nether

World:

- `Phase115OverworldToNether`

Flags:

```text
IMM_PTL_AUTO_DIMENSION_TEST_SOURCE=minecraft:overworld
IMM_PTL_AUTO_DIMENSION_TEST_PORTAL=minecraft:the_nether
IMM_PTL_AUTO_MINIMAL_TRAVERSAL_TEST=true
```

Result:

- prepared source: `minecraft:overworld`;
- portal source: `minecraft:overworld`;
- portal destination: `minecraft:the_nether`;
- `Client World Created minecraft:the_nether`: yes;
- `Client Changed Dimension from minecraft:overworld to minecraft:the_nether`: yes;
- `Client Teleported Statically`: yes;
- `portal_worldChanged stored enteredNetherPosition`: yes;
- `portal_worldChanged triggerDimensionChangeTriggers minecraft:overworld -> minecraft:the_nether`: yes;
- crash: no.

Log:

- `runclient-phase11.5-overworld-to-nether.txt`

### C - Nether -> Overworld

World:

- `Phase115NetherToOverworld`

Flags:

```text
IMM_PTL_AUTO_DIMENSION_TEST_SOURCE=minecraft:the_nether
IMM_PTL_AUTO_DIMENSION_TEST_PORTAL=minecraft:overworld
IMM_PTL_AUTO_MINIMAL_TRAVERSAL_TEST=true
```

Result:

- prepared source: `minecraft:the_nether`;
- portal source: `minecraft:the_nether`;
- portal destination: `minecraft:overworld`;
- `Client Changed Dimension from minecraft:the_nether to minecraft:overworld`: yes;
- `Client Teleported Statically`: yes;
- `portal_worldChanged triggerDimensionChangeTriggers minecraft:the_nether -> minecraft:overworld`: yes;
- `No nearby portal`: 0;
- crash: no.

Log:

- `runclient-phase11.5-nether-to-overworld.txt`

### D - Overworld -> End

World:

- `Phase115OverworldToEnd`

Flags:

```text
IMM_PTL_AUTO_DIMENSION_TEST_SOURCE=minecraft:overworld
IMM_PTL_AUTO_DIMENSION_TEST_PORTAL=minecraft:the_end
IMM_PTL_AUTO_MINIMAL_TRAVERSAL_TEST=true
```

Result:

- prepared source: `minecraft:overworld`;
- portal source: `minecraft:overworld`;
- portal destination: `minecraft:the_end`;
- client world for `minecraft:the_end` created: yes;
- portal selected by `test_minimal_portal_traversal`: yes;
- final dimension change: no;
- `Client Teleported Statically`: no;
- player drowned before successful traversal;
- crash: no.

This is now a real gameplay/test issue, not a false source-dimension harness issue.

### E - End -> Overworld

Not tested because D did not succeed.

## Stability Checks

Across the successful Phase 11.5 runtime tests:

- `Duplicate entity UUID`: 0;
- `ConcurrentModificationException`: 0;
- `Buffer already closed`: 0;
- `Missing program`: 0;
- `UnsupportedOperationException`: 0;
- crash: 0.

After tests:

- global `IMM_PTL_*` flags: none;
- active Phase 11.5 `runClient` process: none;
- `run/config/iris.properties`: `shaderPack=MakeUp-UltraFast-9.5c.zip`.

## Conclusion

The Nether / Overworld harness is now reliable.

The previous Nether -> Overworld false failure is fixed: the portal is created in the real Nether source dimension and traversal succeeds.

End traversal remains unresolved. The new harness reaches the point where an Overworld -> End portal is created, loaded, and selected, but the minimal traversal does not complete before the player drowns. This should be handled as a separate gameplay traversal bug, not as a harness-source bug.

## Recommended Phase 11.6

Target Overworld -> End traversal specifically:

- inspect why the selected End destination portal does not trigger static teleport;
- compare local portal crossing logs between Nether and End;
- avoid renderer, shaderpack, Sodium, Iris, DimLib, and AlternateDimensions changes.
