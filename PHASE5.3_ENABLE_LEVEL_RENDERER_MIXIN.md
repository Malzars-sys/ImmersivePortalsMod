# Phase 5.3 - Activation minimale de MixinLevelRenderer

Date : 13 juin 2026

## Resultat

`MixinLevelRenderer` est maintenant actif dans le profil vanilla et
`LevelRenderer` implemente `IEWorldRenderer` au runtime.

La vue destination reste absente. Le rendu recursif depasse les blocages
`IEGameRenderer`, `IECamera` et `IEWorldRenderer`, puis atteint le nouveau
blocage exact : `FogRendererContext.swappingManager` n'est pas initialise car
le contexte fog avance et `MixinRenderSystem_Fog` restent isoles.

Le fallback cyan est conserve sans crash :

```text
IEGameRenderer active in vanilla profile: true
IECamera active in vanilla profile: true
IEWorldRenderer active in vanilla profile: true
Minimal recursive portal rendering fallback: advanced fog context is isolated in the vanilla profile
```

## Cause de l'exclusion

`src/main/resources/imm_ptl.mixins.json` declarait deja
`client.render.MixinLevelRenderer`, mais le filtre `processResources` de
`build.gradle` supprimait explicitement cette entree lorsque
`vanilla_core_compile=true`.

L'ancien mixin contenait de nombreux hooks lourds : injections dans
`renderLevel`, clipping, meteo, sky, remplacement de `ViewArea`, terrain,
translucide et rendu d'entites. Ces hooks ont ete retires de la facade
reactivee.

## Correctif minimal

- retrait de la seule exclusion Gradle de `client.render.MixinLevelRenderer` ;
- remplacement du mixin par une facade `IEWorldRenderer` sans injection ;
- conservation des shadows 26.1 valides :
  `entityRenderDispatcher`, `viewArea`, `renderBuffers`, `visibleSections` ;
- etats locaux neutres pour les anciens contrats transparency, frustum,
  render buffers et listes de sections ;
- rendu direct d'entite conserve en no-op, car l'API 26.1 utilise des etats
  extraits ;
- ajout du log runtime unique `IEWorldRenderer` ;
- ajout d'un fallback explicite lorsque le contexte fog avance est absent.

Le JSON genere contient `MixinLevelRenderer`, mais ne contient pas
`MixinLevelRenderer_Optional`, `MixinLevelRenderer_Clouds`,
`MixinLevelRenderer_ForceMainThreadRebuild`, `MixinMultiBufferSourceBufferSource`,
`MixinScreenEffectRenderer`, les mixins shader ou fog interdits.

## Incidents detectes et corriges

La premiere tentative recursive avec `IEWorldRenderer` actif a expose un
`NullPointerException` sur `FogRendererContext.swappingManager`.

Le contexte fog n'a pas ete reactive. Une garde de fallback a ete ajoutee
avant toute modification d'etat recursive.

## Fichiers inspectes

- `build.gradle`
- `src/main/resources/imm_ptl.mixins.json`
- `build/resources/main/imm_ptl.mixins.json`
- `src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinLevelRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/ducks/IEWorldRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/render/MyGameRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/render/renderer/RendererUsingFrameBuffer.java`
- `src/main/java/qouteall/imm_ptl/core/ClientWorldLoader.java`
- `src/main/java/qouteall/imm_ptl/core/render/ViewAreaRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/render/ImmPtlViewArea.java`
- `src/main/java/qouteall/imm_ptl/core/render/context_management/FogRendererContext.java`
- classe Minecraft 26.1 `net.minecraft.client.renderer.LevelRenderer`,
  inspectee avec `javap`

## Fichiers modifies

- `build.gradle`
- `src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinLevelRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/render/renderer/RendererUsingFrameBuffer.java`
- `MIGRATION_PLAN_26.1.md`
- `PHASE5.3_ENABLE_LEVEL_RENDERER_MIXIN.md`

## Validation

Monde utilise : `Phase50Test`, avec portails minimaux sauvegardes.

- `compileJava` : BUILD SUCCESSFUL, 0 erreur
- `processResources` : BUILD SUCCESSFUL
- deux lancements finaux : BUILD SUCCESSFUL, aucun crash
- `IEGameRenderer` actif au runtime : oui
- `IECamera` actif au runtime : oui
- `IEWorldRenderer` actif au runtime : oui
- portail sauvegarde present cote client et recharge : oui
- cadre cyan fallback : conserve
- vue destination visible : non
- sauvegarde/rechargement : fonctionne
- `Duplicate entity UUID` sur les lancements finaux : 0
- traversee : code non modifie et validation 5.1 conservee ; la commande
  automatisee n'a pas ete resoumise de maniere fiable pendant les lancements
  finaux 5.3

## Contraintes

Sodium, Iris, DimLib, AlternateDimensions dynamique, stencil avance,
framebuffer avance complet, mixins shader, fog et variantes lourdes de
LevelRenderer ne sont pas reactives.
