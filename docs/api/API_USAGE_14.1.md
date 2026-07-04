# API Usage 14.1 - Minimal Server-Side Portal API

Status: experimental.

This API is the first public server-side layer for third-party mods. It wraps
the validated internal portal creation path and avoids exposing renderer,
networking, mixin, chunk tracking, Sodium, Iris, or shaderpack internals.

Package:

```java
qouteall.imm_ptl.api
```

## Create One Portal

```java
PortalCreationResult result = PortalApi.builder(server)
    .owner(Identifier.fromNamespaceAndPath("example_mod", "ritual_portal"))
    .source(sourceLevel, sourcePos)
    .target(targetLevel, targetPos)
    .shape(new PortalShapeSpec.Rectangle(2.0, 3.0))
    .visual(PortalVisualOptions.defaults())
    .teleport(PortalTeleportOptions.defaults())
    .sourceAnchorId("source_pentacle_id")
    .targetAnchorId("target_pentacle_id")
    .create();

if (result instanceof PortalCreationResult.Success success) {
    PortalHandle handle = success.primary();
}
else if (result instanceof PortalCreationResult.Failure failure) {
    sourceLevel.getServer().sendSystemMessage(failure.reason());
}
```

## Create a Linked Pair

```java
PortalCreationResult result = PortalApi.builder(server)
    .owner(Identifier.fromNamespaceAndPath("example_mod", "ritual_portal"))
    .source(sourceLevel, sourcePos)
    .target(targetLevel, targetPos)
    .shape(new PortalShapeSpec.Rectangle(2.0, 3.0))
    .teleport(new PortalTeleportOptions(
        true,  // teleportable
        true,  // interactable
        false, // cross portal collision
        true,  // preserve entity scale
        true   // preserve gravity
    ))
    .createLinkedPair();

if (result instanceof PortalCreationResult.Success success) {
    PortalHandle primary = success.primary();
    Optional<PortalHandle> reverse = success.reverse();
}
```

## Horizontal Portal

Horizontal rectangles are available for ground/ceiling style portals, but
visual clipping is still proof-grade in the 26.1 alpha renderer.

```java
PortalCreationResult result = PortalApi.builder(server)
    .owner(Identifier.fromNamespaceAndPath("wha", "pentacle_portal"))
    .source(level, pentacleCenter)
    .target(targetLevel, targetPentacleCenter)
    .shape(new PortalShapeSpec.HorizontalRectangle(3.0, 3.0))
    .visual(new PortalVisualOptions(
        true,
        0xAA55FFFF,
        PortalVisualOptions.Style.SIMPLE_MAGIC
    ))
    .createLinkedPair();
```

## Find and Remove

```java
Optional<PortalHandle> found = PortalApi.findPortal(server, handle);

boolean removed = PortalApi.removePortal(
    server,
    handle,
    PortalRemovalReason.KILLED
);
```

## Persistent Metadata

Portals created through the API store minimal public metadata:

- API version;
- owner id;
- optional source anchor id;
- optional target anchor id;
- high-level visual options.

This metadata is stored under the portal NBT key `imm_ptl_public_api`.
It does not replace the internal portal NBT format.

## Limits

This phase does not provide:

- full pentacle/anchor registry;
- circle or custom mesh shapes;
- public rendering hooks;
- advanced clipping;
- shaderpack-specific behavior;
- Sodium/Iris APIs;
- DimLib / AlternateDimensions APIs;
- public chunk loading controls;
- teleport events.

WHA should use this as the low-level server portal creation layer. The
anchor/pentacle API is planned for Phase 14.2.

## Stability Notes

The API is intentionally conservative. It hides raw portal mutation order, sync
details, and reverse portal creation. Future phases can add events and richer
shape/visual systems without exposing current renderer internals.
