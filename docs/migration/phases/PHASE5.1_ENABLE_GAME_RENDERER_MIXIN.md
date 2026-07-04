# Phase 5.1 - Activation minimale de MixinGameRenderer

Date : 12 juin 2026

## Resultat

`MixinGameRenderer` est maintenant actif dans le profil vanilla et
`Minecraft.gameRenderer instanceof IEGameRenderer` vaut `true` au runtime.

La vue destination reste absente. Le nouveau blocage exact est l'absence de
`IECamera` sur la camera vanilla, car `MixinCamera` reste isole par le profil
vanilla. Le rendu minimal detecte ce cas avant l'appel recursif et conserve le
cadre cyan sans crash :

```text
IEGameRenderer active in vanilla profile: true
Minimal recursive portal rendering fallback: MixinCamera is isolated in the vanilla profile
```

## Cause de l'exclusion

`src/main/resources/imm_ptl.mixins.json` declarait deja
`client.render.MixinGameRenderer`, mais le filtre `processResources` de
`build.gradle` supprimait explicitement cette entree lorsque
`vanilla_core_compile=true`.

L'ancien `MixinGameRenderer` contenait aussi de nombreuses injections liees au
rendu historique. Les reactiver ensemble aurait depasse la portee de cette
phase et risque de restaurer indirectement des hooks de rendu non migres.

## Correctif minimal

- retrait de la seule exclusion Gradle de `client.render.MixinGameRenderer` ;
- conservation des exclusions shader, Sodium, Iris, DimLib et LevelRenderer ;
- reduction de `MixinGameRenderer` a la facade `IEGameRenderer` necessaire ;
- conservation des seuls acces camera et lightmap ;
- remplacement des anciens etats `renderHand` et panorama par des
  comportements neutres compatibles 26.1 ;
- log runtime unique du statut `IEGameRenderer` ;
- fallback explicite si la camera n'implemente pas encore `IECamera`.

Le JSON genere contient `client.render.MixinGameRenderer` et ne contient pas
`MixinGameRenderer_Shaders`, `MixinShaderInstance`, `MixinProgram`,
`MixinRenderSystem_Clipping`, `MixinLevelRenderer_Optional` ou
`MixinLevelRenderer_Clouds`.

## Incidents detectes et corriges

Le premier lancement a expose le shadow obsolete `GameRenderer.renderHand`.
Il a ete retire, car ce champ n'existe plus en 26.1.

La premiere tentative de rendu apres activation de `IEGameRenderer` a ensuite
expose un `ClassCastException` de `Camera` vers `IECamera`. `MixinCamera` n'a
pas ete reactive. Une garde de fallback a ete ajoutee avant l'appel recursif.

## Fichiers inspectes

- `build.gradle`
- `src/main/resources/imm_ptl.mixins.json`
- `build/resources/main/imm_ptl.mixins.json`
- `src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinGameRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/ducks/IEGameRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/ducks/IECamera.java`
- `src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinCamera.java`
- `src/main/java/qouteall/imm_ptl/core/render/MyGameRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/render/renderer/RendererUsingFrameBuffer.java`

## Fichiers modifies

- `build.gradle`
- `src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinGameRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/render/renderer/RendererUsingFrameBuffer.java`
- `MIGRATION_PLAN_26.1.md`
- `PHASE5.1_ENABLE_GAME_RENDERER_MIXIN.md`

## Validation

Monde utilise : `Phase50Test`, avec portail minimal sauvegarde et recharge.

- `compileJava` : BUILD SUCCESSFUL, 0 erreur
- `runClient` final : BUILD SUCCESSFUL, aucun crash
- second lancement/rechargement : BUILD SUCCESSFUL, aucun crash
- `IEGameRenderer` actif au runtime : oui
- portail present cote client : oui
- cadre cyan fallback : conserve
- vue destination visible : non
- `/imm_ptl_client_debug report_loaded_portals` : fonctionne
- `/imm_ptl_client_debug test_minimal_portal_traversal` : fonctionne
- `Client Teleported Statically` : observe
- sauvegarde/rechargement : fonctionne
- `Duplicate entity UUID` sur les lancements finaux : 0

## Nettoyage des rapports obsoletes

Les anciens journaux texte de compilation, lancement et diff remplaces par
les resultats 5.1 ont ete supprimes. Cela inclut les journaux suivis par Git
des phases 4.2 a 4.7, les artefacts temporaires des phases 4.8 a 5.0 et les
deux crashlogs intermediaires corriges pendant cette phase.

Les rapports Markdown historiques des phases 4.8, 4.9 et 5.0 sont conserves,
car ils documentent encore les decisions et validations precedentes.

## Contraintes

Sodium, Iris, DimLib, AlternateDimensions dynamique, stencil avance,
framebuffer avance complet et anciens mixins shader ne sont pas reactives.
