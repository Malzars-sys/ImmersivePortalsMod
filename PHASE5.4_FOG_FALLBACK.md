# Phase 5.4 - Fallback fog vanilla minimal

Date : 13 juin 2026

## Resultat

Le rendu recursif minimal ne depend plus obligatoirement du contexte fog
avance. Quand `FogRendererContext.swappingManager` est absent, le rendu garde
le fog vanilla courant et emet une seule fois :

```text
Advanced fog context unavailable; using vanilla fog fallback
```

Le chemin a depasse le blocage fog et a expose le blocage suivant :
`ParticleEngine` n'implemente pas `IEParticleManager`, car
`MixinParticleEngine` reste volontairement isole du profil vanilla.

Une garde avant toute mutation d'etat conserve maintenant le cadre cyan sans
crash :

```text
Minimal recursive portal rendering fallback: MixinParticleEngine is isolated in the vanilla profile
```

La vue destination n'est donc pas encore visible.

## Audit

`FogRendererContext.swappingManager` est initialise par le contexte fog
historique, lui-meme dependant des mixins fog exclus du profil vanilla.

Il etait requis dans `MyGameRenderer.switchAndRenderTheWorld` pour :

- `pushSwapping(newDimension)` avant le rendu de la destination ;
- `popSwapping()` pendant la restauration du monde courant.

`RendererUsingFrameBuffer` bloquait auparavant le rendu recursif avant cet
appel lorsque le manager etait nul.

## Correctif minimal

- suppression de la garde bloquante fog dans `RendererUsingFrameBuffer` ;
- dans `MyGameRenderer`, swap fog uniquement si le manager avance existe ;
- conservation du fog vanilla courant lorsque le manager est absent ;
- ajout du log fog unique demande ;
- conservation du `try/finally` du framebuffer et de la couche portail ;
- ajout d'une garde `IEParticleManager` avant toute mutation d'etat recursive,
  afin de conserver un fallback cyan stable.

Aucun mixin fog, clipping, shader, Sodium, Iris ou DimLib n'a ete reactive.

## Fichiers inspectes

- `build.gradle`
- `src/main/resources/imm_ptl.mixins.json`
- `build/resources/main/imm_ptl.mixins.json`
- `src/main/java/qouteall/imm_ptl/core/render/context_management/FogRendererContext.java`
- `src/main/java/qouteall/imm_ptl/core/render/MyGameRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/render/renderer/RendererUsingFrameBuffer.java`
- `src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinRenderSystem_Fog.java`
- `src/main/java/qouteall/imm_ptl/core/mixin/client/particle/MixinParticleEngine.java`

## Fichiers modifies

- `src/main/java/qouteall/imm_ptl/core/render/MyGameRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/render/renderer/RendererUsingFrameBuffer.java`
- `MIGRATION_PLAN_26.1.md`
- `PHASE5.4_FOG_FALLBACK.md`

## Validation

- `compileJava` : BUILD SUCCESSFUL, 0 erreur
- `processResources` : BUILD SUCCESSFUL
- `runClient` final : BUILD SUCCESSFUL, aucun crash
- `IEGameRenderer` actif : oui
- `IECamera` actif : oui
- `IEWorldRenderer` actif : oui
- portail sauvegarde present cote client : oui
- cadre cyan fallback : conserve
- traversee Overworld vers Overworld : observee deux fois
- sauvegarde a la fermeture : reussie
- `Duplicate entity UUID` : 0
- vue destination visible : non

Le premier essai a confirme que le chemin atteignait le fallback fog, puis a
expose un `ClassCastException` sur `IEParticleManager`. Le lancement final
utilise la garde preflight et se termine proprement.

## Nouveau blocage exact

`build.gradle` exclut encore
`client.particle.MixinParticleEngine` du profil vanilla. Le rendu recursif
minimal exige actuellement `IEParticleManager` pour changer temporairement le
monde du moteur de particules.

Ce mixin n'a pas ete reactive pendant cette phase. Il constitue le prochain
blocage a auditer avant de poursuivre le rendu recursif.
