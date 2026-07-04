# Phase 14.1 - Minimal Server-Side Portal API

Date: 2026-07-04

Baseline:

- Release tag: `v7.0.0-alpha.1-mc26.1-vanilla`
- Release commit: `ce44e6c33818033292f7ef3ca8832601bd7f1fb4`
- API design: `docs/api/API_DESIGN_14.0.md`

## Scope

This phase adds a minimal public server-side API in `qouteall.imm_ptl.api`.
It does not change renderer internals, Sodium, Iris, DimLib,
AlternateDimensions, shaderpacks, chunk tracking, or teleportation logic.

## Files Created

- `src/main/java/qouteall/imm_ptl/api/PortalApi.java`
- `src/main/java/qouteall/imm_ptl/api/PortalBuilder.java`
- `src/main/java/qouteall/imm_ptl/api/PortalHandle.java`
- `src/main/java/qouteall/imm_ptl/api/PortalShapeSpec.java`
- `src/main/java/qouteall/imm_ptl/api/PortalVisualOptions.java`
- `src/main/java/qouteall/imm_ptl/api/PortalTeleportOptions.java`
- `src/main/java/qouteall/imm_ptl/api/PortalCreationResult.java`
- `src/main/java/qouteall/imm_ptl/api/PortalRemovalReason.java`
- `src/main/java/qouteall/imm_ptl/api/PortalPersistentData.java`
- `docs/api/API_USAGE_14.1.md`
- `docs/api/PHASE14.1_MINIMAL_SERVER_PORTAL_API.md`

## Files Modified

- `src/main/java/qouteall/imm_ptl/core/IPModMain.java`
  - calls `PortalApi.init()` so public API metadata can persist through the
    existing portal NBT read/write signals.
- `docs/api/README.md`
- `MIGRATION_PLAN_26.1.md`

## Public API Added

Minimal creation and management:

- `PortalApi.builder(MinecraftServer)`
- `PortalApi.findPortal(ServerLevel, UUID)`
- `PortalApi.findPortal(MinecraftServer, PortalHandle)`
- `PortalApi.removePortal(MinecraftServer, PortalHandle, PortalRemovalReason)`

Builder:

- `owner(Identifier)`
- `source(ServerLevel, Vec3)`
- `target(ServerLevel, Vec3)`
- `shape(PortalShapeSpec)`
- `visual(PortalVisualOptions)`
- `teleport(PortalTeleportOptions)`
- `sourceAnchorId(String)`
- `targetAnchorId(String)`
- `create()`
- `createLinkedPair()`

Value objects:

- `PortalHandle`
- `PortalShapeSpec.Rectangle`
- `PortalShapeSpec.HorizontalRectangle`
- `PortalShapeSpec.ExplicitAxes`
- `PortalVisualOptions`
- `PortalTeleportOptions`
- `PortalCreationResult.Success`
- `PortalCreationResult.Failure`
- `PortalRemovalReason`
- `PortalPersistentData`

## Internal Path Used

The builder reuses the validated portal runtime path:

- `Portal.ENTITY_TYPE.create(...)`
- `Portal#setOriginPos(...)`
- `Portal#setDestinationDimension(...)`
- `Portal#setDestination(...)`
- `Portal#setOrientationAndSize(...)`
- existing portal teleport/interact/collision fields
- `McHelper.spawnServerEntity(...)`
- `PortalManipulation.createReversePortal(...)` for linked pairs
- existing portal sync behavior
- existing `Portal.READ_PORTAL_DATA_SIGNAL` and
  `Portal.WRITE_PORTAL_DATA_SIGNAL` for public metadata

## Persistence

API-created portals store minimal public metadata in portal NBT under:

```text
imm_ptl_public_api
```

Stored fields:

- `apiVersion`
- `owner`
- optional `sourceAnchorId`
- optional `targetAnchorId`
- `visible`
- `frameColorArgb`
- `style`

The internal portal save format is not replaced.

## Validation

- `compileJava processResources`: BUILD SUCCESSFUL
- `build`: BUILD SUCCESSFUL

## Runtime Test

No `runClient` test was added in this phase. A dev command that exercises the
public API can be added later, but the API itself is not a command surface.

## Limits

Not included yet:

- WHA pentacle anchor registry;
- teleport events;
- public callbacks;
- circle/custom shapes;
- custom visual renderer;
- shaderpack-specific behavior;
- Sodium/Iris integration;
- DimLib / AlternateDimensions;
- public chunk loading controls.

`PortalVisualOptions` stores high-level intent and applies only the existing
visibility switch for now.

## Risks

- Horizontal portals need better visual QA before being presented as polished.
- Visual options are intentionally metadata-first and may not visibly affect the
  current minimal renderer.
- Public metadata is registered through common init; saves created before this
  API simply do not contain the new metadata.

## Next Phase

Phase 14.2 should add anchor/pentacle-oriented concepts and events:

- `PortalAnchor`
- `PortalAnchorId`
- `PortalLinkApi`
- `PortalEvents`
- `PortalTeleportContext`

That is the layer WHA should eventually use directly.
