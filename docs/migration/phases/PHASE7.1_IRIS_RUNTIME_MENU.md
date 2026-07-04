# Phase 7.1 - Iris Runtime Menu, No Shaderpack

Date: 2026-06-25

## Objective

Load Iris at runtime and reach the Minecraft main menu without loading a
shaderpack, without entering a world, and without activating the Immersive
Portals Iris renderer.

## Runtime Flags

Runtime Iris is tested with:

```powershell
.\gradlew.bat runClient --console=plain -Penable_iris_compat=true -Penable_iris=true
```

`enable_iris=false` remains the default in `gradle.properties`.

## Dependency Notes

Iris 1.10.8 has a hard runtime dependency on Sodium `0.8.x`.

The first runtime attempt with Iris alone failed:

```text
Mod 'Iris' (iris) 1.10.8+mc26.1 requires any 0.8.x version of sodium, which is missing.
```

Using the Phase 6 Sodium baseline version also failed:

```text
Mod 'Sodium' (sodium) 0.8.9+mc26.1.1 is incompatible with version 1.10.8 or earlier of mod 'Iris'.
```

To keep the Sodium baseline untouched, a separate Iris runtime Sodium coordinate
was added:

```properties
iris_sodium_path=maven.modrinth:sodium:mc26.1-0.8.7-fabric
```

The normal Sodium baseline remains:

```properties
sodium_path=maven.modrinth:sodium:mc26.1.1-0.8.9-fabric
```

## Runtime Mixins

Immersive Portals compatibility mixins active at runtime: none.

`imm_ptl_compat.mixins.json` remains excluded from processed resources in this
Phase 7.1 Iris runtime menu profile. Iris only loads its own mixins.

Still excluded:

- Immersive Portals Iris runtime mixins;
- Sodium shader mixins;
- `MixinSodiumDefaultShaderInterface`;
- `MixinSodiumShaderLoader`;
- DimLib;
- AlternateDimensions;
- shader clipping;
- legacy Iris portal renderer.

## Shaderpack Status

The `run/shaderpacks` folder is absent.

The runtime log confirms:

```text
Shaders are disabled because no valid shaderpack is selected
```

## Validation

Vanilla:

```powershell
.\gradlew.bat compileJava processResources --rerun-tasks --console=plain
```

Result: BUILD SUCCESSFUL.

Sodium compile-only:

```powershell
.\gradlew.bat compileJava processResources --rerun-tasks --console=plain -Penable_sodium_compat=true
```

Result: BUILD SUCCESSFUL.

Iris compile-only:

```powershell
.\gradlew.bat compileJava processResources --rerun-tasks --console=plain -Penable_iris_compat=true
```

Result: BUILD SUCCESSFUL.

Iris runtime menu:

```powershell
.\gradlew.bat runClient --console=plain -Penable_iris_compat=true -Penable_iris=true
```

Result: BUILD SUCCESSFUL.

## Runtime Result

- Minecraft started: yes.
- Iris loaded: yes, `iris 1.10.8+mc26.1`.
- Sodium loaded for Iris runtime: yes, `sodium 0.8.7+mc26.1`.
- Menu reached: yes.
- Shaderpack loaded: no.
- World entered: no.
- Crash: 0.
- Mixin error: 0.
- DimLib error: 0.
- Normal shutdown: yes, `Stopping!` then `BUILD SUCCESSFUL`.

## Next Step

Phase 7.2 can test world loading with Iris runtime, still without shaderpack and
still without enabling the legacy Iris portal renderer. Sodium/Iris version
alignment should remain separate from the Phase 6 Sodium 0.8.9 baseline.

## Artifacts

- `compile-phase7.1-vanilla.txt`
- `compile-phase7.1-sodium.txt`
- `compile-phase7.1-iris.txt`
- `runclient-phase7.1-iris-menu.txt`
- `git-diff-phase7.1.txt`
