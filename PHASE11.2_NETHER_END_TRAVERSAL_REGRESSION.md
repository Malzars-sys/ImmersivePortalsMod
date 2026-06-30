# Phase 11.2 - Nether / End Traversal Regression

## Scope

This phase tested real vanilla dimensions after the Phase 10 shaderpack baseline freeze and Phase 11 documentation work.

Strictly not touched:

- renderer pipelines;
- shaderpack fallback defaults;
- Sodium / Iris compatibility paths;
- DimLib / AlternateDimensions;
- shader clipping / stencil / framebuffer advanced paths.

## Repository And Environment Checks

- Current branch context: `1.21`.
- Required commits observed:
  - `5c4b7dcf Freeze Phase 10 shaderpack rendering baseline`
  - `cf52c2b9 Document post shaderpack freeze decision audit`
  - `2a94e584 Document shaderpack fallbacks and dev flags`
- `run/config/iris.properties` remained on:
  - `shaderPack=MakeUp-UltraFast-9.5c.zip`
- No global `IMM_PTL_*` variables remained active after the tests.
- No Phase 10.x / 11.x `runClient` process remained active after the tests.

## Test Harness Changes

The existing `/portal make_portal` command was too brittle for automated dimension tests because it depends on the player raycast and a valid block hit. A development-only command was added instead:

```text
/imm_ptl_debug create_dimension_test_portal <dimension>
```

It reuses the minimal test portal path and only changes the destination dimension.

For interdimensional startup timing, a development-only delay override was also added:

```text
IMM_PTL_AUTO_MINIMAL_TRAVERSAL_DELAY_TICKS=<ticks>
```

Default behavior is unchanged when the flag is absent.

## Validation

Compilation:

```text
compileJava processResources: BUILD SUCCESSFUL
```

Log file:

```text
compile-phase11.2-26.1.txt
```

## Runtime Tests

All tests used clean Phase112 worlds generated under `run/saves` and vanilla runtime.

### A - Overworld -> Nether

World:

```text
Phase112OverworldToNether
```

Result:

- server portal creation: yes;
- client destination world creation: yes, `minecraft:the_nether`;
- portal present client-side: yes, after using delayed traversal;
- traversal command accepted: yes;
- final dimension traversal marker: no.

Key log:

```text
Created minimal test portal at (0.5, 81.5, 4.5) targeting (0.5, 81.5, 14.5) in minecraft:the_nether
Client World Created minecraft:the_nether
Testing minimal traversal through Portal{..., (minecraft:overworld ...)->(minecraft:the_nether ...)}
```

No `Client Teleported Statically` marker appeared during the long wait.

### B - Nether -> Overworld

World:

```text
Phase112NetherToOverworld
```

Result:

- server portal creation: yes;
- client destination world creation: yes, `minecraft:overworld`;
- portal present client-side: yes;
- traversal command accepted: yes;
- final dimension traversal marker: not observed.

### C - Overworld -> End

World:

```text
Phase112OverworldToEnd
```

Result:

- server portal creation: yes;
- client destination world creation: yes, `minecraft:the_end`;
- portal present client-side: yes;
- traversal command accepted: yes;
- final dimension traversal marker: not observed.

The log includes the debug-command positioning step:

```text
Testing minimal traversal through Portal{..., (minecraft:overworld ...)->(minecraft:the_end ...)}
literal{Player...} ResourceKey[minecraft:dimension / minecraft:overworld] teleported from ... to ...
```

This is the setup teleport used by the client debug command, not confirmed final portal traversal.

### D - End -> Overworld

World:

```text
Phase112EndToOverworld
```

Result:

- server portal creation: yes;
- client destination world creation: yes, `minecraft:overworld`;
- portal present client-side: yes;
- traversal command accepted: yes;
- final dimension traversal marker: not observed.

## Stability Checks

Across the Phase 11.2 runs:

- crash: 0 observed;
- `Duplicate entity UUID`: 0 observed;
- `ConcurrentModificationException`: 0 observed;
- `Buffer already closed`: 0 observed;
- `Missing program`: 0 observed;
- `UnsupportedOperationException`: 0 observed.

## Save / Reload

A minimal reload attempt showed the Phase112 datapack state can persist, but the forced process shutdown used by automated runs is not a clean save/quit workflow. Because the final interdimensional traversal did not complete, save/reload of completed Nether/End traversal remains not validated.

## Conclusion

Nether / End portal creation and client-side portal availability are mostly working with the new delayed test harness.

Actual interdimensional traversal is not validated and appears blocked after the client debug command places the player at the portal start position. The expected final marker:

```text
Client Teleported Statically
```

was not observed.

Status:

- Nether OK: partial, portal created and found, traversal not completed.
- End OK: partial, portal created and found, traversal not completed.
- Save/reload minimal OK: not validated for completed interdimensional traversal.

## Recommended Phase 11.3

Open a focused gameplay bug phase:

- inspect `ClientTeleportationManager`;
- inspect server collision / portal crossing detection for cross-dimension portals;
- inspect whether the debug traversal velocity is insufficient or applied in the wrong dimension;
- add a stronger interdimensional traversal diagnostic if needed;
- keep renderer, shaderpack, Sodium, Iris, DimLib and AlternateDimensions untouched.

