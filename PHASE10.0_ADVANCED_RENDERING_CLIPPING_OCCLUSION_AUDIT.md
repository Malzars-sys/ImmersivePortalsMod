# PHASE 10.0 - Audit du rendu avance, clipping et occlusion propre

Date : 2026-06-29

## Objectif

Auditer l'etat du rendu avance apres la baseline Iris shaderpack fallback, sans
modifier le renderer, les mappings Iris, les pipelines actifs ou les profils de
compatibilite.

Question centrale :

> Quelle est la prochaine micro-phase raisonnable pour ameliorer clipping et
> occlusion sans casser les baselines vanilla, Sodium non-shader et Iris
> shaderpack fallback ?

## Baseline de depart

Commits de reference observes :

- `5545115e` - `Stabilize vanilla minimal portal renderer baseline`
- `545169c1` - `Stabilize Sodium non-shader portal rendering baseline`
- `8c09eb37` - `Stabilize Iris shaderpack minimal portal rendering`
- `9a5e231a` - `Add experimental Iris framebuffer depth modes`
- `57968c54` - `Formalize manual Iris shaderpack-safe framebuffer fallback`
- `603d5020` - `Document LEQUAL visual instability under Complementary`
- `0ef37376` - `Document Iris shaderpack fallback traversal regression`
- `404c5000` - `Stabilize Iris shaderpack fallback rendering baseline`

Etat de configuration verifie :

- `run/config/iris.properties` est restaure sur
  `shaderPack=MakeUp-UltraFast-9.5c.zip`.
- Aucun flag global observe pour :
  - `IMM_PTL_FRAMEBUFFER_DEPTH_MODE`
  - `IMM_PTL_FORCE_FRAMEBUFFER_NO_DEPTH_MASK`
  - `IMM_PTL_AUTO_VISIBLE_TEST_PORTAL`
  - `IMM_PTL_AUTO_MINIMAL_TRAVERSAL_TEST`
  - `IMM_PTL_CAPTURE_MINIMAL_RECURSIVE_PORTAL`

## Fichiers inspectes

- `build.gradle`
- `MIGRATION_PLAN_26.1.md`
- `src/main/java/qouteall/imm_ptl/core/render/renderer/RendererUsingFrameBuffer.java`
- `src/main/java/qouteall/imm_ptl/core/render/renderer/RendererUsingStencil.java`
- `src/main/java/qouteall/imm_ptl/core/render/renderer/PortalRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/render/PortalEntityRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/render/MyRenderHelper.java`
- `src/main/java/qouteall/imm_ptl/core/render/MyGameRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/render/FrontClipping.java`
- `src/main/java/qouteall/imm_ptl/core/render/pipeline/IPRenderPipelines.java`
- `src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinGameRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinRenderSystem_Clipping.java`
- `src/main/java/qouteall/imm_ptl/core/compat/mixin/sodium/MixinSodiumDefaultShaderInterface.java`
- `src/main/java/qouteall/imm_ptl/core/compat/mixin/sodium/MixinSodiumShaderLoader.java`
- `src/main/java/qouteall/imm_ptl/core/compat/mixin/iris/MixinIrisSodiumShader.java`
- `src/main/java/qouteall/imm_ptl/core/compat/iris_compatibility/IrisPortalRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/compat/iris_compatibility/IrisCompatibilityPortalRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/compat/iris_compatibility/ExperimentalIrisPortalRenderer.java`

## Ce qui fonctionne aujourd'hui

Le chemin minimal stable est clair :

- `PortalEntityRenderer.submit(...)` collecte le portail visible et soumet le
  quad/cadre, sans relancer le rendu monde depuis l'iteration vanilla des
  entites.
- `MixinGameRenderer` declenche le rendu framebuffer depuis le hook
  `GameRenderer.renderLevel`, hors du chemin reentrant qui causait le risque de
  `ConcurrentModificationException`.
- `RendererUsingFrameBuffer` rend une seule recursion dans le framebuffer
  secondaire et conserve le cadre cyan comme fallback.
- `MyRenderHelper.submitPortalAreaWithFramebuffer(...)` soumet le quad texture via
  `SubmitNodeCollector`.
- `IPRenderPipelines` fournit les pipelines minimaux `POSITION_TEX` :
  - framebuffer sans profondeur ;
  - framebuffer `EQUAL` ;
  - framebuffer `LEQUAL` ;
  - framebuffer `ALWAYS` ;
  - masque profondeur du portail.
- Sous Iris, les pipelines minimaux sont raccordes par reflexion a des
  `ShaderKey` Iris simples :
  - depth mask -> `BASIC_COLOR` ;
  - framebuffer textured -> `TEXTURED`.
- Les modes experimentaux restent disponibles sans changer le defaut global :
  - `default` -> `depth-masked-equal` ;
  - `no_depth` -> fallback manuel shaderpack-safe ;
  - `lequal` -> visible mais intermittent sous Complementary ;
  - `always` -> diagnostic.
- Le fallback historique
  `IMM_PTL_FORCE_FRAMEBUFFER_NO_DEPTH_MASK=true` reste mappe vers `no_depth`
  seulement si le nouveau mode n'est pas defini.

Validations retenues des phases precedentes :

- MakeUp `default` : visible et traversable.
- Complementary `no_depth` : runtime/traversee valides, occlusion degradee.
- Complementary `lequal` : visuel possible mais intermittent.
- `Missing program` : 0 dans les tests shaderpack.
- `Buffer already closed` : 0.
- `ConcurrentModificationException` : 0.
- `Duplicate entity UUID` : 0.
- `UnsupportedOperationException` : 0.

## Ce qui est volontairement degrade

Le rendu minimal n'est pas encore un rendu Immersive Portals complet :

- une seule recursion ;
- fog vanilla fallback ;
- pas de clipping global des blocs, entites, particules et block entities ;
- pas de stencil avance ;
- pas de shader clipping ;
- pas de renderer Iris avance ;
- sous `no_depth`, la robustesse shaderpack est meilleure mais l'occlusion est
  volontairement reduite ;
- sous `default`, le masque profondeur est utile avec MakeUp, mais fragile avec
  certains shaderpacks deferred/temporaux comme Complementary.

## Ce qui reste desactive ou isole

Les exclusions restent correctes pour la baseline actuelle :

- `MixinRenderSystem_Clipping` est exclu du profil actif.
- `MixinRenderSystem_Fog` reste exclu.
- `MixinGameRenderer_Shaders`, `MixinProgram` et `MixinShaderInstance` restent
  exclus.
- `MixinSodiumDefaultShaderInterface` et `MixinSodiumShaderLoader` restent
  exclus.
- Les renderers Iris legacy sont des facades compile/runtime no-op :
  - `IrisPortalRenderer`
  - `IrisCompatibilityPortalRenderer`
  - `ExperimentalIrisPortalRenderer`
- DimLib et AlternateDimensions restent isoles.

## Limites techniques confirmees

### Clipping global

`FrontClipping` conserve le calcul de plan, mais le chemin actif est un
prefiltrage CPU minimal des entites portail. Les fonctions qui devraient pousser
un uniforme de clipping shader sont volontairement no-op tant qu'un pipeline
avec uniforme de plan n'existe pas.

`GL_CLIP_PLANE0` seul ne suffit pas avec les shaders vanilla 26.1/Iris, car les
pipelines modernes ne garantissent pas l'ecriture de `gl_ClipDistance` ni un plan
global compatible avec toutes les familles de rendu.

Conclusion : le clipping general ne peut pas etre restaure proprement par un
simple toggle global.

### Stencil

`RendererUsingStencil` existe encore, mais il repose sur l'ancien chemin direct
OpenGL :

- `glStencilFunc`
- `glStencilOp`
- `glStencilMask`
- `glClear(GL_STENCIL_BUFFER_BIT)`
- manipulations directes de color/depth mask
- dependance a l'etat stencil du framebuffer principal.

Ce chemin est dangereux a reactiver directement dans le render graph 26.1,
surtout avec Iris/Sodium et les shaderpacks.

Conclusion : un futur stencil doit etre reconstruit comme micro-prototype
capability-gated, pas reactive comme ancien renderer complet.

### RenderPipeline public 26.1

Le chemin public observe via `DepthStencilState` expose les comparaisons de
profondeur utiles, mais pas de controle stencil public exploitable pour le
pipeline minimal actuel. Le log runtime Phase 5.14 et le code indiquent :

`usable stencil=false (RenderPipeline DepthStencilState exposes depth only)`.

Conclusion : le stencil propre via API publique n'est pas disponible dans la
forme necessaire au renderer minimal.

### Iris shaderpack

Le pont Iris actuel est volontairement minimal :

- mapping reflectif de pipelines IP vers `ShaderKey` Iris simples ;
- pas de renderer Iris avance ;
- pas de shader transform global ;
- pas de shader clipping ;
- pas de Sodium shader mixins.

Complementary montre que les passes deferred/temporales peuvent rendre le
masque profondeur `EQUAL` ou `LEQUAL` intermittent. Le fallback `no_depth` est
donc le plus robuste, mais il perd l'occlusion.

Conclusion : le probleme Complementary est bien un conflit d'ordre/profondeur
ou de passes shaderpack, pas une absence de texture framebuffer.

## Options pour la suite

### Option A - Continuer a durcir le chemin minimal actuel

Contenu :

- garder `default`, `no_depth`, `lequal`, `always` ;
- ajouter seulement des diagnostics ou petits ajustements d'ordre/profondeur ;
- ne pas changer le defaut global ;
- garder `no_depth` manuel.

Risque : faible.

Gain : faible a moyen, mais protege les baselines.

### Option B - Clipping CPU/geometrique limite

Contenu :

- etendre prudemment le prefiltrage CPU autour du plan portail ;
- limiter davantage les portails/entites IP absurdes ;
- ne pas pretendre clipper terrain, block entities ou particules vanilla.

Risque : faible a moyen.

Gain : limite. Utile pour reduire certains artefacts, insuffisant pour un vrai
clipping de scene.

### Option C - Micro-experience ordre/profondeur shaderpack-safe

Contenu :

- tester l'ordre exact masque profondeur -> quad texture ;
- tester un decalage/protection de profondeur si possible ;
- comparer comportement MakeUp `default` et Complementary `no_depth/lequal` ;
- documenter sans changer le mode par defaut.

Risque : moyen mais contenu si tout reste opt-in.

Gain : meilleur candidat immediat pour ameliorer l'occlusion sans ouvrir les
gros systemes.

### Option D - Prototype stencil-like minimal

Contenu :

- ne pas reactiver `RendererUsingStencil` ;
- auditer une micro-passe stencil/profondeur capability-gated ;
- fallback immediat si l'etat stencil n'est pas disponible ou si Iris refuse le
  chemin.

Risque : eleve. Le render graph 26.1 et Iris peuvent invalider les assumptions
GL directes.

Gain : potentiellement important, mais pas la prochaine marche la plus sure.

### Option E - Shader clipping

Contenu :

- ajouter un plan de clipping dans les shaders/pipelines de rendu monde.

Risque : tres eleve maintenant. Il faudrait toucher plusieurs familles :

- terrain vanilla ;
- entites ;
- block entities ;
- particules ;
- lignes/debug ;
- translucent ;
- chemins Sodium ;
- chemins Iris/shaderpack.

Gain : vrai clipping, mais trop transversal avant de stabiliser davantage les
compatibilites.

### Option F - Deferer clipping/occlusion avancee

Contenu :

- figer la baseline actuelle ;
- garder MakeUp comme shaderpack principal ;
- garder Complementary en support partiel via `no_depth` manuel ;
- passer a une autre grande famille de migration.

Risque : faible.

Gain : aucun gain visuel immediat, mais stabilite maximale.

## Recommandation

Ne pas lancer maintenant le shader clipping global ni l'ancien stencil.

La prochaine phase recommandee est :

## Phase 10.1 - Micro-audit ordre/profondeur du quad framebuffer

Objectif propose :

- rester sur le renderer minimal actuel ;
- ne pas changer le defaut global ;
- ne pas promouvoir `no_depth` automatiquement ;
- ne pas reactiver shader/stencil avance ;
- comparer uniquement des variantes opt-in d'ordre/profondeur autour du masque
  minimal.

Travail concret propose :

1. Instrumenter proprement, sans spam, l'ordre :
   - soumission depth mask ;
   - soumission quad framebuffer ;
   - pipeline choisi ;
   - mode profondeur choisi ;
   - shaderpack actif si Iris est present.
2. Tester MakeUp `default` comme baseline principale.
3. Retester Complementary :
   - `default` ;
   - `lequal` ;
   - `no_depth`.
4. Explorer uniquement des micro-variantes opt-in :
   - depth mask avant/apres selon ce que permet `SubmitNodeCollector` ;
   - compare op existantes ;
   - depth write strictement limitee ;
   - aucun changement du fallback cyan.
5. Si aucune variante ne stabilise Complementary, documenter que le prochain
   vrai gain necessitera stencil/render graph ou shader clipping dedie.

Pourquoi cette option :

- elle garde les trois baselines intactes ;
- elle reste reversible ;
- elle attaque le probleme observe de clignotement/occlusion sans ouvrir Iris
  legacy, Sodium shader mixins ou DimLib ;
- elle preserve `no_depth` comme fallback manuel robuste.

## Validation Phase 10.0

Aucun changement de code runtime n'a ete effectue pendant cet audit.

Compilation non relancee : non necessaire pour ce rapport documentaire, car le
renderer et les ressources de runtime n'ont pas ete modifies.

## Conclusion

Le renderer minimal est sain et suffisamment robuste pour rester la base de
travail. Les limites actuelles ne viennent plus d'un probleme de framebuffer ou
de SubmitNodeCollector, mais du manque de clipping/occlusion avancee compatible
26.1 + Iris/Sodium.

La bonne prochaine marche est une micro-phase d'ordre/profondeur, pas une
reactivation brutale du stencil ou du shader clipping global.
