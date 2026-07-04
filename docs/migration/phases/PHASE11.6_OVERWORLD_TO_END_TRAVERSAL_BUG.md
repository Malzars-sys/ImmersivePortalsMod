# Phase 11.6 - Overworld -> End Traversal Bug

## Objective

Understand why the Phase 11.5 Overworld -> End test selected the portal and created the End client world, but never reached `Client Teleported Statically`.

Rendering, shaderpack, Sodium, Iris, DimLib, AlternateDimensions, pipelines, and fallback rendering code were not touched.

## Files Inspected

- `src/main/java/qouteall/imm_ptl/core/commands/ClientDebugCommand.java`
- `src/main/java/qouteall/imm_ptl/core/commands/PortalDebugCommands.java`
- `src/main/java/qouteall/imm_ptl/core/teleportation/ClientTeleportationManager.java`
- `runclient-phase11.5-overworld-to-end.txt`
- `runclient-phase11.5-overworld-to-nether.txt`
- `run/config/iris.properties`

## Diagnosis

The Phase 11.5 End failure was not an End-specific teleportation rejection.

The client logs showed:

- portal selected: yes;
- destination world `minecraft:the_end` created: yes;
- expected local movement: `localZ 0.25 -> -0.45`;
- actual runtime movement before fix: `localZ 0.25 -> 0.25`;
- candidates: 0;
- player falling vertically;
- final result: drowned.

The Nether control showed the expected behavior:

- reset local position: `localZ 0.25`;
- next tick: `localZ -0.20`;
- candidates: 1;
- `Client Teleported Statically`.

This proved that the End run never crossed the portal plane. The source-side setup platform was unreliable: the harness placed blocks around `(0, 80, 0)` without explicitly loading the chunk first. In the failing run, server corrections caused the player to fall instead of retaining the debug movement across the portal plane.

## Fix Applied

The Phase 11.5 harness was strengthened:

- test position moved to a dry high position:
  - player setup: `(0.5, 120.0, 0.5)`;
  - portal origin: around `y=121.5`;
- source and destination platforms are both prepared;
- chunk `(0, 0)` is explicitly loaded before platform writes;
- platform expanded and made two blocks thick;
- air pocket expanded around the portal test area.

No main teleportation logic was changed.

## Validation

### Compile

Command:

```text
.\gradlew.bat compileJava processResources --rerun-tasks --console=plain
```

Result:

- BUILD SUCCESSFUL

Log:

- `compile-phase11.6-26.1.txt`

### Overworld -> Overworld Control

World:

- `Phase116OverworldControl`

Result:

- portal created: yes;
- `Client Teleported Statically`: yes;
- crash: no.

Log:

- `runclient-phase11.6-overworld-control.txt`

### Overworld -> End

World:

- `Phase116OverworldToEndDebug`

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
- `Client World Created minecraft:the_end`: yes;
- portal selected by client traversal command: yes;
- local crossing: `localZ 0.2500 -> -0.4500`;
- candidates: 1;
- `Client Changed Dimension from minecraft:overworld to minecraft:the_end`: yes;
- `Client Teleported Statically`: yes;
- `portal_worldChanged triggerDimensionChangeTriggers minecraft:overworld -> minecraft:the_end`: yes;
- crash: no.

Log:

- `runclient-phase11.6-overworld-to-end.txt`

### End -> Overworld

World:

- `Phase116EndToOverworld`

Flags:

```text
IMM_PTL_AUTO_DIMENSION_TEST_SOURCE=minecraft:the_end
IMM_PTL_AUTO_DIMENSION_TEST_PORTAL=minecraft:overworld
IMM_PTL_AUTO_MINIMAL_TRAVERSAL_TEST=true
```

Result:

- prepared source: `minecraft:the_end`;
- portal source: `minecraft:the_end`;
- portal destination: `minecraft:overworld`;
- portal selected by client traversal command: yes;
- local crossing: `localZ 0.2500 -> -0.4500`;
- candidates: 1;
- `Client Changed Dimension from minecraft:the_end to minecraft:overworld`: yes;
- `Client Teleported Statically`: yes;
- `portal_worldChanged triggerDimensionChangeTriggers minecraft:the_end -> minecraft:overworld`: yes;
- crash: no.

Log:

- `runclient-phase11.6-end-to-overworld.txt`

## Stability Checks

Across Phase 11.6 runtime logs:

- `No nearby portal`: 0;
- `drowned`: 0 after fix;
- `fell out of the world`: 0;
- `Duplicate entity UUID`: 0;
- `ConcurrentModificationException`: 0;
- `Buffer already closed`: 0;
- `Missing program`: 0;
- `UnsupportedOperationException`: 0;
- crash: 0.

After tests:

- global `IMM_PTL_*` flags: none;
- active Phase 11.6 `runClient` process: none;
- `run/config/iris.properties`: `shaderPack=MakeUp-UltraFast-9.5c.zip`.

## Conclusion

Overworld -> End and End -> Overworld are now validated with the minimal interdimensional traversal harness.

The proven cause was the test harness platform/chunk setup, not a renderer issue and not an End-specific rejection in the core teleportation path.

## Recommended Phase 11.7

Run a save/reload regression for the validated real dimensions:

- Overworld -> Nether;
- Nether -> Overworld;
- Overworld -> End;
- End -> Overworld.

Keep it gameplay-only and avoid renderer, Sodium, Iris, DimLib, AlternateDimensions, and shaderpack fallback changes.
