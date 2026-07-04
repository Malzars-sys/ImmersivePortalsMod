# Phase 8.2 - Iris pipeline override audit

Date : 28 juin 2026

## Objectif

Auditer pourquoi Iris avec shaderpack signalait deux pipelines Immersive Portals
comme absents de sa liste d'overrides, sans restaurer le renderer Iris avance et
sans casser le portail fonctionnel de la Phase 8.1.

Pipelines concernes au depart :

- `minecraft:pipeline/imm_ptl_portal_depth_mask`
- `minecraft:pipeline/imm_ptl_draw_framebuffer_in_area_depth_masked`

## Fichiers inspectes

- `src/main/java/qouteall/imm_ptl/core/render/pipeline/IPRenderPipelines.java`
- `src/main/java/qouteall/imm_ptl/core/render/renderer/RendererUsingFrameBuffer.java`
- `runclient-phase8.1-iris-shaderpack-portal.txt`
- jar local Iris :
  `iris-1.10.8+26.1-fabric.jar`
- classes Iris inspectees avec `javap` :
  - `net.irisshaders.iris.mixin.MixinShaderManager_Overrides`
  - `net.irisshaders.iris.pipeline.IrisPipelines`
  - `net.irisshaders.iris.pipeline.programs.ShaderKey`
  - `net.irisshaders.iris.pipeline.programs.ShaderMap`
  - `net.irisshaders.iris.pipeline.programs.ShaderOverrides`
  - `net.irisshaders.iris.mixin.MixinRenderPipeline`

## Diagnostic Iris

Le message vient de `MixinShaderManager_Overrides.redirectIrisProgram`.

Chemin runtime :

```text
RenderType.draw
MultiBufferSource.BufferSource.endBatch
GlRenderPass.setPipeline
GlDevice.getOrCompilePipeline
Iris redirectIrisProgram
```

Iris fait ceci quand un shaderpack est actif :

1. obtient le pipeline Iris courant ;
2. verifie `shouldOverrideShaders()`;
3. ignore seulement certains pipelines internes ;
4. appelle `IrisPipelines.getPipeline(irisPipeline, renderPipeline)`;
5. si aucun `ShaderKey` n'est trouve, logge `Missing program ... in override list`.

La table d'override est interne a Iris :

```text
IrisPipelines.coreShaderMap
IrisPipelines.coreShaderMapShadow
```

Elle contient les pipelines vanilla connus. Les pipelines custom Immersive
Portals n'y sont pas enregistres, donc `getPipeline(...)` retourne `null`.

Le log est encore plus bruyant parce que nos pipelines ont ete crees avec :

```java
.withLocation("pipeline/...")
```

ce qui donne un identifiant `minecraft:pipeline/...`. Pour le namespace
`minecraft`, Iris logge ce cas comme fatal/interne, meme si le client continue.

## Reponses au diagnostic demande

A. Les pipelines doivent-ils etre declares cote Iris ?

Oui. Iris doit connaitre le `RenderPipeline` et le mapper vers un `ShaderKey`.
Sans ce mapping, le shaderpack ne sait pas quel programme utiliser.

B. Les pipelines doivent-ils etre declares cote shaderpack ?

Pas directement pour ce correctif minimal. Iris expose deja des `ShaderKey`
standard (`TEXTURED`, `BASIC_COLOR`, etc.) qui pointent vers les programmes du
shaderpack. Le plus petit correctif est donc de mapper nos pipelines vers ces
cles Iris existantes.

C. Peut-on leur donner un fallback vanilla-safe ?

Oui, via `IrisPipelines.assignPipeline(RenderPipeline, ShaderKey)`.
Le code l'appelle par reflexion pour eviter une dependance directe a Iris dans
les profils vanilla et Sodium-only.

D. Peut-on les rediriger vers un programme existant accepte par Iris ?

Oui :

- `imm_ptl_portal_depth_mask` -> `ShaderKey.BASIC_COLOR`
- `imm_ptl_draw_framebuffer_in_area_depth_masked` -> `ShaderKey.TEXTURED`
- `imm_ptl_draw_framebuffer_in_area` -> `ShaderKey.TEXTURED`

E. Le probleme vient-il uniquement du depth mask ?

Non. Les deux pipelines depth-mask etaient visibles en Phase 8.1 parce que la
voie depth-mask etait disponible. Le pipeline non masque aurait eu le meme
probleme si le fallback sans masque avait ete utilise.

F. Le probleme vient-il du nom `minecraft:pipeline/...` ?

Partiellement. Le vrai probleme est l'absence du pipeline dans
`IrisPipelines.coreShaderMap`. Le namespace `minecraft` rend seulement le log
plus severe cote Iris.

G. Le probleme vient-il de `SubmitNodeCollector` ?

Non. `SubmitNodeCollector` soumet correctement le quad. L'erreur apparait plus
tard, au moment ou Iris redirige le `RenderPipeline` vers un programme shader.

H. Le probleme vient-il de la texture framebuffer dynamique ?

Non pour l'erreur `Missing program`. La texture framebuffer est disponible et
liee via l'alias `AbstractTexture`. Le probleme etait la selection du programme
shader Iris.

## Correctif minimal applique

Fichier modifie :

```text
src/main/java/qouteall/imm_ptl/core/render/pipeline/IPRenderPipelines.java
```

Ajout :

- detection Iris par reflexion ;
- appel reflechi a :
  `net.irisshaders.iris.pipeline.IrisPipelines.assignPipeline(...)`;
- mapping des pipelines minimaux Immersive Portals vers des `ShaderKey` Iris
  standards ;
- log unique :
  `Registered Iris shaderpack fallback mappings for minimal portal pipelines`.

Ce correctif :

- ne depend pas directement d'Iris a la compilation vanilla ;
- ne reactive pas le renderer Iris avance ;
- ne reactive pas shader clipping ;
- ne reactive pas les mixins shader Sodium ;
- ne reactive pas DimLib ni AlternateDimensions.

## Pipelines audites

### `DRAW_FRAMEBUFFER_IN_AREA`

- Java : `IPRenderPipelines.Slot.DRAW_FRAMEBUFFER_IN_AREA`
- ResourceLocation : `minecraft:pipeline/imm_ptl_draw_framebuffer_in_area`
- vertex format : `DefaultVertexFormat.POSITION_TEX`
- vertex shader : `core/position_tex`
- fragment shader : `core/position_tex`
- sampler : `Sampler0`
- depth state : vanilla/default
- texture : framebuffer secondaire via alias `imm_ptl:minimal_portal_framebuffer`
- usage : fallback quad texture sans masque profondeur
- mapping Iris : `ShaderKey.TEXTURED`

### `DRAW_FRAMEBUFFER_IN_AREA_DEPTH_MASKED`

- Java : `IPRenderPipelines.Slot.DRAW_FRAMEBUFFER_IN_AREA_DEPTH_MASKED`
- ResourceLocation :
  `minecraft:pipeline/imm_ptl_draw_framebuffer_in_area_depth_masked`
- vertex format : `DefaultVertexFormat.POSITION_TEX`
- vertex shader : `core/position_tex`
- fragment shader : `core/position_tex`
- sampler : `Sampler0`
- depth test : `CompareOp.EQUAL`
- depth write : `false`
- texture : framebuffer secondaire via alias `imm_ptl:minimal_portal_framebuffer`
- usage : quad texture soumis apres masque profondeur
- mapping Iris : `ShaderKey.TEXTURED`

### `PORTAL_DEPTH_MASK`

- Java : `IPRenderPipelines.Slot.PORTAL_DEPTH_MASK`
- ResourceLocation : `minecraft:pipeline/imm_ptl_portal_depth_mask`
- vertex format : `DefaultVertexFormat.POSITION_COLOR`
- vertex shader : `core/position_color`
- fragment shader : `core/position_color`
- color target : write none
- depth test : `CompareOp.LESS_THAN_OR_EQUAL`
- depth write : `true`
- texture : aucune
- usage : rectangle depth-only avant le quad texture
- mapping Iris : `ShaderKey.BASIC_COLOR`

## Validations

- Vanilla :
  `.\gradlew.bat compileJava processResources --rerun-tasks --console=plain`
  - resultat : BUILD SUCCESSFUL
  - log : `compile-phase8.2-vanilla.txt`

- Sodium compile-only :
  `.\gradlew.bat compileJava processResources --rerun-tasks --console=plain -Penable_sodium_compat=true`
  - resultat : BUILD SUCCESSFUL
  - log : `compile-phase8.2-sodium.txt`

- Iris compile-only :
  `.\gradlew.bat compileJava processResources --rerun-tasks --console=plain -Penable_iris_compat=true`
  - resultat : BUILD SUCCESSFUL
  - log : `compile-phase8.2-iris.txt`

## Runtime Iris + shaderpack + portail

Commande :

```powershell
.\gradlew.bat runClient --console=plain -Penable_iris_compat=true -Penable_iris=true --args=--quickPlaySingleplayer=Phase81IrisShaderpackPortalTest
```

Flags :

- `IMM_PTL_AUTO_VISIBLE_TEST_PORTAL=true`
- `IMM_PTL_AUTO_MINIMAL_TRAVERSAL_TEST=true`
- `IMM_PTL_CAPTURE_MINIMAL_RECURSIVE_PORTAL=true`
- `IMM_PTL_MINIMAL_RECURSIVE_PORTAL_SCREENSHOT=phase8.2-iris-pipeline-override-audit.png`

Resultat :

- runClient : BUILD SUCCESSFUL
- shaderpack actif : `MakeUp-UltraFast-9.5c.zip`
- mapping Iris enregistre : oui
- portail cree : oui
- portail present cote client : oui
- `PortalEntityRenderer.submit` appele : oui
- framebuffer minimal atteint : oui
- texture framebuffer disponible : oui, `854x480`
- quad `SubmitNodeCollector` soumis : oui
- erreurs `Missing program` : 0 dans le run final
- capture obtenue : oui
- traversee : oui
- `Client Teleported Statically` : oui
- fermeture normale : oui
- crash : 0
- erreur mixin bloquante : 0
- `UnsupportedOperationException` : 0
- `Duplicate entity UUID` : 0
- `ConcurrentModificationException` : 0
- `Buffer already closed` : 0

Logs :

- `runclient-phase8.2-iris-shaderpack-portal.txt`
- `runclient-phase8.2-iris-shaderpack-portal-stderr.txt`

Capture :

```text
run/screenshots/phase8.2-iris-pipeline-override-audit.png
```

Observation visuelle : la capture automatique reste tres sombre et n'est pas
concluante. Elle ne doit pas etre interpretee comme une preuve d'invisibilite :
une observation interactive apres rotation de la camera a permis de voir le
portail. La Phase 8.2 corrige le blocage Iris `Missing program`, mais la preuve
visuelle automatique doit encore etre amelioree.

## Conclusion

Cause exacte : Iris ne connaissait pas les pipelines RenderPipeline custom
Immersive Portals dans `IrisPipelines.coreShaderMap`, donc il ne pouvait pas
les associer a un `ShaderKey` de shaderpack.

Correction minimale possible maintenant : oui. Elle est appliquee par reflexion
dans `IPRenderPipelines` et mappe les trois pipelines minimaux vers des
`ShaderKey` Iris standards.

Etat final : le portail reste fonctionnel et traversable sous shaderpack, les
erreurs `Missing program` sont supprimees, et le rendu avance Iris reste
volontairement non restaure.

Suite recommandee : Phase 8.3, observation visuelle controlee apres suppression
des erreurs d'override, avec une capture plus fiable face au portail et une
distinction explicite entre observation interactive et capture automatique.
