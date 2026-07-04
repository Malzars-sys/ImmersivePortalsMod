# Phase 5.2 - Activation minimale de MixinCamera

Date : 12 juin 2026

## Resultat

`MixinCamera` est maintenant actif dans le profil vanilla et la camera
principale implemente `IECamera` au runtime.

La vue destination reste absente. Le chemin recursif depasse les blocages
`IEGameRenderer` et `IECamera`, puis atteint le nouveau blocage exact :
`LevelRenderer` n'implemente pas `IEWorldRenderer`, car `MixinLevelRenderer`
reste volontairement isole.

Le fallback cyan est conserve sans crash :

```text
IEGameRenderer active in vanilla profile: true
IECamera active in vanilla profile: true
Minimal recursive portal rendering fallback: MixinLevelRenderer is isolated in the vanilla profile
```

## Cause de l'exclusion

`src/main/resources/imm_ptl.mixins.json` declarait deja
`client.render.MixinCamera`, mais le filtre `processResources` de
`build.gradle` supprimait explicitement cette entree lorsque
`vanilla_core_compile=true`.

Le mixin contenait aussi deux injections historiques pour le fog et le rendu
du joueur en camera detachee. Elles n'etaient pas necessaires a `IECamera` et
ont ete retirees.

## Correctif minimal

- retrait de la seule exclusion Gradle de `client.render.MixinCamera` ;
- reduction de `MixinCamera` a la facade `IECamera` ;
- conservation de `ip_resetState`, `portal_setPos`,
  `portal_setFocusedEntity` et des accesseurs de hauteur camera ;
- migration du shadow `level` de `BlockGetter` vers `Level` pour Minecraft
  26.1 ;
- ajout du log runtime unique du statut `IECamera` ;
- ajout d'un fallback explicite lorsque `IEWorldRenderer` est absent.

Le JSON genere contient uniquement `MixinCamera` et `MixinGameRenderer` parmi
les mixins reactives de cette serie. Les mixins shader, fog, clouds,
LevelRenderer optionnel, Sodium, Iris et DimLib restent exclus.

## Incidents detectes et corriges

Le premier lancement a expose le changement de type du champ
`Camera.level`, passe de `BlockGetter` a `Level`.

La premiere tentative recursive avec `IECamera` actif a ensuite expose un
`ClassCastException` de `LevelRenderer` vers `IEWorldRenderer`. Aucun mixin
LevelRenderer n'a ete reactive ; une garde de fallback a ete ajoutee.

## Fichiers inspectes

- `build.gradle`
- `src/main/resources/imm_ptl.mixins.json`
- `build/resources/main/imm_ptl.mixins.json`
- `src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinCamera.java`
- `src/main/java/qouteall/imm_ptl/core/ducks/IECamera.java`
- `src/main/java/qouteall/imm_ptl/core/render/MyGameRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/render/renderer/RendererUsingFrameBuffer.java`
- `src/main/java/qouteall/imm_ptl/core/render/CrossPortalViewRendering.java`
- classe Minecraft 26.1 `net.minecraft.client.Camera`, inspectee avec `javap`

## Fichiers modifies

- `build.gradle`
- `src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinCamera.java`
- `src/main/java/qouteall/imm_ptl/core/render/renderer/RendererUsingFrameBuffer.java`
- `MIGRATION_PLAN_26.1.md`
- `PHASE5.2_ENABLE_CAMERA_MIXIN.md`

## Validation

Monde utilise : `Phase50Test`, avec portails minimaux sauvegardes.

- `compileJava` : BUILD SUCCESSFUL, 0 erreur
- `processResources` : BUILD SUCCESSFUL
- deux lancements finaux : BUILD SUCCESSFUL, aucun crash
- `IEGameRenderer` actif au runtime : oui
- `IECamera` actif au runtime : oui
- `/imm_ptl_debug create_minimal_test_portal` : fonctionne
- portail present cote client et recharge : oui
- cadre cyan fallback : conserve
- vue destination visible : non
- sauvegarde/rechargement : fonctionne
- `Duplicate entity UUID` sur les lancements finaux : 0
- traversee : chemin non modifie, validation 5.1 conservee ; la commande
  automatisee n'a pas ete resoumise de maniere fiable pendant les lancements
  finaux 5.2

## Contraintes

Sodium, Iris, DimLib, AlternateDimensions dynamique, stencil avance,
framebuffer avance complet, mixins shader et mixins LevelRenderer ne sont pas
reactives.
