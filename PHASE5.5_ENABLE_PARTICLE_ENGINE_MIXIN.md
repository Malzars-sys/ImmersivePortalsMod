# Phase 5.5 - Activation minimale de MixinParticleEngine

Date : 18 juin 2026

## Resultat

`MixinParticleEngine` est maintenant actif dans le profil vanilla et
`ParticleEngine` implemente `IEParticleManager` au runtime.

Le rendu recursif minimal depasse donc le blocage particules de la phase 5.4.
La vue destination reste absente, mais le crash est evite : l'echec est capture
et le cadre cyan reste le fallback.

Logs runtime confirmes :

```text
IEGameRenderer active in vanilla profile: true
IECamera active in vanilla profile: true
IEWorldRenderer active in vanilla profile: true
IEParticleManager active in vanilla profile: true
Advanced fog context unavailable; using vanilla fog fallback
Beginning minimal recursive portal render in minecraft:overworld
Minimal recursive portal render failed; using cyan frame fallback
```

## Cause de l'exclusion

`src/main/resources/imm_ptl.mixins.json` declarait deja
`client.particle.MixinParticleEngine`, mais le filtre `processResources` de
`build.gradle` supprimait cette entree lorsque `vanilla_core_compile=true`.

Le mixin historique contenait encore des injections dans le rendu et le tick
des particules. Ces hooks ne sont pas necessaires a la phase 5.5.

## Correctif minimal

- retrait de la seule exclusion Gradle de
  `client.particle.MixinParticleEngine` ;
- reduction de `MixinParticleEngine` a une facade `IEParticleManager` ;
- conservation uniquement du shadow `ParticleEngine.level` et de
  `ip_setWorld(ClientLevel)` ;
- suppression des injections de rendu/tick de particules ;
- ajout du log runtime unique
  `IEParticleManager active in vanilla profile: true/false`.

Aucun mixin Sodium, Iris, DimLib, shader, fog, clipping, stencil avance ou
framebuffer avance complet n'a ete reactive.

## Fichiers inspectes

- `build.gradle`
- `src/main/resources/imm_ptl.mixins.json`
- `build/resources/main/imm_ptl.mixins.json`
- `src/main/java/qouteall/imm_ptl/core/mixin/client/particle/MixinParticleEngine.java`
- `src/main/java/qouteall/imm_ptl/core/ducks/IEParticleManager.java`
- `src/main/java/qouteall/imm_ptl/core/render/MyGameRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/render/renderer/RendererUsingFrameBuffer.java`

## Fichiers modifies

- `build.gradle`
- `src/main/java/qouteall/imm_ptl/core/mixin/client/particle/MixinParticleEngine.java`
- `src/main/java/qouteall/imm_ptl/core/render/renderer/RendererUsingFrameBuffer.java`
- `MIGRATION_PLAN_26.1.md`
- `PHASE5.5_ENABLE_PARTICLE_ENGINE_MIXIN.md`

## Validation

- `compileJava` : BUILD SUCCESSFUL, 0 erreur
- `processResources` : BUILD SUCCESSFUL
- `runClient` final : BUILD SUCCESSFUL, aucun crash
- `IEGameRenderer` actif : oui
- `IECamera` actif : oui
- `IEWorldRenderer` actif : oui
- `IEParticleManager` actif : oui
- portail present cote client : oui
- cadre cyan fallback : conserve
- sauvegarde a la fermeture : reussie
- `Duplicate entity UUID` : 0
- traversee : non retestee manuellement pendant ce lancement ; le code de
  teleportation n'a pas ete modifie dans cette phase
- vue destination visible : non

## Nouveau blocage exact

Le rendu recursif minimal atteint maintenant `GameRenderer.renderLevel`, puis
echoue sur :

```text
java.lang.NullPointerException: Cannot invoke
"net.minecraft.client.renderer.Lightmap.getTextureView()" because
"this.lightmap" is null
```

Le stacktrace pointe vers `GameRenderer.lightmap(GameRenderer.java:819)`, appele
pendant le rendu des sections de chunks. Cela indique que le `GameRenderer`
26.1 attend un etat `Lightmap` runtime supplementaire, distinct de la facade
historique `IEGameRenderer.ip_setLightmapTextureManager`.

Ce blocage est capture par le fallback existant et ne fait pas crasher le
client.
