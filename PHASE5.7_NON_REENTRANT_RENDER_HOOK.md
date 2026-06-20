# Phase 5.7 - Hook de rendu non reentrant

Date : 18 juin 2026

## Resultat

Le rendu recursif minimal n'est plus declenche depuis
`PortalEntityRenderer.submit`.

`PortalEntityRenderer.submit` collecte seulement un portail candidat visible.
Le rendu framebuffer est ensuite tente depuis un hook minimal dans
`GameRenderer.renderLevel`, au debut d'une frame de rendu et hors de
`LevelRenderer.submitEntities`.

Le risque de `ConcurrentModificationException` dans `submitEntities` est donc
leve.

## Chemin implemente

1. `PortalEntityRenderer.submit`
   - conserve le cadre cyan minimal ;
   - ajoute le portail visible a une petite file via
     `RendererUsingFrameBuffer.queueMinimalPortalFromEntityRenderer(...)` ;
   - ne lance plus `GameRenderer.renderLevel`.

2. `MixinGameRenderer.renderLevel`
   - injecte au `HEAD` de `GameRenderer.renderLevel(DeltaTracker)` ;
   - appelle
     `RendererUsingFrameBuffer.renderQueuedMinimalPortalsFromGameRendererHook()`
     si le rendu recursif minimal est active et que le moteur n'est pas deja
     en train de rendre un portail.

3. `RendererUsingFrameBuffer`
   - garde une seule entree par frame ;
   - verifie les facades `IEGameRenderer`, `IECamera`, `IEWorldRenderer` et
     `IEParticleManager` ;
   - verifie que le `Lightmap` est disponible ;
   - garde une protection de reentrance locale ;
   - conserve le fallback cyan en cas d'indisponibilite du blit.

## Audit LevelRenderer / GameRenderer

`LevelRenderer.submitEntities` est appele pendant la construction de la passe
principale du `LevelRenderer`. Lancer un second `GameRenderer.renderLevel`
depuis `PortalEntityRenderer.submit` reentrait dans cette iteration et pouvait
modifier la collection d'entites pendant son parcours.

Le hook `GameRenderer.renderLevel` est plus tot dans le flux de frame, avant
l'appel principal a `LevelRenderer.submitEntities`. Le rendu recursif appele
depuis ce hook est garde contre sa propre reentrance via `PortalRendering` et
un flag local.

## Nouveau blocage exact

Une tentative intermediaire a confirme que le rendu partait bien du nouveau
hook :

```text
Rendering minimal recursive portal from GameRenderer renderLevel hook ...
```

Le rendu de la scene destination passe alors les blocages precedents, puis
echoue au moment de blitter le framebuffer secondaire dans la cible principale :

```text
java.lang.IllegalStateException: Buffer already closed
at com.mojang.blaze3d.opengl.GlCommandEncoder.writeToBuffer(...)
at qouteall.imm_ptl.core.render.pipeline.IPRenderPipelines.drawMesh(...)
```

Ce blocage indique que le hook choisi est assez tot pour eviter
`submitEntities`, mais encore trop tot pour uploader/dessiner le quad final du
framebuffer dans le pipeline GPU 26.1.

Le blit est maintenant capture :

```text
Minimal recursive portal framebuffer blit failed; using cyan frame fallback
```

Apres le premier echec, les frames suivantes passent en fallback explicite :

```text
Minimal recursive portal rendering fallback: framebuffer blit is not available in this hook yet
```

## Fichiers inspectes

- `src/main/java/qouteall/imm_ptl/core/render/PortalEntityRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/render/renderer/RendererUsingFrameBuffer.java`
- `src/main/java/qouteall/imm_ptl/core/render/MyGameRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinGameRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinLevelRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/ducks/IEWorldRenderer.java`
- `net.minecraft.client.renderer.GameRenderer` 26.1 via `javap`
- `net.minecraft.client.renderer.LevelRenderer` 26.1 via `javap`

## Fichiers modifies

- `src/main/java/qouteall/imm_ptl/core/render/PortalEntityRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/render/renderer/RendererUsingFrameBuffer.java`
- `src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinGameRenderer.java`
- `MIGRATION_PLAN_26.1.md`
- `PHASE5.7_NON_REENTRANT_RENDER_HOOK.md`

## Validation

- `compileJava` : BUILD SUCCESSFUL, 0 erreur
- `processResources` : BUILD SUCCESSFUL
- `runClient` final : BUILD SUCCESSFUL, aucun crash
- `ConcurrentModificationException` : 0
- `Duplicate entity UUID` : 0
- `Game crashed` / `Reported exception` : 0
- portail present cote client : conserve par les phases precedentes
- cadre cyan fallback : conserve
- traversee : non retestee manuellement pendant ce lancement ; la logique de
  teleportation n'a pas ete modifiee dans cette phase
- vue destination visible : non

Le dernier lancement stable n'a pas remis le portail dans le champ pendant la
fenetre observee, donc il n'a pas retente le blit. L'essai intermediaire a
cependant confirme le nouveau chemin de hook et le blocage suivant. Le code
final capture ce blocage et garde le client stable.

## Contraintes

Sodium, Iris, DimLib, shaders, fog mixins, clipping mixins, stencil avance et
framebuffer avance complet ne sont pas reactives.
