# PHASE 10.4 - Audit render-graph / stencil-like sans glStencil

Date : 2026-06-30

## Objectif

Auditer une strategie de masque portail compatible Minecraft 26.1 sans utiliser
directement `glStencil*`, sans reactiver `RendererUsingStencil`, sans shader
clipping global et sans changer les modes framebuffer existants.

## Etat initial verifie

Commits presents :

- `404c5000 Stabilize Iris shaderpack fallback rendering baseline`
- `2fddda41 Add opt-in framebuffer order diagnostics`
- `e196758b Add opt-in no-depth portal geometry clip diagnostics`

Configuration :

- `run/config/iris.properties` : `shaderPack=MakeUp-UltraFast-9.5c.zip`
- aucun flag global `IMM_PTL_*` actif au debut de l'audit.

## Fichiers inspectes

- `src/main/java/qouteall/imm_ptl/core/render/pipeline/IPRenderPipelines.java`
- `src/main/java/qouteall/imm_ptl/core/render/renderer/RendererUsingFrameBuffer.java`
- `src/main/java/qouteall/imm_ptl/core/render/renderer/RendererUsingStencil.java`
- `src/main/java/qouteall/imm_ptl/core/render/PortalEntityRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/render/MyRenderHelper.java`
- `src/main/java/qouteall/imm_ptl/core/compat/IPPortingLibCompat.java`
- `src/main/java/qouteall/imm_ptl/core/mixin/client/render/framebuffer/MixinRenderTarget.java`
- `src/main/java/qouteall/imm_ptl/core/mixin/client/render/framebuffer/MixinMainTarget.java`
- `src/main/resources/assets/immersive_portals/shaders/core/*`
- classes Minecraft 26.1.2 via `javap` :
  - `com.mojang.blaze3d.pipeline.DepthStencilState`
  - `com.mojang.blaze3d.pipeline.RenderPipeline`
  - `com.mojang.blaze3d.systems.RenderPass`
  - `com.mojang.blaze3d.systems.CommandEncoder`
  - `com.mojang.blaze3d.pipeline.RenderTarget`
  - `com.mojang.blaze3d.textures.TextureFormat`
  - `net.minecraft.client.renderer.SubmitNodeCollector`

## API publique observee

`DepthStencilState` expose seulement :

- `depthTest`
- `writeDepth`
- `depthBiasScaleFactor`
- `depthBiasConstant`

Il n'expose pas de reference stencil, masque stencil, operation stencil ou
clear stencil.

`CommandEncoder` expose :

- creation de `RenderPass` avec color + optional depth ;
- clear color ;
- clear color + depth ;
- clear depth ;
- copie texture/buffer.

Il n'expose pas de clear stencil ni de render pass avec attachment stencil.

`RenderPass` expose :

- pipeline ;
- textures ;
- uniforms ;
- scissor ;
- vertex/index buffers ;
- draw.

Il n'expose pas de stencil state dynamique.

`TextureFormat` expose publiquement :

- `RGBA8`
- `RED8`
- `RED8I`
- `DEPTH32`

Aucun format public depth-stencil n'apparait dans cette API.

## Evaluation des pistes

### A - Stencil public RenderPipeline

Verdict : non disponible.

API necessaire :

- stencil compare op ;
- stencil write mask ;
- stencil ref ;
- stencil operations ;
- stencil attachment ou clear stencil.

Etat 26.1 observe :

- absent de `DepthStencilState` ;
- absent de `CommandEncoder` ;
- absent de `RenderPass` ;
- absent des formats publics `TextureFormat`.

Compatibilite :

- vanilla : impossible via API publique actuelle ;
- Sodium : impossible sans etat backend non public ;
- Iris : risque eleve, car shaderpack/backend peut remplacer les passes.

### B - Masque profondeur plus robuste

Verdict : deja exploite partiellement, utile mais limite.

Etat actuel :

- `PORTAL_DEPTH_MASK` ecrit la forme du portail dans le depth buffer ;
- les pipelines framebuffer testent `EQUAL`, `LEQUAL` ou `ALWAYS`.

Gain :

- stabilise l'occlusion du quad dans certains cas ;
- conserve l'integration SubmitNodeCollector.

Limite :

- ne decoupe pas le contenu deja rendu dans la texture destination ;
- `EQUAL` est fragile sous shaderpack ;
- `LEQUAL` est intermittent avec Complementary ;
- `no_depth` reste plus robuste visuellement mais degrade l'occlusion.

Risque :

- faible en vanilla/MakeUp ;
- moyen sous Iris shaderpack ;
- faible crash, mais gain visuel plafonne.

### C - Masque couleur/alpha separe

Verdict : piste possible, mais demande un vrai prototype de composition.

Principe :

- rendre la forme du portail dans une texture masque `RED8` ou `RGBA8` ;
- composer la texture framebuffer destination avec ce masque ;
- eviter le stencil GPU.

API necessaire :

- `TextureTarget`/`RenderTargetDescriptor` avec color target ;
- pipeline `POSITION_COLOR` ou `POSITION_TEX_COLOR` pour ecrire le masque ;
- pipeline de composition qui sample `Sampler0` framebuffer + `Sampler1` masque.

Compatibilite :

- vanilla : possible ;
- Sodium : probablement neutre si limite au quad portail ;
- Iris : risque moyen, car un pipeline custom multi-sampler doit etre mappe vers
  un shaderpack fallback. Le bridge Iris reflectif actuel mappe les pipelines
  minimaux vers `TEXTURED`/`BASIC_COLOR`, mais pas encore une composition
  multi-texture.

Gain attendu :

- peut limiter le quad compose a un masque texture ;
- ne corrige pas le clipping du monde destination lui-meme ;
- peut reduire certains debordements si la composition actuelle fuit hors zone.

Complexite : moyenne.

Test minimal :

- MakeUp default ;
- Complementary `no_depth` ;
- comparer avant/apres avec capture native.

### D - Render target intermediaire masque

Verdict : possible, mais plus lourd que C.

Principe :

- rendre le framebuffer destination ;
- rendre un masque portail ;
- composer les deux dans une texture intermediaire ;
- soumettre seulement la texture composee sur le quad portail.

Compatibilite :

- vanilla : possible ;
- Sodium : a priori compatible si le chemin reste hors chunk renderer ;
- Iris : risque moyen/eleve selon mapping du pipeline de composition.

Risque performance :

- une target et une passe supplementaires par portail rendu ;
- acceptable pour une seule recursion, a mesurer.

Gain attendu :

- meilleur controle de la couleur/alpha finale ;
- toujours pas un clipping des blocs avant rasterisation.

Complexite : moyenne a elevee.

### E - Passe composition avec texture masque

Verdict : meilleure piste Phase 10.5.

Pourquoi :

- ne depend pas d'un stencil public inexistant ;
- peut rester opt-in ;
- respecte l'architecture actuelle :
  - `GameRenderer.renderLevel` rend le framebuffer destination ;
  - `PortalEntityRenderer.submit` soumet la geometrie ;
  - pas de rendu monde reentrant ;
  - pas de `glStencil*`.

Prototype recommande :

- nouveau mode dev opt-in, par exemple
  `IMM_PTL_FRAMEBUFFER_MASK_MODE=alpha_texture` ;
- creer un `TextureTarget` masque `RGBA8` ou `RED8` ;
- rendre le quad portail blanc dans le masque, fond noir ;
- composer `framebuffer * mask` dans une texture intermediaire ou dans le shader
  de quad si un pipeline multi-sampler fiable est possible ;
- fallback cyan si le pipeline ou la texture masque manque.

Compatibilite :

- vanilla : bonne ;
- Sodium : bonne a moyenne ;
- Iris : moyenne, a condition d'ajouter un mapping reflectif dedie ou de
  commencer par vanilla/MakeUp seulement.

Risque :

- shaderpack : moyen ;
- crash : faible si opt-in et fallback ;
- performance : moyen.

### F - Geometrie de portail rendue dans texture masque

Verdict : sous-piste concrete de E.

Details :

- utiliser les memes sommets que `submitPortalAreaWithFramebuffer` ;
- ecrire blanc dans une petite target masque ;
- la cible doit etre dans le meme espace que la composition finale.

Limite importante :

- si le masque est rendu depuis le point de vue principal, il masque la surface
  visible du portail ;
- il ne supprime pas les objets du monde destination qui sont deja dans la
  texture, sauf si la composition les rejette hors zone d'ecran.

Complexite : moyenne.

### G - Portal area mask texture sans stencil

Verdict : variante la plus propre du masque texture.

Approche :

- creer une texture masque par frame ou par portail visible ;
- la dimensionner comme le framebuffer secondaire ou la fenetre ;
- dessiner la silhouette du portail dans cette texture ;
- composer ensuite framebuffer destination + masque.

Gain :

- remplace le role "limiter au rectangle portail" du stencil pour le quad final ;
- evite l'etat GL global.

Limite :

- ne remplace pas le clipping plan des shaders du monde destination ;
- plusieurs portails ou recursion multiple demanderont une gestion de masque par
  layer.

### H - Fallback no_depth + composition controlee

Verdict : bonne strategie pragmatique.

Rappel :

- `no_depth` est robuste sous Complementary ;
- il evite le depth mask fragile ;
- il degrade l'occlusion.

Piste :

- garder `no_depth` manuel ;
- ajouter un mode opt-in qui compose `no_depth` avec texture masque ;
- ne pas changer le default global ;
- mesurer si Complementary devient moins intermittent.

Risque :

- faible si le mode reste manuel ;
- moyen cote shaderpack si le pipeline composition multi-texture n'est pas bien
  mappe.

### I - Impossibilite pratique API publique actuelle

Verdict : vrai pour un stencil equivalent a l'ancien renderer, faux pour un
masque texture approximatif.

Impossible maintenant sans API privee/direct GL :

- stencil buffer public ;
- operations stencil ;
- clear stencil ;
- stencil par layer portal ;
- clipping general du monde destination.

Possible raisonnablement :

- masque couleur/alpha ;
- composition texture ;
- micro-prototype opt-in sans changer les modes existants.

## Pourquoi l'ancien RendererUsingStencil reste dangereux

`RendererUsingStencil` depend directement de :

- `GL11.glClearStencil`
- `GL11.glClear(GL_STENCIL_BUFFER_BIT)`
- `GL11.glEnable(GL_STENCIL_TEST)`
- `GL11.glStencilFunc`
- `GL11.glStencilOp`
- `GL11.glStencilMask`
- `GL11.glColorMask`
- `GL11.glDepthFunc`
- `GL11.glDepthRange`

Risques :

- etat GPU global hors render graph 26.1 ;
- cache d'etat Minecraft/Iris/Sodium non synchronise ;
- stencil buffer principal pas garanti ;
- `MixinRenderTarget`/`MixinMainTarget` modifient les formats attachments, ce qui
  est fragile avec les backends modernes ;
- interaction dangereuse avec Iris final pass et shaderpacks ;
- restoration d'etat difficile en cas d'exception ;
- non compatible avec la trajectoire SubmitNodeCollector actuelle.

Conclusion : ne pas reactiver l'ancien stencil complet.

## Pourquoi no_depth reste utile

`no_depth` :

- est le fallback manuel shaderpack-safe le plus robuste observe avec
  Complementary ;
- evite les modes `EQUAL`/`LEQUAL` qui flicker ou disparaissent selon le
  shaderpack ;
- conserve le quad SubmitNodeCollector et le framebuffer minimal.

Limite :

- occlusion degradee ;
- pas de decoupe du contenu framebuffer ;
- doit rester manuel et non promu automatiquement.

## Pourquoi portal_quad_only ne suffit pas

Phase 10.3 a confirme :

- bounds valides ;
- `width=2.0` ;
- `height=3.0` ;
- le quad est deja limite a la geometrie du portail ;
- les artefacts viennent du contenu deja rendu dans la texture framebuffer.

Donc un clipping CPU du quad ne peut pas regler le probleme principal.

## Decision

Stencil-like via API publique Minecraft 26.1 :

- vrai stencil public : non ;
- stencil equivalent sans shader/texture : non ;
- masque texture render-graph : oui, plausible ;
- clipping general du monde destination : non, pas sans pipeline shader dedie ou
  integration plus large.

## Recommandation Phase 10.5

Faire un micro-prototype opt-in de masque texture/composition, pas un retour au
stencil OpenGL.

Proposition :

- flag : `IMM_PTL_FRAMEBUFFER_MASK_MODE=off|alpha_texture`
- defaut : `off`
- cible initiale : Complementary `no_depth`
- pipeline masque :
  - rendre la silhouette du portail dans une texture masque couleur ;
  - composer framebuffer destination + masque ;
  - fallback vers chemin actuel si le pipeline est indisponible.

Tests minimaux :

- vanilla/MakeUp default reference ;
- Complementary `no_depth` reference ;
- Complementary `no_depth + alpha_texture` ;
- verifier :
  - crash : 0 ;
  - `Missing program` : 0 ;
  - `Buffer already closed` : 0 ;
  - `ConcurrentModificationException` : 0 ;
  - `Duplicate entity UUID` : 0 ;
  - capture native.

## Validation Phase 10.4

Aucun code runtime modifie.

Compilation non relancee, conformement a la consigne : l'audit ne change pas le
code.

Etat final :

- `run/config/iris.properties` reste sur `MakeUp-UltraFast-9.5c.zip` ;
- aucun flag global laisse actif ;
- `portal_quad_only` non promu ;
- `no_depth` non promu ;
- `RendererUsingStencil` non reactive ;
- aucun `glStencil*` ajoute.
