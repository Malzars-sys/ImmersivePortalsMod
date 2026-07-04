# API Design 14.0 - Public Immersive Portals API for Third-Party Mods

Date: 2026-07-04

Baseline:

- Branch: `1.21`
- Release tag: `v7.0.0-alpha.1-mc26.1-vanilla`
- Release commit: `ce44e6c33818033292f7ef3ca8832601bd7f1fb4`

## Goal

Immersive Portals should become a stable base that other mods can build on,
not only a standalone portal mod. The public API should let a mod create,
link, persist, render, and observe portals without depending on internal
rendering, networking, chunk tracking, mixins, or debug commands.

This phase is design-only. It does not change runtime code.

## Target Use Case: WHA / Witch Hate Atelier

WHA wants to:

- draw a magic pentacle on the ground;
- create a horizontal or vertical portal from that pentacle;
- link it to another matching pentacle;
- choose the destination dimension and position;
- choose the shape;
- choose simple visual parameters;
- open and close the portal by ritual;
- preserve the link after save/reload;
- receive callbacks when an entity crosses the portal.

This is a good stress case because it needs both stable server-side portal
semantics and high-level visual options, while avoiding renderer internals.

## Current Internal Surface Audited

Classes inspected:

- `qouteall.imm_ptl.core.api.PortalAPI`
- `qouteall.imm_ptl.core.portal.Portal`
- `qouteall.imm_ptl.core.portal.PortalState`
- `qouteall.imm_ptl.core.portal.PortalExtension`
- `qouteall.imm_ptl.core.portal.PortalManipulation`
- `qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage`
- `qouteall.imm_ptl.core.teleportation.ServerTeleportationManager`
- `qouteall.imm_ptl.core.teleportation.ClientTeleportationManager`
- `qouteall.imm_ptl.core.teleportation.TeleportationUtil`
- `qouteall.imm_ptl.core.McHelper`
- `qouteall.imm_ptl.core.commands.PortalDebugCommands`
- `qouteall.imm_ptl.core.commands.PortalCommand`
- `qouteall.imm_ptl.core.portal.shape.PortalShape`
- `qouteall.imm_ptl.core.portal.shape.RectangularPortalShape`
- `qouteall.imm_ptl.core.portal.shape.SpecialFlatPortalShape`
- `qouteall.imm_ptl.core.portal.shape.BoxPortalShape`

## What Already Exists

The current `PortalAPI` already exposes useful building blocks:

- set portal position/orientation/size;
- set destination dimension, position, rotation, and scale;
- create reverse/flipped/copied portals;
- add/remove global portals;
- add/remove chunk loaders;
- teleport an entity;
- send redirected packets to entity trackers;
- convert dimensions to/from integer ids.

However, this is not yet a safe public mod API. It exposes raw `Portal`
entities and low-level implementation choices. Mods must know the exact order
of mutation, when to call sync, which entity type to create, and how persistence
is handled.

## Public API Principles

1. **Stable objects in, stable handles out.**
   Third-party mods should work with value objects, handles, and events, not
   raw renderer or network internals.

2. **Builder before mutation.**
   The public path should validate the portal definition before spawning.

3. **Server first.**
   Creation, removal, linking, persistence, and teleport decisions belong on
   the logical server.

4. **High-level rendering options only.**
   Public API can expose color, visibility, simple style, and custom client
   decoration hooks later. It must not expose framebuffer, `SubmitNodeCollector`,
   shaderpack, Sodium/Iris, or stencil details.

5. **Explicit experimental boundaries.**
   Anything depending on incomplete clipping, shaderpacks, DimLib, or advanced
   recursion should remain internal/experimental.

## Proposed Public Package

Package root:

```text
qouteall.imm_ptl.api
```

Suggested types:

- `PortalApi`
- `PortalBuilder`
- `PortalHandle`
- `PortalLinkApi`
- `PortalAnchor`
- `PortalAnchorId`
- `PortalShapeSpec`
- `PortalVisualOptions`
- `PortalTeleportOptions`
- `PortalAccessPolicy`
- `PortalEvents`
- `PortalPersistenceApi`
- `PortalQuery`
- `PortalCreationResult`

Keep the existing `qouteall.imm_ptl.core.api.PortalAPI` for compatibility, but
consider marking it as lower-level/internal-adjacent after the new API exists.

## Minimal Useful API for WHA

The first useful API does not need custom renderers. It needs:

- create one portal from an anchor to another anchor;
- create a linked pair;
- remove by handle/id;
- find by id after reload;
- set high-level visual options;
- receive teleport callbacks;
- persist enough owner/link metadata to restore the relation.

### Core Value Types

Pseudo-code:

```java
public record PortalAnchorId(String namespace, String path) {}

public record PortalAnchor(
    PortalAnchorId id,
    ResourceKey<Level> dimension,
    Vec3 position,
    DQuaternion orientation,
    PortalShapeSpec shape,
    CompoundTag data
) {}

public sealed interface PortalShapeSpec {
    record Rectangle(double width, double height) implements PortalShapeSpec {}
    record HorizontalRectangle(double width, double height) implements PortalShapeSpec {}
    record Circle(double radius, int segments) implements PortalShapeSpec {}
}

public record PortalVisualOptions(
    int frameColorArgb,
    boolean visible,
    PortalVisualStyle style,
    Optional<Identifier> decorativeTexture
) {}

public enum PortalVisualStyle {
    DEFAULT,
    DEBUG_CYAN,
    INVISIBLE,
    SIMPLE_MAGIC
}

public record PortalTeleportOptions(
    boolean teleportable,
    boolean crossPortalCollision,
    boolean interactable,
    boolean preserveEntityScale,
    boolean preserveGravity,
    PortalAccessPolicy accessPolicy
) {}
```

### Handles

Mods should not store raw `Portal` references long-term. They should store a
stable handle.

```java
public record PortalHandle(
    UUID entityId,
    ResourceKey<Level> dimension,
    Identifier owner,
    @Nullable PortalAnchorId sourceAnchor,
    @Nullable PortalAnchorId targetAnchor
) {}
```

The implementation can resolve a handle to a live `Portal` when needed.

## Creation API

Simple creation:

```java
PortalCreationResult createPortal(
    ServerLevel sourceLevel,
    Vec3 sourcePosition,
    ServerLevel destinationLevel,
    Vec3 destinationPosition,
    PortalShapeSpec shape,
    PortalVisualOptions visualOptions,
    PortalTeleportOptions teleportOptions
);
```

Anchor-based creation:

```java
PortalCreationResult createPortal(
    PortalAnchor sourceAnchor,
    PortalAnchor targetAnchor,
    PortalCreationOptions options
);
```

Linked pair:

```java
PortalLink createLinkedPortalPair(
    PortalAnchor sourceAnchor,
    PortalAnchor targetAnchor,
    PortalCreationOptions options
);
```

Suggested result type:

```java
public sealed interface PortalCreationResult {
    record Success(PortalHandle primary, Optional<PortalHandle> reverse) implements PortalCreationResult {}
    record Failure(Component reason) implements PortalCreationResult {}
}
```

## Portal Builder

Builder API should validate required fields:

```java
PortalHandle handle = PortalApi.builder(server)
    .owner(new Identifier("wha", "ritual_portal"))
    .source(sourceAnchor)
    .target(targetAnchor)
    .shape(PortalShapeSpec.horizontalRectangle(3.0, 3.0))
    .visual(PortalVisualOptions.simpleMagic(0xFF55FFFF))
    .teleport(PortalTeleportOptions.defaults().crossPortalCollision(false))
    .createLinkedPair()
    .orThrow();
```

Internally this maps to:

- `Portal.ENTITY_TYPE.create(...)`;
- `portal.setOriginPos(...)`;
- `portal.setDestinationDimension(...)`;
- `portal.setDestination(...)`;
- `portal.setOrientationAndSize(...)`;
- selected `PortalShape`;
- selected `PortalExtension` fields;
- `McHelper.spawnServerEntity(...)`;
- optional reverse portal through `PortalManipulation.createReversePortal(...)`;
- optional link ids stored in `PortalExtension` or a new public metadata
  component.

The public builder hides those details.

## Anchor-Based API for WHA

A pentacle should be modelled as an anchor, not as a portal.

Anchor fields:

- stable id, e.g. `wha:pentacle/<uuid>`;
- dimension;
- position;
- orientation;
- ritual type;
- shape;
- owner mod id;
- optional NBT/component data;
- optional block or structure attachment.

API:

```java
PortalAnchorId registerAnchor(PortalAnchor anchor);
Optional<PortalAnchor> findAnchor(PortalAnchorId id);
void removeAnchor(PortalAnchorId id, AnchorRemovalPolicy policy);
PortalLink linkAnchors(PortalAnchorId source, PortalAnchorId target, PortalCreationOptions options);
void unlinkAnchors(PortalAnchorId source, PortalAnchorId target);
```

If an anchor disappears:

- close portals automatically;
- or leave portals dormant;
- or call `portalLinkBroken` and let the owner decide.

Recommended first implementation: store anchor metadata inside portal persistent
metadata and let the owner mod persist its own block/entity anchor registry.
Immersive Portals should not try to own every mod's ritual structure data.

## Event API

Use Fabric `Event` style. Suggested events:

```java
public final class PortalEvents {
    public static final Event<BeforePortalCreated> BEFORE_PORTAL_CREATED;
    public static final Event<AfterPortalCreated> AFTER_PORTAL_CREATED;
    public static final Event<BeforeEntityTeleport> BEFORE_ENTITY_TELEPORT;
    public static final Event<AfterEntityTeleport> AFTER_ENTITY_TELEPORT;
    public static final Event<BeforePortalClosed> BEFORE_PORTAL_CLOSED;
    public static final Event<PortalLinkBroken> PORTAL_LINK_BROKEN;
    public static final Event<PortalLoadedFromSave> PORTAL_LOADED_FROM_SAVE;
}
```

Event semantics:

- `BEFORE_PORTAL_CREATED`: may deny or modify high-level creation options.
- `AFTER_PORTAL_CREATED`: notification only; portal already exists.
- `BEFORE_ENTITY_TELEPORT`: may deny teleportation or adjust target if safe.
- `AFTER_ENTITY_TELEPORT`: notification after server-side teleport completion.
- `BEFORE_PORTAL_CLOSED`: may cancel only if removal reason allows.
- `PORTAL_LINK_BROKEN`: notification when reverse/anchor target disappears.
- `PORTAL_LOADED_FROM_SAVE`: notification after persistent metadata is read.

Do not expose `TeleportationUtil.Teleportation` directly in the public API. It is
too detailed and tied to current collision internals. Provide a public
`PortalTeleportContext` with stable fields:

- entity;
- portal handle;
- source dimension;
- source position;
- destination dimension;
- destination position;
- local portal hit position;
- teleport reason.

## Shape API

Phase 14.1 should support only stable shapes:

- vertical rectangle;
- horizontal rectangle;
- arbitrary rectangle from explicit axes.

Phase 14.2 can add:

- circle approximated by mesh/shape data;
- polygonal planar shape;
- block/structure-attached portal.

Do not expose internal `PortalShape` immediately. Its methods include collision,
frustum, clipping, mesh output, and renderer-facing behavior. Instead expose
`PortalShapeSpec` and translate internally.

For future custom shapes:

```java
public interface PublicPortalShape {
    PortalShapeBounds bounds();
    boolean containsLocalPoint(double x, double y);
    List<Vec2> outline();
}
```

Keep rendering/clipping hooks out of the initial custom-shape contract.

## Visual API

Expose high-level visual choices:

- frame color;
- visible/invisible;
- debug frame;
- simple magic frame;
- optional decorative texture identifier;
- optional client callback later for owner-side decoration.

Do not expose:

- `RendererUsingFrameBuffer`;
- `IPRenderPipelines`;
- framebuffer texture aliases;
- `SubmitNodeCollector`;
- depth modes;
- shaderpack fallback modes;
- Sodium/Iris pipeline overrides.

The current minimal renderer is proof-grade and should remain an implementation
detail. A public magic portal should be able to request "simple magic" visuals,
but Immersive Portals decides how to render that per profile.

## Persistence API

Needs:

- stable portal id;
- stable anchor id;
- owner mod id;
- source/target anchor references;
- shape spec;
- visual options;
- teleport options;
- link policy;
- versioned metadata.

Suggested public metadata:

```java
public record PortalPersistentData(
    int apiVersion,
    Identifier owner,
    PortalAnchorId sourceAnchor,
    PortalAnchorId targetAnchor,
    CompoundTag ownerData
) {}
```

Implementation options:

1. Store public metadata in `Portal` NBT through existing
   `READ_PORTAL_DATA_SIGNAL` / `WRITE_PORTAL_DATA_SIGNAL`.
2. Add a dedicated `PortalPublicMetadata` component-like wrapper later.
3. Keep anchor registries in owner mods, with Immersive Portals storing only
   stable references.

Avoid making `GlobalPortalStorage` the public persistence API. It is specific
to global portals and includes unrelated dimension-stack state.

## Removal and Query API

```java
Optional<PortalHandle> findPortal(UUID entityId, ResourceKey<Level> dimension);
List<PortalHandle> findPortals(PortalQuery query);
boolean removePortal(PortalHandle handle, PortalRemovalReason reason);
int removePortals(PortalQuery query, PortalRemovalReason reason);
```

`PortalQuery` should support:

- owner mod id;
- source anchor;
- target anchor;
- dimension;
- bounding box;
- tag.

Do not require mods to scan raw entities or global portal storage.

## Access Control

Current `Portal` has `specificPlayerId`, `teleportable`, `interactable`, and
`crossPortalCollisionEnabled`.

Public API should lift these into:

```java
public sealed interface PortalAccessPolicy {
    record Everyone() implements PortalAccessPolicy {}
    record OnlyPlayer(UUID playerId) implements PortalAccessPolicy {}
    record OnlyEntities() implements PortalAccessPolicy {}
    record OwnerCallback(Identifier owner, String policyId) implements PortalAccessPolicy {}
}
```

`OwnerCallback` should be delayed until event cancellation semantics are stable.

## What Can Become Public Now

Good candidates:

- high-level portal creation;
- high-level linked pair creation;
- basic rectangle/horizontal rectangle shapes;
- visual visibility/frame color/style options;
- teleportable/interactable/collision options;
- stable handles;
- query/remove by handle;
- save/reload metadata;
- after-create / after-close / after-teleport events.

## What Should Stay Internal

Do not expose directly:

- `RendererUsingFrameBuffer`;
- `RendererUsingStencil`;
- `IPRenderPipelines`;
- framebuffer/depth/stencil/shaderpack modes;
- `SubmitNodeCollector` bridge;
- `ImmPtlClientChunkMap` stale-packet recovery;
- packet redirection internals;
- `ServerTeleportationManager` low-level methods;
- `ClientTeleportationManager`;
- mixin duck interfaces;
- dev commands;
- Sodium/Iris compatibility classes;
- `GlobalPortalStorage` internals;
- raw `PortalShape` renderer/collision methods;
- `commandsOnTeleported` as a primary public callback mechanism.

## Risks

API too broad:

- locks unstable renderer internals into public compatibility;
- makes Sodium/Iris/DimLib refactors harder;
- exposes teleportation details that may need further fixes;
- encourages mods to mutate raw portals unsafely;
- creates persistence obligations before metadata versioning is ready.

API too narrow:

- WHA and similar mods will copy debug-command code;
- mods will store raw entity ids without reload safety;
- no stable event layer for rituals/triggers;
- no clear migration path toward polished rendering.

Rendering risks:

- clipping remains incomplete;
- horizontal portals need better occlusion than vertical debug portals;
- shaderpack behavior is fallback-based;
- advanced recursion is not restored.

Dependencies to postpone:

- Sodium/Iris-specific visual APIs;
- DimLib / dynamic dimensions;
- custom shader clipping;
- custom portal mesh rendering;
- full public chunk-loading controls.

## Proposed Implementation Plan

### Phase 14.1 - Minimal Server-Side Portal API

Implement:

- `qouteall.imm_ptl.api.PortalApi`
- `PortalBuilder`
- `PortalHandle`
- `PortalShapeSpec.Rectangle`
- `PortalShapeSpec.HorizontalRectangle`
- `PortalVisualOptions` with minimal fields
- `PortalTeleportOptions`
- `PortalCreationResult`

Capabilities:

- create one portal;
- create reverse linked pair;
- remove by handle;
- find by handle;
- persist owner/source/target metadata.

No custom renderer. No Sodium/Iris/DimLib.

### Phase 14.2 - Events and WHA Anchor Layer

Implement:

- `PortalAnchor`
- `PortalAnchorId`
- `PortalLinkApi`
- `PortalEvents`
- `PortalTeleportContext`

Capabilities:

- create portal from anchors;
- link/unlink anchors;
- callbacks for create/close/teleport/load.

### Phase 14.3 - Public Query and Persistence Hardening

Implement:

- `PortalQuery`
- versioned metadata;
- reload tests;
- owner mod id filtering;
- safe cleanup when anchor target disappears.

### Phase 14.4 - Visual Options Stabilization

Implement only high-level visuals:

- frame color;
- invisible;
- debug cyan;
- simple magic style.

Do not expose renderer internals.

### Phase 14.5 - Horizontal Portal QA

Build a controlled QA harness for:

- floor portals;
- ceiling portals;
- pentacle-like anchors;
- save/reload;
- entity traversal callbacks.

This should happen before declaring the WHA use case production-ready.

## Example WHA Pseudo-Code

```java
PortalAnchor source = new PortalAnchor(
    new PortalAnchorId("wha", ritualId + "/source"),
    sourceLevel.dimension(),
    pentacleCenter,
    PortalOrientations.horizontalUp(playerYaw),
    new PortalShapeSpec.Circle(1.5, 32),
    whaData
);

PortalAnchor target = new PortalAnchor(
    new PortalAnchorId("wha", linkedRitualId + "/target"),
    targetLevel.dimension(),
    targetPentacleCenter,
    PortalOrientations.horizontalUp(targetYaw),
    new PortalShapeSpec.Circle(1.5, 32),
    targetData
);

PortalLink link = PortalApi.links(server)
    .owner(new Identifier("wha", "pentacle_link"))
    .source(source)
    .target(target)
    .visual(PortalVisualOptions.simpleMagic(0xAA55FFFF))
    .teleport(PortalTeleportOptions.defaults()
        .crossPortalCollision(false)
        .interactable(false)
    )
    .createLinkedPair()
    .orThrow();
```

Teleport callback:

```java
PortalEvents.AFTER_ENTITY_TELEPORT.register(context -> {
    if (context.owner().equals(new Identifier("wha", "pentacle_link"))) {
        WitchRituals.onEntityCrossedPentaclePortal(
            context.entity(),
            context.sourceAnchor(),
            context.targetAnchor()
        );
    }
});
```

## Conclusion

The public API is feasible now if the first implementation is server-side and
high-level. The minimal useful API for WHA is:

- anchor value objects;
- rectangle/horizontal portal creation;
- linked portal pair creation;
- stable handle lookup/removal;
- owner metadata persistence;
- after-create / after-teleport / after-load events;
- simple visual options without renderer internals.

The first coding phase should be **Phase 14.1: Minimal Server-Side Portal API**.
It should wrap the already validated creation path from `PortalDebugCommands`
and `PortalManipulation`, but hide raw mutation order, sync details, and
renderer internals from third-party mods.
