# Phase 12.2 - Alpha Vanilla Release Checklist

## Release Candidate

Jar:

```text
build/libs/immersive-portals-7.0.0-alpha.1-mc26.1-fabric.jar
```

Version:

```text
7.0.0-alpha.1 for Minecraft 26.1
```

Status:

- alpha vanilla minimal release candidate: ready locally
- full port: not complete
- advanced compatibility target: not part of this alpha

## Required Dependencies

Required:

- Minecraft `26.1`
- Fabric Loader `0.19.3`
- Fabric API `0.145.1+26.1`

Bundled / included:

- Cloth Config `26.1.154`

Not required for this alpha:

- Sodium
- Iris
- DimLib
- shaderpacks
- AlternateDimensions dynamic support

## Validation Summary

Build/package:

- `clean compileJava processResources`: BUILD SUCCESSFUL
- `build`: BUILD SUCCESSFUL
- `validateAccessWidener`: OK
- jar generated: yes
- generated vanilla metadata excludes DimLib dependency: yes
- generated vanilla metadata excludes `imm_ptl_compat.mixins.json`: yes
- jar does not embed `run/`, saves, screenshots, logs or shaderpacks: yes

External smoke:

- external clean instance created: yes
- Minecraft `26.1`: OK
- Fabric Loader `0.19.3`: OK
- Fabric API `0.145.1+26.1`: OK
- Immersive Portals `7.0.0-alpha.1`: loaded
- clean world `Phase121ExternalAlphaSmoke`: loaded
- player joined world: yes
- Sodium loaded: no
- Iris loaded: no
- DimLib loaded: no
- shaderpack loaded: no

Dimension gameplay, validated in controlled dev harness:

- Overworld -> Nether reload: OK
- Nether -> Overworld reload: OK
- Overworld -> End reload: OK
- End -> Overworld reload: OK
- save/reload of test portals: OK in Phase 11.11 harness

## Negative Signal Checklist

Final known checks:

- `Network Protocol Error`: 0
- `Error deserializing chunk packet`: 0
- `Duplicate entity UUID`: 0
- `ConcurrentModificationException`: 0
- `Buffer already closed`: 0
- `Missing program`: 0
- `UnsupportedOperationException`: 0
- mixin apply blocker: 0
- access widener blocker: 0
- `fabric.mod.json` blocker: 0

## Known Limits

This alpha is intentionally narrow.

- This is not a complete 26.1 port.
- The target is the vanilla minimal path.
- Advanced recursive rendering is still partial.
- General shader/stencil clipping is not restored.
- Shaderpack support is experimental and not the target of this release.
- Complementary is not guaranteed; fallback modes remain manual/experimental.
- Sodium and Iris runtime compatibility are not part of this alpha target.
- DimLib and AlternateDimensions dynamic support remain isolated.
- Global advanced chunk tracking remains isolated.
- Debug/dev commands are not public gameplay UX.
- Portal traversal proofs for complex dimension reloads come from the controlled
  Phase 11.11 development harness.

## Files To Include In Commit

Recommended tracked files:

- `MIGRATION_PLAN_26.1.md`
- `PHASE12.2_ALPHA_RELEASE_CHECKLIST.md`
- `RELEASE_NOTES_7.0.0-alpha.1-mc26.1.md`
- `git-diff-phase12.2.txt`

If not already committed from previous phases, also include:

- Phase 12.0 packaging docs/fixes
- Phase 12.1 external smoke docs

## Files Not To Commit

Do not commit:

- `run/`
- `build/`
- external instance directory:
  - `C:\Users\simeo\Downloads\ImmersivePortalsAlphaSmoke`
- compile/run logs unless explicitly chosen as evidence
- screenshots
- shaderpacks
- temporary launch command JSON/TXT files
- pip/fabric installer logs
- copied/duplicate migration plans

## Publication Checklist

Before upload:

- verify current branch is correct;
- verify jar path and timestamp;
- verify generated jar contains no temporary files;
- attach only the final jar;
- include release notes;
- mark as alpha/pre-release;
- state Minecraft and Fabric versions clearly;
- state that Sodium/Iris/DimLib are not required and not targeted by this alpha;
- mention known limitations prominently;
- do not promise "complete port" or "full Immersive Portals rendering".

Recommended tag:

```text
v7.0.0-alpha.1-mc26.1-vanilla
```

Tag recommendation:

- recommended after the release docs are committed and the final jar is selected;
- do not tag if the working tree still has unrelated unstaged release files that
  make the baseline ambiguous.

## Conclusion

The alpha vanilla minimal release candidate is ready for local publication
preparation.

Recommended next phase:

- Phase 12.3 commit release docs and optionally create the local annotated tag.
