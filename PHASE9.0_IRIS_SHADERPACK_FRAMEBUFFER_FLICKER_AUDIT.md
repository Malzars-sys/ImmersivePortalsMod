# Phase 9.0 - Audit clignotement framebuffer Iris shaderpack

## Objectif

Auditer pourquoi le contenu framebuffer du portail minimal devient instable ou
clignote sous certains shaderpacks Iris, sans restaurer le renderer Iris avance
et sans modifier le renderer minimal valide en Phase 8.5.

## Etat Git et baseline

Derniers commits observes :

```text
45fc13b7 Document second Iris shaderpack portal test
8c09eb37 Stabilize Iris shaderpack minimal portal rendering
bd3a080c Stabilize Iris no-shaderpack portal rendering baseline
```

Baseline importante :

```text
8c09eb37 Stabilize Iris shaderpack minimal portal rendering
```

`run/config/iris.properties` a ete verifie puis restaure sur :

```text
shaderPack=MakeUp-UltraFast-9.5c.zip
```

## Fichiers inspectes

- `src/main/java/qouteall/imm_ptl/core/render/renderer/RendererUsingFrameBuffer.java`
- `src/main/java/qouteall/imm_ptl/core/render/PortalEntityRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/render/pipeline/IPRenderPipelines.java`
- `src/main/java/qouteall/imm_ptl/core/render/MyRenderHelper.java`
- `runclient-phase8.6-iris-second-shaderpack-visual.txt`
- `runclient-phase8.6-iris-second-shaderpack-traversal.txt`
- shaderpacks locaux :
  - `MakeUp-UltraFast-9.5c.zip`
  - `ComplementaryReimagined_r5.8.1.zip`

## Chemin de rendu audite

Le chemin minimal actuel est :

1. `PortalEntityRenderer.submit`
2. `RendererUsingFrameBuffer.queueMinimalPortalFromEntityRenderer`
3. hook non reentrant `GameRenderer.renderLevel`
4. rendu du framebuffer secondaire
5. retour dans `PortalEntityRenderer.submit`
6. passe depth-only du rectangle de portail :
   `PORTAL_DEPTH_MASK`
7. quad texture du framebuffer avec depth test `EQUAL` :
   `DRAW_FRAMEBUFFER_IN_AREA_DEPTH_MASKED`
8. cadre cyan minimal soumis ensuite comme lignes translucides

Les pipelines Iris fallback de Phase 8.2 sont :

- `PORTAL_DEPTH_MASK` -> `ShaderKey.BASIC_COLOR`
- `DRAW_FRAMEBUFFER_IN_AREA` -> `ShaderKey.TEXTURED`
- `DRAW_FRAMEBUFFER_IN_AREA_DEPTH_MASKED` -> `ShaderKey.TEXTURED`

## Test MakeUp

Shaderpack :

```text
MakeUp-UltraFast-9.5c.zip
```

Monde :

```text
Phase90IrisShaderpackFramebufferAuditMakeUp
```

Preparation :

- copie de `Phase81IrisShaderpackPortalTest`
- suppression des dossiers `entities`
- suppression de `session.lock`

Log :

```text
runclient-phase9.0-makeup-framebuffer-audit.txt
```

Resultat :

- shaderpack actif : oui
- profile Iris : `medium`
- mapping Iris fallback enregistre : oui
- `Missing program` : 0
- portail cree : oui
- `PortalEntityRenderer.submit` appele : oui
- framebuffer secondaire initialise : oui
- texture framebuffer disponible : oui, `854x480`
- depth mask applique : oui
- quad texture `SubmitNodeCollector` soumis : oui
- portail present cote client : oui
- crash Minecraft : 0
- `Duplicate entity UUID` : 0
- `ConcurrentModificationException` : 0
- `Buffer already closed` : 0
- `UnsupportedOperationException` : 0

Observation : MakeUp reste la baseline visuelle validee par la Phase 8.5.

## Test Complementary

Shaderpack :

```text
ComplementaryReimagined_r5.8.1.zip
```

Monde :

```text
Phase90IrisShaderpackFramebufferAuditComplementary
```

Preparation :

- copie de `Phase81IrisShaderpackPortalTest`
- suppression des dossiers `entities`
- suppression de `session.lock`

Log :

```text
runclient-phase9.0-complementary-framebuffer-audit.txt
```

Resultat :

- shaderpack actif : oui
- profile Iris : `HIGH`
- mapping Iris fallback enregistre : oui
- `Missing program` : 0
- portail cree : oui
- `PortalEntityRenderer.submit` appele : oui
- framebuffer secondaire initialise : oui
- texture framebuffer disponible : oui, `854x480`
- depth mask applique : oui
- quad texture `SubmitNodeCollector` soumis : oui
- portail present cote client : oui
- cadre cyan visible : oui
- contenu framebuffer visible : intermittent
- clignotement reproduit : oui, confirme par observation utilisateur
- crash Minecraft : 0
- `Duplicate entity UUID` : 0
- `ConcurrentModificationException` : 0
- `Buffer already closed` : 0
- `UnsupportedOperationException` : 0

## Comparaison shaderpacks

Les logs Immersive Portals sont equivalentes entre MakeUp et Complementary :

- la texture framebuffer est disponible ;
- le depth mask est applique ;
- le quad texture est soumis ;
- le portail est present cote client ;
- aucune erreur d'override Iris ne reste.

La difference apparente est cote shaderpack :

- MakeUp charge le profil `medium` ;
- Complementary charge le profil `HIGH` ;
- Complementary declare davantage de passes `composite`, `deferred`,
  `gbuffers`, et des reglages TAA/camera/depth plus agressifs ;
- Complementary emet aussi des warnings propres a son block ID map, non
  bloquants mais indiquant un pipeline shaderpack distinct.

## Cause probable

Cause probable principale :

**B + D + G : interaction entre le depth mask minimal, le pipeline shaderpack
Iris et l'etat GPU/post-process du shaderpack.**

Le probleme n'est probablement pas :

- une texture framebuffer absente ;
- une erreur `Missing program` ;
- une non-soumission du quad ;
- un crash renderer ;
- un probleme de synchronisation portail.

La texture existe et le quad est soumis. Le cadre cyan reste visible car il est
dessine comme geometrie ligne translucide stable, alors que le contenu
framebuffer passe par un RenderType texture + depth compare `EQUAL`. Sous
Complementary, ce contenu peut etre masque ou perturbe par les passes
shaderpack/deferred/temporales, ou par une difference de profondeur exacte entre
la passe depth-only et la passe texture.

Hypotheses classees :

- A. ordre des passes : possible, mais moins probable car MakeUp fonctionne avec
  le meme ordre ;
- B. depth mask : probable, `EQUAL` est fragile sous shaderpack ;
- C. texture framebuffer : peu probable, logs texture OK ;
- D. pipeline Iris/ShaderKey insuffisant : probable a moyen terme, car
  `ShaderKey.TEXTURED` est un fallback generique et pas un vrai pipeline
  shaderpack portal-aware ;
- E. specifique Complementary : probable ;
- F. timing/capture : partiel, mais l'observation interactive confirme le
  clignotement ;
- G. etat GPU non restaure/post-process : probable ;
- H. camera/rendu recursif minimal incomplet : possible mais pas specifique a
  Complementary.

## Micro-experiences

Aucune micro-experience de code n'a ete appliquee pendant cette phase. Le but
etait de comparer les deux shaderpacks sans casser la baseline MakeUp. Les deux
runs suffisent a isoler le probleme comme specificite shaderpack/post-process,
car le chemin Immersive Portals minimal est identique dans les logs.

## Recommandation Phase 9.1

Faire une micro-experience unique et reversible sur le depth mask :

1. ajouter un flag dev temporaire pour forcer le quad framebuffer non masque
   (`DRAW_FRAMEBUFFER_IN_AREA`) sous shaderpack ;
2. tester MakeUp puis Complementary ;
3. comparer :
   - contenu framebuffer visible plus souvent ?
   - debordement visuel plus fort ?
   - clignotement reduit ?

Si le clignotement disparait sans depth mask, le probleme est confirme cote
`CompareOp.EQUAL`/profondeur shaderpack. Si le clignotement reste, la piste
prioritaire devient `ShaderKey.TEXTURED` insuffisant ou post-process
Complementary incompatible avec l'alias framebuffer actuel.

## Conclusion

MakeUp reste la baseline shaderpack validee.

Complementary est fonctionnel cote logique et pipeline minimal, mais seulement
partiellement compatible visuellement : le contenu framebuffer du portail est
intermittent. La cause la plus probable est une interaction entre le depth mask
minimal, le fallback `ShaderKey.TEXTURED` et les passes shaderpack
deferred/temporales de Complementary.
