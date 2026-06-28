# Phase 8.1 - Iris shaderpack portal test

Date : 28 juin 2026

## Objectif

Tester un portail minimal avec Iris + shaderpack actif, sans restaurer le
renderer Iris avance, sans shader clipping et sans DimLib.

## Shaderpack

- fichier : `MakeUp-UltraFast-9.5c.zip`
- chemin : `run/shaderpacks/MakeUp-UltraFast-9.5c.zip`
- taille : 398840 octets
- configuration Iris :
  `run/config/iris.properties`
- selection active :
  `shaderPack=MakeUp-UltraFast-9.5c.zip`

## Validations compilation

- Vanilla :
  `.\gradlew.bat compileJava processResources --rerun-tasks --console=plain`
  - resultat : BUILD SUCCESSFUL
  - log : `compile-phase8.1-vanilla.txt`

- Sodium compile-only :
  `.\gradlew.bat compileJava processResources --rerun-tasks --console=plain -Penable_sodium_compat=true`
  - resultat : BUILD SUCCESSFUL
  - log : `compile-phase8.1-sodium.txt`

- Iris compile-only :
  `.\gradlew.bat compileJava processResources --rerun-tasks --console=plain -Penable_iris_compat=true`
  - resultat : BUILD SUCCESSFUL
  - log : `compile-phase8.1-iris.txt`

## Monde temoin

Monde utilise :

```text
Phase81IrisShaderpackPortalTest
```

Le monde a ete prepare depuis `Phase80IrisShaderpackWorldTest`, pas depuis
`Phase50Test`. Avant le run, les dossiers `entities` de la copie ont ete
supprimes afin d'eviter les anciens portails sauvegardes. Le portail teste a
donc ete cree pendant cette phase par le flag dev.

## Runtime

Commande :

```powershell
.\gradlew.bat runClient --console=plain -Penable_iris_compat=true -Penable_iris=true --args=--quickPlaySingleplayer=Phase81IrisShaderpackPortalTest
```

Flags actifs :

- `IMM_PTL_AUTO_VISIBLE_TEST_PORTAL=true`
- `IMM_PTL_AUTO_MINIMAL_TRAVERSAL_TEST=true`
- `IMM_PTL_CAPTURE_MINIMAL_RECURSIVE_PORTAL=true`
- `IMM_PTL_MINIMAL_RECURSIVE_PORTAL_SCREENSHOT=phase8.1-iris-shaderpack-portal.png`

Resultat :

- runClient : BUILD SUCCESSFUL
- Iris charge : oui, `iris 1.10.8+mc26.1`
- Sodium runtime Iris : oui, `sodium 0.8.7+mc26.1`
- fallback FRAPI Iris/Sodium 0.8.7 enregistre : oui
- shaderpack actif : oui
- log Iris : `Using shaderpack: MakeUp-UltraFast-9.5c.zip`
- pipeline Iris Overworld cree : oui
- mini-groupe Portal actif : oui
- portail cree : oui
- portail present cote client : oui
- bypass `EntityRenderer.shouldRender` pour `Portal` : actif
- bypass `LevelRenderer.isSectionCompiledAndVisible` pour `Portal` : actif
- `PortalEntityRenderer.submit` appele : oui
- framebuffer minimal atteint : oui
- texture framebuffer disponible : oui, `854x480`
- quad texture `SubmitNodeCollector` soumis : oui
- capture obtenue : oui
- traversee declenchee : oui
- `Client Teleported Statically` : oui
- fermeture normale : oui

## Capture

Capture :

```text
run/screenshots/phase8.1-iris-shaderpack-portal.png
```

Observation : la capture montre le cadre cyan du portail dans le rendu
shaderpack actif. La vue framebuffer destination n'est pas clairement lisible
dans l'image, meme si les logs confirment que la texture framebuffer est
disponible et que le quad texture a ete soumis.

## Erreurs et blocage visuel

Le client ne crash pas, mais Iris signale deux erreurs non fatales :

```text
Missing program minecraft:pipeline/imm_ptl_portal_depth_mask in override list.
Missing program minecraft:pipeline/imm_ptl_draw_framebuffer_in_area_depth_masked in override list.
```

Classification : blocage F, texture framebuffer / SubmitNodeCollector sous
shaderpack Iris. Les pipelines RenderPipeline Immersive Portals minimaux sont
soumis, mais Iris avec shaderpack tente de rediriger les programmes et ne trouve
pas d'override pour ces deux pipelines.

Ce point explique probablement pourquoi le cadre cyan reste visible alors que
la vue destination n'est pas lisible dans la capture.

## Absence de regressions

- crash : 0
- erreur mixin bloquante : 0
- `UnsupportedOperationException` : 0
- `Duplicate entity UUID` : 0
- `ConcurrentModificationException` : 0
- `Buffer already closed` : 0

Notes non bloquantes :

- warnings Iris `betterendforge::*` issus du shaderpack ;
- `IPModInfoChecking` 404 ;
- Realms auth FabricMC.

## Conclusion

Critere minimal atteint : avec Iris + MakeUp-UltraFast actif, le portail est
cree, present cote client, atteint par `PortalEntityRenderer`, et traversable
sans crash.

Blocage restant : le rendu visuel framebuffer sous shaderpack n'est pas encore
correctement integre aux overrides Iris pour les pipelines
`imm_ptl_portal_depth_mask` et
`imm_ptl_draw_framebuffer_in_area_depth_masked`.

Suite recommandee : Phase 8.2, audit cible des overrides Iris pour les deux
pipelines minimaux Immersive Portals, sans restaurer le renderer Iris avance et
sans activer les mixins shader Sodium.
