# PHASE 10.2 - Validation visuelle ciblee des variantes ordre/profondeur

Date : 2026-06-30

## Objectif

Comparer visuellement les variantes `IMM_PTL_FRAMEBUFFER_ORDER_MODE` sous
Complementary, sans modifier le renderer et sans changer le comportement par
defaut.

## Contraintes respectees

- aucun changement de code ;
- defaut global conserve ;
- `no_depth` non promu automatiquement ;
- `quad_first` non promu ;
- `no_mask_reference` non promu ;
- aucun stencil reactive ;
- aucun shader clipping reactive ;
- aucun mixin shader Sodium active ;
- aucun renderer Iris legacy restaure ;
- DimLib et AlternateDimensions restent isoles.

## Etat initial

- commit baseline Iris fallback present : `404c5000`
- commit Phase 10.1 present : `2fddda41`
- `run/config/iris.properties` commencait sur
  `shaderPack=MakeUp-UltraFast-9.5c.zip`
- aucun flag global cible n'etait actif.

## Methode

Shaderpack teste :

- `ComplementaryReimagined_r5.8.1.zip`

Flags communs :

- `IMM_PTL_AUTO_VISIBLE_TEST_PORTAL=true`
- `IMM_PTL_AUTO_MINIMAL_TRAVERSAL_TEST=false`

Les captures F2 automatisees ont ete tentees, mais elles etaient soit prises sur
`Loading terrain`, soit absentes. Les captures de fenetre Windows ont aussi ete
rejetees, car elles ont capture une autre fenetre. Pour eviter une fausse preuve
visuelle, la validation finale utilise le mecanisme de capture native du render
target deja present :

- `IMM_PTL_CAPTURE_MINIMAL_RECURSIVE_PORTAL=true`
- `IMM_PTL_MINIMAL_RECURSIVE_PORTAL_SCREENSHOT=<nom>`

Ce fallback ne modifie pas le renderer et capture directement l'image Minecraft
produite apres soumission du quad framebuffer.

## Tests realises

### A - default/default

Monde : `Phase102ComplementaryDefaultVisual`

Configuration :

- `IMM_PTL_FRAMEBUFFER_DEPTH_MODE=default`
- `IMM_PTL_FRAMEBUFFER_ORDER_MODE` absent

Log : `runclient-phase10.2-complementary-default-visual.txt`

Capture : `run/screenshots/phase10.2-complementary-default.png`

Resultat :

- shaderpack actif : Complementary ;
- pipeline : `depth-masked-equal` ;
- framebuffer : `854x480` ;
- depth mask applique ;
- quad SubmitNodeCollector soumis ;
- portail present cote client ;
- contenu framebuffer : tres sombre/intermittent, surtout cadres imbriques ;
- occlusion : partielle/non fiable ;
- crash : 0.

### B - default/mask_first_explicit

Monde : `Phase102ComplementaryMaskFirstVisual`

Configuration :

- `IMM_PTL_FRAMEBUFFER_DEPTH_MODE=default`
- `IMM_PTL_FRAMEBUFFER_ORDER_MODE=mask_first_explicit`

Log : `runclient-phase10.2-complementary-mask-first-visual.txt`

Capture : `run/screenshots/phase10.2-complementary-mask-first.png`

Resultat :

- pipeline : `depth-masked-equal` ;
- ordre : depth mask puis quad, explicitement force ;
- framebuffer : `854x480` ;
- quad soumis ;
- capture principale : aucune amelioration visible, frame sans portail utile ;
- capture de repetition : cadre visible mais contenu framebuffer sombre/absent ;
- occlusion : non fiable ;
- crash : 0.

Interpretation :

`mask_first_explicit` confirme que l'ordre actuel est reproductible, mais ne
stabilise pas Complementary.

### C - default/quad_first

Monde : `Phase102ComplementaryQuadFirstVisual`

Configuration :

- `IMM_PTL_FRAMEBUFFER_DEPTH_MODE=default`
- `IMM_PTL_FRAMEBUFFER_ORDER_MODE=quad_first`

Log : `runclient-phase10.2-complementary-quad-first-visual.txt`

Capture : `run/screenshots/phase10.2-complementary-quad-first.png`

Resultat :

- pipeline : `depth-masked-equal` ;
- ordre : quad puis depth mask ;
- framebuffer : `854x480` ;
- quad soumis ;
- premiere capture : cadre/imbrique visible, mais contenu sombre ;
- repetition : frame sans portail utile ;
- clignotement/intermittence : oui ;
- occlusion : non fiable ;
- crash : 0.

Interpretation :

`quad_first` est techniquement executable, mais ne donne pas une amelioration
stable. Il ne doit pas devenir un candidat automatique.

### D - default/no_mask_reference

Monde : `Phase102ComplementaryNoMaskReferenceVisual`

Configuration :

- `IMM_PTL_FRAMEBUFFER_DEPTH_MODE=default`
- `IMM_PTL_FRAMEBUFFER_ORDER_MODE=no_mask_reference`

Log : `runclient-phase10.2-complementary-no-mask-reference-visual.txt`

Capture : `run/screenshots/phase10.2-complementary-no-mask-reference.png`

Resultat :

- pipeline : `non-depth-masked` ;
- depth mask desactive par ordre opt-in ;
- framebuffer : `854x480` ;
- quad soumis ;
- contenu framebuffer : clairement visible ;
- cadre cyan : visible ;
- clignotement observe dans cette capture : non ;
- occlusion : degradee, car absence de depth mask ;
- crash : 0.

Interpretation :

`no_mask_reference` confirme que le probleme visuel de Complementary est bien
lie au masque profondeur strict ou a son interaction avec les passes shaderpack.
Le contenu devient visible quand le depth mask est retire.

### E - no_depth/default

Monde : `Phase102ComplementaryNoDepthVisual`

Configuration :

- `IMM_PTL_FRAMEBUFFER_DEPTH_MODE=no_depth`
- `IMM_PTL_FRAMEBUFFER_ORDER_MODE` absent

Log : `runclient-phase10.2-complementary-no-depth-visual.txt`

Capture : `run/screenshots/phase10.2-complementary-no-depth.png`

Resultat :

- pipeline : `non-depth-masked` ;
- depth mask intentionnellement desactive ;
- framebuffer : `854x480` ;
- quad soumis ;
- contenu framebuffer : clairement visible ;
- cadre cyan : visible ;
- clignotement observe dans cette capture : non ;
- occlusion : degradee mais stable ;
- crash : 0.

Interpretation :

`no_depth` reste le fallback manuel shaderpack-safe le plus robuste connu pour
Complementary.

## Erreurs recherchees

Sur tous les logs Phase 10.2 :

- `Missing program` : 0 ;
- `Buffer already closed` : 0 ;
- `ConcurrentModificationException` : 0 ;
- `Duplicate entity UUID` : 0 ;
- `UnsupportedOperationException` : 0 ;
- `BUILD FAILED` : 0 ;
- crash report : 0.

## Captures utiles

Captures natives utiles :

- `run/screenshots/phase10.2-complementary-default.png`
- `run/screenshots/phase10.2-complementary-mask-first.png`
- `run/screenshots/phase10.2-complementary-quad-first.png`
- `run/screenshots/phase10.2-complementary-no-mask-reference.png`
- `run/screenshots/phase10.2-complementary-no-depth.png`
- `run/screenshots/phase10.2-complementary-default-repeat.png`
- `run/screenshots/phase10.2-complementary-mask-first-repeat.png`
- `run/screenshots/phase10.2-complementary-quad-first-repeat.png`

Captures F2 :

- obtenues partiellement, mais non utiles : elles capturent `Loading terrain` ou
  n'apparaissent pas selon le run automatise.

Captures de fenetre Windows :

- rejetees : mauvaise fenetre capturee.

## Etat final

- `run/config/iris.properties` restaure sur
  `shaderPack=MakeUp-UltraFast-9.5c.zip` ;
- aucun flag global cible laisse actif ;
- aucun code modifie ;
- aucune compilation relancee, car cette phase ne modifie pas le code.

## Conclusion

La validation visuelle confirme que l'ordre `SubmitNodeCollector` seul ne
stabilise pas Complementary.

Constats :

- `default/default` : cadre visible, contenu framebuffer sombre/intermittent ;
- `mask_first_explicit` : pas mieux que le defaut ;
- `quad_first` : techniquement valide, mais instable/intermittent ;
- `no_mask_reference` : contenu clairement visible, occlusion degradee ;
- `no_depth` : contenu clairement visible, fallback manuel le plus robuste.

Conclusion technique :

Le probleme Complementary vient bien de l'interaction depth mask strict /
shaderpack deferred-postprocess. Les variantes d'ordre ne suffisent pas a rendre
le mode depth-masked stable.

## Recommandation Phase 10.3

Ne pas promouvoir `quad_first`.

Ne pas changer le defaut global.

Garder `no_depth` comme fallback manuel shaderpack-safe.

Phase 10.3 recommandee :

- micro-phase de clipping CPU limite ou de masque geometrique sans depth strict ;
- objectif : reduire les artefacts quand `no_depth` est utilise, sans revenir au
  depth mask strict qui declenche l'intermittence sous Complementary.
