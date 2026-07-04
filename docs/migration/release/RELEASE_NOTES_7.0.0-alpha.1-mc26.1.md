# Immersive Portals 7.0.0-alpha.1 for Minecraft 26.1

This is a minimal vanilla alpha for Minecraft 26.1.

It is intended for early testing of the port. It is not a complete restoration
of all Immersive Portals features.

## Requirements

- Minecraft `26.1`
- Fabric Loader `0.19.3`
- Fabric API `0.145.1+26.1`

Bundled:

- Cloth Config `26.1.154`

Not required:

- Sodium
- Iris
- DimLib
- shaderpacks

## What Was Validated

- The mod jar builds successfully.
- The jar loads in a clean external Fabric instance.
- A clean singleplayer world can be entered.
- The vanilla minimal portal path has been validated in controlled development
  tests.
- Four real dimension reload directions were validated in the Phase 11.11 test
  harness:
  - Overworld -> Nether
  - Nether -> Overworld
  - Overworld -> End
  - End -> Overworld

## Known Limitations

- This is an alpha, not a finished 26.1 port.
- Advanced recursive portal rendering is still partial.
- General shader/stencil clipping is not restored.
- Shaderpack support is experimental and not targeted by this alpha.
- Sodium and Iris runtime compatibility are not part of this alpha target.
- DimLib and AlternateDimensions dynamic support are isolated.
- Advanced global chunk tracking is isolated.
- Development/debug commands used for validation are not public gameplay UI.

## Recommended Testing

Use a clean Fabric instance with only:

- Fabric API
- this Immersive Portals jar

Avoid adding Sodium, Iris, shaderpacks or DimLib when testing this alpha unless a
later compatibility phase explicitly targets them.

## Reporting Bugs

When reporting issues, include:

- Minecraft version;
- Fabric Loader version;
- Fabric API version;
- full latest log or crash report;
- whether Sodium/Iris/DimLib/shaderpacks were present;
- exact steps to reproduce.
