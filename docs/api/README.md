# Immersive Portals Public API Design

This directory contains design documents for the future public Immersive
Portals API.

The goal is to make Immersive Portals usable by third-party mods through a
stable, documented, high-level API instead of requiring direct access to
renderer, networking, mixin, chunk tracking, or debug-command internals.

## Target Use Case

The main design stress case is WHA / Witch Hate Atelier:

- draw a magic pentacle in the world;
- open a horizontal or vertical portal from that pentacle;
- link it to another matching pentacle;
- persist the link across save/reload;
- receive callbacks when an entity crosses the portal.

## Documents

- [`API_DESIGN_14.0.md`](API_DESIGN_14.0.md) - initial public API audit and
  design.
- [`API_USAGE_14.1.md`](API_USAGE_14.1.md) - experimental usage notes for the
  minimal server-side portal API.
- [`PHASE14.1_MINIMAL_SERVER_PORTAL_API.md`](PHASE14.1_MINIMAL_SERVER_PORTAL_API.md)
  - implementation report for Phase 14.1.

## Planned First Implementation

The first implementation phase is:

- Phase 14.1 - Minimal Server-Side Portal API

Status: implemented as an experimental server-side API. It includes portal
creation, linked portal pairs, stable handles, removal, lookup and minimal
persistent public metadata. Events and the WHA anchor/pentacle layer are still
planned for later phases.

Advanced rendering, Sodium, Iris, DimLib, shaderpacks, shader clipping, and
renderer internals are not part of the initial stable public API surface.
