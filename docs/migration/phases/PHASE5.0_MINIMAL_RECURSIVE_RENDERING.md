# Phase 5.0 - Rendu recursif minimal vanilla

Date : 12 juin 2026

## Resultat

Le chemin minimal de rendu recursif vanilla est prepare, mais la vue
destination ne peut pas encore etre affichee dans le profil vanilla actuel.
Le fallback cyan reste stable et explicite.

Le blocage technique exact est l'exclusion de `MixinGameRenderer` par le
filtre Gradle vanilla dans `build.gradle`. Au runtime, `GameRenderer`
n'implemente donc pas `IEGameRenderer`. Cette interface est necessaire au
chemin existant de `MyGameRenderer` pour changer la camera, la lightmap et le
contexte de rendu. Une tentative sans garde produit un `ClassCastException`.

Le chemin final detecte cette absence avant toute tentative risquee et logue :

```text
Minimal recursive portal rendering fallback: MixinGameRenderer is isolated in the vanilla profile
```

## Chemin prepare

- `IPCGlobal.useMinimalRecursivePortalRendering` active le mode Phase 5.0.
- `PortalEntityRenderer`, point runtime garanti actif, demande une tentative
  au renderer framebuffer une seule fois par frame.
- `RendererUsingFrameBuffer` limite le rendu a une couche et a la dimension
  courante, prepare un framebuffer secondaire et restaure son etat avec
  `try/finally`.
- `MyGameRenderer` contient l'appel controle a `GameRenderer.renderLevel`.
- `IPRenderPipelines` fournit un pipeline `POSITION_TEX` minimal.
- `MyRenderHelper.drawPortalAreaWithFramebuffer` prepare le rectangle texture
  avec le framebuffer destination.

Ces dernieres etapes ne sont pas executees tant que `IEGameRenderer` est
absent, afin de conserver un lancement stable.

## Ce qui est rendu

- portail synchronise cote client ;
- cadre cyan minimal et diagonales ;
- rendu vanilla normal autour du portail.

La vue destination reste en fallback/no-op. Aucun stencil avance, framebuffer
avance, clipping parfait ou recursion multiple n'est active.

## Fichiers inspectes

- `src/main/java/qouteall/imm_ptl/core/render/MyGameRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/render/PortalEntityRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/render/MyRenderHelper.java`
- `src/main/java/qouteall/imm_ptl/core/render/FrontClipping.java`
- `src/main/java/qouteall/imm_ptl/core/render/CrossPortalViewRendering.java`
- `src/main/java/qouteall/imm_ptl/core/render/GuiPortalRendering.java`
- `src/main/java/qouteall/imm_ptl/core/render/ViewAreaRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/render/SecondaryFrameBuffer.java`
- `src/main/java/qouteall/imm_ptl/core/render/pipeline/IPRenderPipelines.java`
- `src/main/java/qouteall/imm_ptl/core/render/renderer/PortalRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/render/renderer/RendererUsingFrameBuffer.java`
- `src/main/java/qouteall/imm_ptl/core/render/renderer/RendererUsingStencil.java`
- `src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinGameRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinLevelRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/ClientWorldLoader.java`
- `build.gradle`
- `build/resources/main/imm_ptl.mixins.json`

## Validation

Monde propre utilise : `Phase50Test`, sans donnees d'entites ni joueur
heritees.

- `compileJava` : BUILD SUCCESSFUL, 0 erreur
- `runClient` : BUILD SUCCESSFUL, aucun crash
- second lancement/rechargement : BUILD SUCCESSFUL, aucun crash
- `/imm_ptl_debug create_minimal_test_portal` : fonctionne
- portail present cote client : oui
- cadre cyan visible : oui
- vue destination visible : non, blocage documente ci-dessus
- `/imm_ptl_client_debug report_loaded_portals` : fonctionne
- `/imm_ptl_client_debug test_minimal_portal_traversal` : fonctionne
- traversee Overworld vers Overworld : oui
- sauvegarde/rechargement : reussi
- `Duplicate entity UUID` : 0

Capture du fallback :

`run/screenshots/2026-06-12_19.45.51.png`

## Contraintes

Sodium, Iris, DimLib, AlternateDimensions dynamique, stencil avance et
framebuffer avance complet ne sont pas reactives.
