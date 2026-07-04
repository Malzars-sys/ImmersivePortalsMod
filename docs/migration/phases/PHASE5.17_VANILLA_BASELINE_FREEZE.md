# Phase 5.17 - Gel technique du renderer vanilla

Date : 20 juin 2026

## Commit de reference

Message prevu :

`Stabilize vanilla minimal portal renderer baseline`

Le hash final est fourni par `git log --oneline -1` apres creation du commit.
Il ne peut pas etre inscrit dans le commit qui le produit sans rendre le hash
auto-referentiel.

## Perimetre inclus

### Profil et facade renderer

- `build.gradle`
- `src/main/java/qouteall/imm_ptl/core/IPCGlobal.java`
- `src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinCamera.java`
- `src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinGameRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinLevelRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/mixin/client/particle/MixinParticleEngine.java`

### Rendu minimal et framebuffer

- `src/main/java/qouteall/imm_ptl/core/render/FrontClipping.java`
- `src/main/java/qouteall/imm_ptl/core/render/MyGameRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/render/MyRenderHelper.java`
- `src/main/java/qouteall/imm_ptl/core/render/PortalEntityRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/render/pipeline/IPRenderPipelines.java`
- `src/main/java/qouteall/imm_ptl/core/render/renderer/PortalRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/render/renderer/RendererUsingFrameBuffer.java`

### Debug et reproductibilite

- `src/main/java/qouteall/imm_ptl/core/commands/PortalCommand.java`
- `src/main/java/qouteall/imm_ptl/core/commands/PortalDebugCommands.java`
- `src/main/java/qouteall/imm_ptl/core/commands/ClientDebugCommand.java`
- `src/main/java/qouteall/imm_ptl/core/platform_specific/IPModEntryClient.java`

### Documentation

- `MIGRATION_PLAN_26.1.md`
- rapports uniques `PHASE4.8` et `PHASE4.9`
- rapports uniques `PHASE5.0` a `PHASE5.17`

Les rapports retracent la reactivation progressive des facades runtime, le
passage au hook non reentrant, le pont `SubmitNodeCollector`, les prototypes de
clipping/profondeur et la decision de ne pas ouvrir un chantier shader vanilla.

## Fichiers exclus

- toutes les copies `* - Copie*` ;
- tous les fichiers `compile-phase*.txt`, `runclient-phase*.txt` et leurs
  variantes stderr ;
- tous les fichiers `git-diff-phase*.txt` ;
- les anciens logs suivis par Git actuellement supprimes (`compile-errors*`,
  `compile-phase4.*`, `runclient-phase4.*`).

Les suppressions historiques ne sont pas necessaires a la baseline renderer et
restent volontairement non stagees. Elles pourront etre traitees dans un commit
de nettoyage separe, sans les melanger au gel technique.

## Profil vanilla gele

Actifs :

- `MixinGameRenderer` comme facade `IEGameRenderer` minimale ;
- `MixinCamera` comme facade `IECamera` minimale ;
- `MixinLevelRenderer` comme facade `IEWorldRenderer` minimale ;
- `MixinParticleEngine` comme facade `IEParticleManager` minimale ;
- rendu destination dans un framebuffer secondaire, une recursion ;
- soumission du quad texture par `SubmitNodeCollector` ;
- masque profondeur minimal et cadre cyan fallback.

Toujours isoles :

- Sodium et ses mixins ;
- Iris et ses mixins ;
- DimLib et AlternateDimensions dynamiques ;
- `MixinRenderSystem_Clipping` ;
- `MixinRenderSystem_Fog` ;
- `MixinGameRenderer_Shaders`, `MixinProgram` et `MixinShaderInstance` ;
- ancien stencil et renderer avances.

## Flags de developpement

- `IMM_PTL_AUTO_VISIBLE_TEST_PORTAL` : actif uniquement en environnement dev ;
- `IMM_PTL_AUTO_MINIMAL_TRAVERSAL_TEST` : actif uniquement en environnement
  dev ;
- `IMM_PTL_CAPTURE_MINIMAL_RECURSIVE_PORTAL` : capture opt-in unique ;
- `IMM_PTL_MINIMAL_RECURSIVE_PORTAL_SCREENSHOT` : basename assaini, sans chemin
  arbitraire.

Sans variable d'environnement, aucun pilote automatique ou capture n'est
enregistre.

## Validation

- `git diff --check` avant staging : aucune erreur ;
- `compileJava` : BUILD SUCCESSFUL ;
- `processResources` : BUILD SUCCESSFUL ;
- aucun nouveau code de rendu ajoute pendant la phase 5.17 ;
- profil de ressources verifie : les mixins shader/fog/clipping lourds restent
  filtres.

La validation runtime complete reste celle de la phase 5.15 : menu, monde
Phase50Test, rendu destination, traversable, sauvegarde/rechargement et fermeture
sans crash. Aucun code runtime n'a change pendant les phases 5.16 et 5.17.

## Limites connues

- clipping general incomplet ;
- stencil non expose par l'API publique `DepthStencilState` 26.1 ;
- fog vanilla conserve en fallback ;
- une seule recursion ;
- rendu multi-dimension dynamique non restaure ;
- Sodium, Iris et DimLib non actifs.

## Etape suivante

La baseline vanilla doit rester intacte. La Phase 6.0 ouvrira un profil Sodium
separe, compile-only dans un premier temps, avec activation progressive des
mixins de compatibilite.
