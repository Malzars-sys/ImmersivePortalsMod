# Phase 14.1B - API Runtime Smoke Test

Status: completed on July 4, 2026.

## Scope

This phase validates the minimal public server-side portal API at runtime.
It does not change renderer behavior, Sodium, Iris, DimLib, AlternateDimensions,
chunk tracking, shaderpacks, or shader pipelines.

Validated baseline before this phase:

- release tag: `v7.0.0-alpha.1-mc26.1-vanilla`;
- release commit: `ce44e6c33818033292f7ef3ca8832601bd7f1fb4`;
- API implementation commit: `b6b2dc92 Add minimal public server portal API`.

## Harness Added

A development-only command was added:

```text
/imm_ptl_debug api_create_linked_test_portal
```

It is registered through the existing debug command path, which remains gated by
`FabricLoader.getInstance().isDevelopmentEnvironment()`.

The command creates a linked Overworld -> Overworld portal pair through the
public API only:

```java
PortalApi.builder(server)
    .owner(Identifier.fromNamespaceAndPath("imm_ptl", "api_smoke_test"))
    .source(world, origin)
    .target(world, destination)
    .shape(new PortalShapeSpec.Rectangle(2.0, 3.0))
    .visual(new PortalVisualOptions(true, 0xFF55FFFF, PortalVisualOptions.Style.DEBUG_CYAN))
    .teleport(PortalTeleportOptions.defaults())
    .sourceAnchorId("phase14_1b_source")
    .targetAnchorId("phase14_1b_target")
    .createLinkedPair();
```

The command does not call `Portal.ENTITY_TYPE.create(...)` or
`PortalManipulation` directly. It uses the API builder as the only creation
entry point.

An opt-in client dev flag was also added:

```text
IMM_PTL_AUTO_API_SMOKE_TEST=true
```

When enabled in a dev runtime, the client waits for the world/player connection
and sends:

```text
imm_ptl_debug api_create_linked_test_portal
```

## Persistence Logging

The public metadata persistence key is:

```text
imm_ptl_public_api
```

Runtime logs now identify:

- API metadata persistence registration;
- metadata assignment;
- metadata writes to portal NBT;
- metadata reads from portal NBT.

These logs apply only to portals carrying `imm_ptl_public_api` metadata.

## Validation Commands

Compilation:

```text
.\gradlew.bat compileJava processResources --console=plain
.\gradlew.bat build --console=plain
```

Runtime creation smoke test:

```text
IMM_PTL_AUTO_API_SMOKE_TEST=true
.\gradlew.bat runClient --args="--quickPlaySingleplayer Phase141BApiSmoke" --console=plain
```

Runtime reload smoke test:

```text
.\gradlew.bat runClient --args="--quickPlaySingleplayer Phase141BApiSmoke" --console=plain
```

The runtime commands were stopped by the automation timeout after the evidence
was collected. The client reached the world and no crash was observed.

## Creation Result

Creation log evidence:

- `PortalApi init: registering public portal metadata persistence key imm_ptl_public_api`;
- `Running dev PortalApi smoke test command`;
- `Assigned imm_ptl_public_api metadata` for both linked portals;
- `Wrote imm_ptl_public_api metadata` for both linked portals;
- `Read imm_ptl_public_api metadata` on the client side;
- `PortalApi smoke test PortalCreationResult.Success`;
- `PortalApi smoke test requested saveAllChunks after creation`;
- `Queued minimal recursive portal from PortalEntityRenderer`;
- `Rendering minimal recursive portal from GameRenderer renderLevel hook`;
- `Minimal recursive portal framebuffer texture available: true`;
- `Minimal recursive portal textured quad submitted via SubmitNodeCollector: true`.

The created portals used owner:

```text
imm_ptl:api_smoke_test
```

The handles contained stable UUIDs, dimension keys and anchor ids:

- `phase14_1b_source`;
- `phase14_1b_target`.

## Reload Result

The reload run was launched without `IMM_PTL_AUTO_API_SMOKE_TEST`.

Reload log evidence:

- both linked portals were read from `imm_ptl_public_api` on the server thread;
- both linked portals were read from `imm_ptl_public_api` on the client thread;
- the reloaded portal was collected by `PortalEntityRenderer`;
- the minimal framebuffer renderer reached the same SubmitNodeCollector path;
- the metadata was written back during save.

This confirms that API metadata survives save/reload and that API-created
portals re-enter the validated renderer path.

## Regression Scan

Observed harmful runtime failures:

- crash: 0;
- `Network Protocol Error`: 0;
- `Error deserializing chunk packet`: 0;
- `Duplicate entity UUID`: 0;
- `ConcurrentModificationException`: 0;
- `Buffer already closed`: 0;
- `UnsupportedOperationException`: 0.

Non-blocking existing noise:

- update check `IPModInfoChecking` 404;
- Realms auth parsing warning in the dev runtime;
- Java restricted native access warning from Gradle/JDK.

## Files Touched

Code:

- `src/main/java/qouteall/imm_ptl/api/PortalPersistentData.java`;
- `src/main/java/qouteall/imm_ptl/core/commands/PortalDebugCommands.java`;
- `src/main/java/qouteall/imm_ptl/core/platform_specific/IPModEntryClient.java`.

Docs:

- `docs/api/API_USAGE_14.1.md`;
- `docs/api/README.md`;
- `docs/api/PHASE14.1B_API_RUNTIME_SMOKE_TEST.md`;
- `MIGRATION_PLAN_26.1.md`.

Generated logs:

- `compile-phase14.1B-26.1.txt`;
- `build-phase14.1B-26.1.txt`;
- `runclient-phase14.1B-api-smoke-create.txt`;
- `runclient-phase14.1B-api-smoke-reload.txt`.

The generated logs are useful evidence but should not be committed unless a
maintainer explicitly wants to version runtime logs.

## Conclusion

The minimal public server-side portal API is runtime-valid for the core smoke
path:

- creates a linked portal pair;
- returns stable handles;
- writes public metadata;
- reloads public metadata;
- renders through the existing minimal portal renderer;
- does not require Sodium, Iris, DimLib or renderer internals.

Recommended next phase:

- Phase 14.2 - anchor / pentacle API and public events for third-party mods.
