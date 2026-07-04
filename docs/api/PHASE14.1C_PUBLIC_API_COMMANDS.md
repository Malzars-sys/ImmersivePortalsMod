# Phase 14.1C - Public API Commands

Status: implemented on July 4, 2026.

## Scope

This phase adds a small command harness for the minimal public server-side
portal API. The commands are not a second API surface. They are QA and
modder-facing helpers that exercise `PortalApi`, `PortalBuilder` and
`PortalHandle`.

No renderer, Sodium, Iris, DimLib, AlternateDimensions, teleportation,
framebuffer, shaderpack or chunk-tracking code was changed.

## Commands

Root command:

```text
/imm_ptl_api
```

Permission:

```text
permission level 2
```

Commands added:

```text
/imm_ptl_api create_forward [distance] [width] [height]
/imm_ptl_api create_linked_forward [distance] [width] [height]
/imm_ptl_api create_floor_linked [distance] [size]
/imm_ptl_api inspect_nearest [radius]
/imm_ptl_api list_nearby [radius]
/imm_ptl_api remove_nearest [radius]
```

Defaults:

- `distance`: `10`;
- `width`: `2`;
- `height`: `3`;
- `size`: `3`;
- inspect/remove radius: `16`;
- list radius: `32`.

## API Path

Creation commands use:

- `PortalApi.builder(server)`;
- `PortalBuilder.owner(...)`;
- `PortalBuilder.source(...)`;
- `PortalBuilder.target(...)`;
- `PortalBuilder.shape(...)`;
- `PortalBuilder.visual(...)`;
- `PortalBuilder.teleport(...)`;
- `PortalBuilder.create()`;
- `PortalBuilder.createLinkedPair()`.

Removal uses:

- `PortalApi.removePortal(server, handle, PortalRemovalReason.COMMAND)`.

The new commands do not call `Portal.ENTITY_TYPE.create(...)` directly and do
not call `PortalManipulation` directly.

Inspection commands may read nearby `Portal` entities to display QA
information. They do not mutate portal entities directly.

## Owners

Vertical command-created portals use:

```text
imm_ptl:command_api
```

Horizontal/floor command-created portals use:

```text
imm_ptl:command_api_floor
```

The floor command uses `PortalShapeSpec.HorizontalRectangle` and
`PortalVisualOptions.Style.SIMPLE_MAGIC`. This is a QA path for WHA/pentacle
style use cases. It does not imply that horizontal portal rendering is final.

## Files Modified

Code:

- `src/main/java/qouteall/imm_ptl/api/PortalRemovalReason.java`;
- `src/main/java/qouteall/imm_ptl/core/commands/PortalApiCommands.java`;
- `src/main/java/qouteall/imm_ptl/core/commands/PortalCommand.java`.

Docs:

- `docs/api/API_USAGE_14.1.md`;
- `docs/api/README.md`;
- `docs/api/PHASE14.1C_PUBLIC_API_COMMANDS.md`;
- `MIGRATION_PLAN_26.1.md`.

## Validation

Compilation:

```text
.\gradlew.bat compileJava processResources --console=plain
```

Result:

```text
BUILD SUCCESSFUL
```

Build:

```text
.\gradlew.bat build --console=plain
```

Result:

```text
BUILD SUCCESSFUL
```

Runtime:

- not executed in this phase;
- command behavior is compile-validated;
- a future runtime QA pass should run `create_linked_forward`,
  `inspect_nearest`, `list_nearby` and `remove_nearest` in a clean dev world.

## Limits

- The commands are experimental.
- They are protected by permission level 2.
- They are not intended as the preferred integration surface for mods.
- Third-party mods should call `qouteall.imm_ptl.api` directly.
- Horizontal portal QA does not mean horizontal rendering is final.
- No public events or anchor/pentacle registry are included yet.

## Next Phase

Recommended next phase:

- Phase 14.2 - anchor / pentacle API and public events for WHA-style mods.
