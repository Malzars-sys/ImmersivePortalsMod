# Phase 9.1 - Micro-experience framebuffer sans depth mask

## Objectif

Tester une seule hypothese : le clignotement du contenu framebuffer sous
Complementary vient-il principalement du chemin depth mask minimal
`CompareOp.EQUAL` ?

## Modification appliquee

Un flag dev a ete ajoute au chemin minimal framebuffer :

```text
IMM_PTL_FORCE_FRAMEBUFFER_NO_DEPTH_MASK=true
```

Portee :

- uniquement `RendererUsingFrameBuffer.renderPortalInEntityRenderer(...)` ;
- uniquement le rendu minimal framebuffer du portail ;
- par defaut : desactive ;
- comportement normal Phase 8.5/9.0 conserve quand le flag est absent.

Effet avec le flag actif :

- ne pas utiliser `DRAW_FRAMEBUFFER_IN_AREA_DEPTH_MASKED` ;
- ne pas executer la passe `PORTAL_DEPTH_MASK` ;
- utiliser `DRAW_FRAMEBUFFER_IN_AREA` ;
- garder le cadre cyan inchange.

Logs ajoutes :

- `Minimal recursive portal no-depth-mask experiment active: true/false`
- `Minimal recursive portal framebuffer pipeline mode: depth-masked/non-depth-masked`

## Compilation

- vanilla : `BUILD SUCCESSFUL`
- Sodium compile-only : `BUILD SUCCESSFUL`
- Iris compile-only : `BUILD SUCCESSFUL`

Logs :

```text
compile-phase9.1-vanilla.txt
compile-phase9.1-sodium.txt
compile-phase9.1-iris.txt
```

## Mondes de test

Chaque monde est une copie nettoyee de `Phase81IrisShaderpackPortalTest` avec
suppression des dossiers `entities` et de `session.lock`.

- `Phase91MakeUpDepthBaseline`
- `Phase91MakeUpNoDepthMask`
- `Phase91ComplementaryDepthBaseline`
- `Phase91ComplementaryNoDepthMask`

## Resultats

### MakeUp - depth baseline

Log :

```text
runclient-phase9.1-makeup-depth-baseline.txt
```

Resultat :

- shaderpack actif : `MakeUp-UltraFast-9.5c.zip`
- flag no-depth-mask : false
- pipeline framebuffer : `depth-masked`
- texture framebuffer : `854x480`
- depth mask applique : oui
- quad SubmitNodeCollector soumis : oui
- portail present cote client : oui
- `Missing program` : 0
- crash/CME/buffer ferme/UUID duplique/UOE : 0
- `BUILD SUCCESSFUL`

### MakeUp - no depth mask

Log :

```text
runclient-phase9.1-makeup-no-depth-mask.txt
```

Resultat :

- shaderpack actif : `MakeUp-UltraFast-9.5c.zip`
- flag no-depth-mask : true
- pipeline framebuffer : `non-depth-masked`
- texture framebuffer : `854x480`
- depth mask applique : non
- quad SubmitNodeCollector soumis : oui
- portail present cote client : oui
- `Missing program` : 0
- crash/CME/buffer ferme/UUID duplique/UOE : 0
- `BUILD SUCCESSFUL`

Conclusion MakeUp : le flag ne crash pas et le comportement par defaut reste
identique quand il est absent.

### Complementary - depth baseline

Log :

```text
runclient-phase9.1-complementary-depth-baseline.txt
```

Resultat :

- shaderpack actif : `ComplementaryReimagined_r5.8.1.zip`
- flag no-depth-mask : false
- pipeline framebuffer : `depth-masked`
- texture framebuffer : `854x480`
- depth mask applique : oui
- quad SubmitNodeCollector soumis : oui
- portail present cote client : oui
- rendu observe precedemment : clignotant/intermittent, parfois seulement cadre
  cyan
- `Missing program` : 0
- crash/CME/buffer ferme/UUID duplique/UOE : 0
- `BUILD SUCCESSFUL`

### Complementary - no depth mask

Log :

```text
runclient-phase9.1-complementary-no-depth-mask.txt
```

Resultat :

- shaderpack actif : `ComplementaryReimagined_r5.8.1.zip`
- flag no-depth-mask : true
- pipeline framebuffer : `non-depth-masked`
- texture framebuffer : `854x480`
- depth mask applique : non
- quad SubmitNodeCollector soumis : oui
- portail present cote client : oui
- capture F2 obtenue : `run/screenshots/2026-06-28_20.52.46.png`
- contenu framebuffer visible dans la capture : oui
- `Missing program` : 0
- crash/CME/buffer ferme/UUID duplique/UOE : 0
- `BUILD SUCCESSFUL`

## Interpretation

La micro-experience confirme fortement la piste depth mask / `CompareOp.EQUAL`.

Avec Complementary et le chemin depth-masked, le contenu framebuffer est
intermittent et peut disparaitre au moment de la capture. Avec le flag
no-depth-mask, le pipeline passe explicitement en `non-depth-masked` et une
capture F2 montre du contenu framebuffer visible.

Ce n'est pas une preuve que le mode sans depth mask est un rendu final correct :
il peut deborder visuellement, perdre l'occlusion et rester imparfait. Mais il
isole le clignotement principal autour de la comparaison de profondeur exacte
et/ou de son interaction avec les passes shaderpack.

## Conclusion

Hypothese validee : le clignotement Complementary vient principalement du chemin
depth mask minimal `CompareOp.EQUAL` sous shaderpack.

MakeUp reste la baseline shaderpack validee. Complementary reste partiellement
compatible, mais le mode no-depth-mask montre une piste claire pour Phase 9.2.

## Proposition Phase 9.2

Concevoir un chemin depth compatible shaderpack, par exemple :

1. conserver le flag no-depth-mask comme diagnostic seulement ;
2. tester un depth compare moins strict que `EQUAL`, si l'API le permet ;
3. separer la passe visuelle framebuffer de l'occlusion stricte ;
4. documenter les artefacts acceptables pour un fallback shaderpack.
