# Phase 7.4 - Mini-groupe Iris/Sodium portal bypass

Date: 26 juin 2026

## Resultat

Le mini-groupe Iris/Sodium 0.8.7 limite aux deux bypass `Portal` fonctionne.
Sous Iris runtime sans shaderpack, le portail atteint maintenant
`PortalEntityRenderer.submit`, le cadre cyan est visible, le framebuffer minimal
est soumis, une capture native est obtenue et la traversee reste fonctionnelle.

Validations finales :

- Vanilla `compileJava processResources` : BUILD SUCCESSFUL ;
- Sodium compile-only : BUILD SUCCESSFUL ;
- Iris compile-only : BUILD SUCCESSFUL ;
- Iris runtime monde sans shaderpack : BUILD SUCCESSFUL ;
- Iris runtime portail sans shaderpack : BUILD SUCCESSFUL ;
- Iris runtime : `1.10.8+mc26.1` ;
- Sodium runtime Iris : `0.8.7+mc26.1` ;
- fallback FRAPI Iris/Sodium 0.8.7 : enregistre ;
- shaderpack charge : non ;
- DimLib : inactif ;
- AlternateDimensions : inactif ;
- renderer Iris avance : no-op ;
- mixins shader Sodium : inactifs ;
- crash final : 0.

## Mini-groupe actif

`processResources` genere `imm_ptl_compat.mixins.json` avec exactement :

```json
[
  "sodium.MixinSodiumPortalEntityRenderer",
  "sodium.MixinSodiumPortalLevelRenderer"
]
```

Mixins explicitement non actifs dans ce profil :

- `sodium.IESodiumWorldRenderer`
- `sodium.MixinSodiumFlawlessFrames`
- `sodium.MixinSodiumWorldRenderer`
- `sodium.MixinSodiumViewport`
- `sodium.MixinSodiumOcclusionCuller`
- `sodium.MixinSodiumRenderRegion`
- `sodium.MixinSodiumRenderSectionManager`
- `sodium.MixinSodiumDefaultShaderInterface`
- `sodium.MixinSodiumShaderLoader`

La baseline Sodium reste inchangee : `sodium_path` pointe toujours vers Sodium
0.8.9. Le runtime Iris utilise toujours le chemin separe `iris_sodium_path` vers
Sodium 0.8.7.

## Compilation

Commandes validees :

```text
.\gradlew.bat compileJava processResources --rerun-tasks --console=plain
.\gradlew.bat compileJava processResources --rerun-tasks --console=plain -Penable_sodium_compat=true
.\gradlew.bat compileJava processResources --rerun-tasks --console=plain -Penable_iris_compat=true
```

Resultats :

- `compile-phase7.4-vanilla.txt` : BUILD SUCCESSFUL ;
- `compile-phase7.4-sodium.txt` : BUILD SUCCESSFUL ;
- `compile-phase7.4-iris.txt` : BUILD SUCCESSFUL.

## Monde Iris sans shaderpack

Commande :

```text
.\gradlew.bat runClient --console=plain -Penable_iris_compat=true -Penable_iris=true --args=--quickPlaySingleplayer=Phase72IrisNoShaderTest
```

Resultat :

- Iris charge : oui ;
- Sodium 0.8.7 charge : oui ;
- fallback FRAPI enregistre : oui ;
- mini-groupe Portal actif : oui ;
- shaderpack charge : non ;
- joueur connecte : oui ;
- fermeture normale : oui ;
- `BUILD SUCCESSFUL`.

Extraits :

```text
Registered minimal Fabric Renderer API fallback for Iris with Sodium 0.8.7
Shaders are disabled because no valid shaderpack is selected
Player909 joined the game
Sodium portal entity frustum bypass active: true
Sodium portal section visibility bypass active: true
BUILD SUCCESSFUL
```

## Test portail

Flags utilises :

```text
IMM_PTL_AUTO_VISIBLE_TEST_PORTAL=true
IMM_PTL_AUTO_MINIMAL_TRAVERSAL_TEST=true
IMM_PTL_CAPTURE_MINIMAL_RECURSIVE_PORTAL=true
IMM_PTL_MINIMAL_RECURSIVE_PORTAL_SCREENSHOT=phase7.4-iris-portal-bypass.png
```

Resultat :

- portail cree ou detecte : oui ;
- portail present cote client : oui ;
- bypass `EntityRenderer.shouldRender` actif pour `Portal` : oui ;
- bypass `LevelRenderer.isSectionCompiledAndVisible` actif pour `Portal` : oui ;
- `PortalEntityRenderer.submit` appele : oui ;
- cadre cyan visible : oui ;
- framebuffer minimal atteint : oui ;
- texture framebuffer disponible : oui ;
- quad texture soumis via `SubmitNodeCollector` : oui ;
- masque profondeur minimal applique : oui ;
- capture obtenue : oui ;
- traversee declenchee : oui ;
- `Client Teleported Statically` : oui ;
- fermeture normale : oui ;
- `BUILD SUCCESSFUL`.

Extraits :

```text
Sodium portal entity frustum bypass active: true
Sodium portal section visibility bypass active: true
PortalEntityRenderer submit called under Sodium: true
Queued minimal recursive portal from PortalEntityRenderer
Rendering minimal recursive portal from GameRenderer renderLevel hook
Submitting minimal recursive portal framebuffer from PortalEntityRenderer
Minimal recursive portal framebuffer texture available: true (854x480, portal 1)
Minimal portal depth mask applied: true
Minimal recursive portal textured quad submitted via SubmitNodeCollector: true
Captured minimal recursive portal screenshot: Saved screenshot as phase7.4-iris-portal-bypass.png
Client Teleported Statically
BUILD SUCCESSFUL
```

Capture :

```text
run/screenshots/phase7.4-iris-portal-bypass.png
```

Observation rapide : le cadre cyan est visible sous pluie/fog Iris sans
shaderpack. Le rendu reste celui du renderer minimal vanilla/Sodium :
clipping incomplet, une recursion et fallback fog vanilla.

## Verifications negatives

Dans `runclient-phase7.4-iris-portal.txt` :

- `UnsupportedOperationException` : 0 ;
- `Duplicate entity UUID` : 0 ;
- `ConcurrentModificationException` : 0 ;
- `Buffer already closed` : 0 ;
- `Mixin apply failed` : 0 ;
- crash marker `#@!@#` : 0 ;
- `BUILD FAILED` : 0.

La seule ligne `Exception` observee est l'erreur Realms externe habituelle, sans
impact sur le test.

## Fichiers produits

- `compile-phase7.4-vanilla.txt`
- `compile-phase7.4-sodium.txt`
- `compile-phase7.4-iris.txt`
- `runclient-phase7.4-iris-world.txt`
- `runclient-phase7.4-iris-portal.txt`
- `processresources-phase7.4-iris-runtime.txt`
- `git-diff-phase7.4.txt`
- `run/screenshots/phase7.4-iris-portal-bypass.png`

## Conclusion

Critere minimal atteint : sous Iris runtime sans shaderpack, avec Sodium 0.8.7,
le mini-groupe limite aux deux bypass `Portal` permet d'atteindre
`PortalEntityRenderer.submit` sans activer le Groupe C complet, sans shaderpack,
sans renderer Iris avance et sans mixins shader Sodium.
