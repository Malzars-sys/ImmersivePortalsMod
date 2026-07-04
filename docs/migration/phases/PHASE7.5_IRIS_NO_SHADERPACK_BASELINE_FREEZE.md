# Phase 7.5 - Iris no-shaderpack baseline freeze

Date: 26 juin 2026

## Commit prevu

Message :

```text
Stabilize Iris no-shaderpack portal rendering baseline
```

Objectif : figer une baseline Iris runtime sans shaderpack, testee, visible,
traversable et separee des chantiers shaderpack / renderer Iris avance.

## Baselines precedentes

- Vanilla : `5545115e50e2b98b46303d7cf319d5d96f41ba5b`
  `Stabilize vanilla minimal portal renderer baseline`
- Sodium non-shader : `545169c1dc49418f91da8c13b70ff40cb0ebc8a6`
  `Stabilize Sodium non-shader portal rendering baseline`

## Versions et dependances

- Iris runtime : `1.10.8+mc26.1`
- Sodium baseline : `sodium_path=maven.modrinth:sodium:mc26.1.1-0.8.9-fabric`
- Sodium runtime Iris : `iris_sodium_path=maven.modrinth:sodium:mc26.1-0.8.7-fabric`
- Iris runtime reste derriere `-Penable_iris_compat=true -Penable_iris=true`
- `enable_iris=false` et `enable_iris_compat=false` par defaut.

Le `iris_sodium_path` separe est conserve parce qu'Iris 1.10.8 accepte Sodium
0.8.x, tandis que Sodium 0.8.9 declare casser Iris `<=1.10.8`. La baseline
Sodium non-shader reste donc sur 0.8.9 ; seul le runtime Iris utilise 0.8.7.

## Fallback FRAPI

`IrisSodiumFrapiFallbackRenderer` reste limite :

- environnement de developpement seulement ;
- Iris charge ;
- Sodium charge ;
- aucun provider Fabric Renderer API deja actif.

Il corrige le trou de Sodium 0.8.7 : ce jar declare un renderer FRAPI et
desactive Indigo, mais n'enregistre aucun provider. Le fallback ne remplace pas
le provider Sodium 0.8.9 de la baseline Sodium.

## Mixins Iris/Sodium actifs

`processResources` en profil Iris runtime genere `imm_ptl_compat.mixins.json`
avec exactement :

```json
[
  "sodium.MixinSodiumPortalEntityRenderer",
  "sodium.MixinSodiumPortalLevelRenderer"
]
```

Ces deux mixins permettent seulement :

- le bypass `EntityRenderer.shouldRender` pour les entites `Portal` ;
- le bypass `LevelRenderer.isSectionCompiledAndVisible` pour les entites
  `Portal`.

## Mixins explicitement exclus

Le profil Iris runtime n'active pas :

- `sodium.IESodiumWorldRenderer`
- `sodium.MixinSodiumFlawlessFrames`
- `sodium.MixinSodiumWorldRenderer`
- `sodium.MixinSodiumViewport`
- `sodium.MixinSodiumOcclusionCuller`
- `sodium.MixinSodiumRenderRegion`
- `sodium.MixinSodiumRenderSectionManager`
- `sodium.MixinSodiumDefaultShaderInterface`
- `sodium.MixinSodiumShaderLoader`
- renderer Iris avance Immersive Portals
- shaderpack Iris
- DimLib
- AlternateDimensions dynamique
- clipping shader.

## Validations

### Compilation

- `compile-phase7.5-vanilla.txt` : BUILD SUCCESSFUL
- `compile-phase7.5-sodium.txt` : BUILD SUCCESSFUL
- `compile-phase7.5-iris.txt` : BUILD SUCCESSFUL

### Ressources Iris runtime

- `processresources-phase7.5-iris-runtime.txt` : BUILD SUCCESSFUL
- `imm_ptl_compat.mixins.json` genere : oui
- contenu : exactement les deux mixins `Portal`
- Groupe C complet : non
- mixins shader Sodium : non.

### Monde Iris sans shaderpack

- `runclient-phase7.5-iris-world.txt` : BUILD SUCCESSFUL
- Iris charge : oui
- Sodium 0.8.7 charge : oui
- fallback FRAPI enregistre : oui
- shaderpack charge : non
- joueur connecte : oui
- fermeture normale : oui
- crash : 0
- erreur mixin : 0.

### Portail Iris sans shaderpack

- `runclient-phase7.5-iris-portal.txt` : BUILD SUCCESSFUL
- portail cree ou detecte : oui
- portail present cote client : oui
- `PortalEntityRenderer.submit` appele : oui
- cadre cyan visible : oui
- framebuffer minimal atteint : oui
- texture framebuffer disponible : oui
- quad texture soumis via `SubmitNodeCollector` : oui
- capture obtenue : oui
- traversee : oui
- `Client Teleported Statically` : oui
- `Duplicate entity UUID` : 0
- `ConcurrentModificationException` : 0
- `Buffer already closed` : 0
- `UnsupportedOperationException` : 0
- erreur mixin : 0.

Capture :

```text
run/screenshots/phase7.5-iris-no-shaderpack-baseline.png
```

## Limites connues

- aucun shaderpack teste ;
- renderer Iris avance toujours no-op ;
- clipping general incomplet ;
- fog vanilla fallback ;
- une seule recursion ;
- mini-groupe Iris limite aux portails, pas au Groupe C complet ;
- Sodium runtime Iris reste en 0.8.7 par contrainte Iris 1.10.8.

## Fichiers inclus dans le commit

Prevus pour staging selectif :

- `build.gradle`
- `gradle.properties`
- `MIGRATION_PLAN_26.1.md`
- `src/main/java/qouteall/imm_ptl/core/compat/iris_compatibility/ExperimentalIrisPortalRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/compat/iris_compatibility/IPIrisHelper.java`
- `src/main/java/qouteall/imm_ptl/core/compat/iris_compatibility/IrisCompatibilityPortalRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/compat/iris_compatibility/IrisPortalRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/compat/iris_compatibility/IrisSodiumFrapiFallbackRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/compat/mixin/iris/MixinIrisTransformPatcher.java`
- `src/main/java/qouteall/imm_ptl/core/platform_specific/IPModEntryClient.java`
- `PHASE7.0_IRIS_COMPILE_PROFILE.md`
- `PHASE7.1_IRIS_RUNTIME_MENU.md`
- `PHASE7.2_IRIS_RUNTIME_WORLD_NO_SHADERPACK.md`
- `PHASE7.3_IRIS_PORTAL_NO_SHADERPACK.md`
- `PHASE7.4_IRIS_SODIUM_PORTAL_BYPASS.md`
- `PHASE7.5_IRIS_NO_SHADERPACK_BASELINE_FREEZE.md`

## Fichiers exclus du commit

- logs generes `compile-phase*.txt` et `runclient-phase*.txt` ;
- `processresources-phase7.5-iris-runtime.txt` ;
- captures `run/screenshots/*.png` ;
- dossier `run/` ;
- dossier `build/` ;
- vieux logs Phase 4 supprimes dans le worktree ;
- copies locales comme `MIGRATION_PLAN_26.1 - Copie*.md` ;
- autres artefacts temporaires deja presents.

## Conclusion

La baseline Iris runtime sans shaderpack est prete a etre commitee : profil
stable, monde charge, portail visible, traversable, capture obtenue, sans
shaderpack et sans activation du renderer Iris avance.
