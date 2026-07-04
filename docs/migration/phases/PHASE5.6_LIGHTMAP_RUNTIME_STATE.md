# Phase 5.6 - Etat Lightmap runtime pendant le rendu recursif minimal

Date : 18 juin 2026

## Resultat

Le crash `GameRenderer.lightmap(...)` avec `this.lightmap == null` est corrige.

Le rendu recursif minimal n'efface plus le lightmap principal lorsque la
destination est la meme dimension que le monde courant. Le chemin de rendu
depasse donc le blocage `Lightmap` observe en phase 5.5.

La vue destination reste absente. Le nouveau blocage exact est la
non-reentrance de `LevelRenderer.submitEntities` lorsque le rendu recursif est
declenche depuis `PortalEntityRenderer.submit`.

Le fallback cyan est conserve sans crash :

```text
Minimal recursive portal rendering fallback: LevelRenderer entity submission is not reentrant yet
```

## Audit GameRenderer 26.1

Inspection `javap` de `net.minecraft.client.renderer.GameRenderer` :

- champ exact : `private final net.minecraft.client.renderer.Lightmap lightmap`
- initialise dans le constructeur par `new Lightmap()`
- `lightmap()` retourne `lightmap.getTextureView()` sauf si `useUiLightmap`
  est actif
- `levelLightmap()` retourne aussi `lightmap.getTextureView()`
- `tick()` utilise `LightmapRenderStateExtractor`
- le rendu de frame normal appelle `lightmap.render(...)` avant
  `renderLevel(...)`

L'ancienne facade `IEGameRenderer.ip_setLightmapTextureManager` reste utile
pour remplacer temporairement le champ final via mixin, mais elle etait
insuffisante seule car le code appelant lui passait parfois `null`.

## Cause du lightmap null

`DimensionRenderHelper` garde `lightmapTexture = null` quand son `world` est le
monde client courant. C'est intentionnel pour eviter de creer un lightmap
artificiel ou duplique pour le monde principal.

Dans le cas Overworld vers Overworld, `MyGameRenderer.switchAndRenderTheWorld`
faisait :

```java
ieGameRenderer.ip_setLightmapTextureManager(helper.lightmapTexture);
```

Comme `helper.lightmapTexture` etait `null`, le champ `GameRenderer.lightmap`
devenait nul pendant le rendu recursif.

## Correctif minimal

- conserver le lightmap principal lorsque le helper de destination n'a pas de
  lightmap dedie :

```java
Lightmap newLightmap = helper.lightmapTexture != null ? helper.lightmapTexture : oldLightmap;
```

- utiliser `newLightmap` pendant le swap de rendu ;
- ajouter une garde preflight cote framebuffer :

```text
Minimal recursive portal rendering fallback: GameRenderer lightmap is unavailable
```

Cette garde ne s'est pas declenchee pendant le lancement final, confirmant que
le lightmap etait valide.

## Nouveau blocage exact

Un lancement intermediaire, apres correction du lightmap mais avant garde de
reentrance, a expose :

```text
java.util.ConcurrentModificationException
at net.minecraft.client.renderer.LevelRenderer.submitEntities(LevelRenderer.java:850)
```

Le rendu recursif etait declenche depuis `PortalEntityRenderer.submit`, donc au
milieu de l'iteration vanilla des entites. Relancer `GameRenderer.renderLevel`
depuis ce point fait reentrer `LevelRenderer.submitEntities` et modifie la
collection en cours d'iteration.

Le correctif de stabilite de cette phase est de garder le chemin desactive via
un flag local explicite :

```java
private static final boolean ENABLE_ENTITY_SUBMIT_RECURSIVE_RENDER = false;
```

Le fallback documente le blocage sans crash. La prochaine etape devra deplacer
le rendu recursif minimal vers un hook non situe dans `submitEntities`, ou
isoler/capturer proprement la liste d'entites.

## Fichiers inspectes

- `net.minecraft.client.renderer.GameRenderer` 26.1 via `javap`
- `src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinGameRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/ducks/IEGameRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/render/MyGameRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/render/renderer/RendererUsingFrameBuffer.java`
- `src/main/java/qouteall/imm_ptl/core/ClientWorldLoader.java`
- `src/main/java/qouteall/imm_ptl/core/render/context_management/DimensionRenderHelper.java`

## Fichiers modifies

- `src/main/java/qouteall/imm_ptl/core/render/MyGameRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/render/renderer/RendererUsingFrameBuffer.java`
- `MIGRATION_PLAN_26.1.md`
- `PHASE5.6_LIGHTMAP_RUNTIME_STATE.md`

## Validation

- `compileJava` : BUILD SUCCESSFUL, 0 erreur
- `processResources` : BUILD SUCCESSFUL
- `runClient` final : BUILD SUCCESSFUL, aucun crash
- `IEGameRenderer` actif : oui
- `IECamera` actif : oui
- `IEWorldRenderer` actif : oui
- `IEParticleManager` actif : oui
- lightmap valide pendant le preflight recursif : oui
- portail present cote client : oui
- cadre cyan fallback : conserve
- sauvegarde a la fermeture : reussie
- `Duplicate entity UUID` : 0
- traversee : non retestee manuellement pendant ce lancement ; la logique de
  teleportation n'a pas ete modifiee dans cette phase
- vue destination visible : non

## Contraintes

Sodium, Iris, DimLib, shaders, `MixinGameRenderer_Shaders`,
`MixinShaderInstance`, `MixinProgram`, `MixinRenderSystem_Fog`,
`MixinRenderSystem_Clipping`, stencil avance et framebuffer avance complet ne
sont pas reactives.
