# PHASE 10.1 - Micro-audit ordre/profondeur du quad framebuffer

Date : 2026-06-30

## Objectif

Auditer le chemin minimal framebuffer autour de l'ordre SubmitNodeCollector et du
masque profondeur, uniquement avec des variantes opt-in.

Contraintes respectees :

- mode global par defaut conserve : `default` -> `depth-masked-equal` ;
- `no_depth` reste manuel ;
- `lequal` reste disponible mais non promu ;
- `always` reste diagnostic ;
- aucun ancien stencil reactive ;
- aucun shader clipping global reactive ;
- aucun mixin shader Sodium reactive ;
- aucun renderer Iris legacy restaure ;
- DimLib et AlternateDimensions restent isoles.

Baseline importante :

- `404c5000 Stabilize Iris shaderpack fallback rendering baseline`
- `e7c0f87d Document advanced portal rendering and clipping audit`

## Etat initial

Verification :

- `run/config/iris.properties` etait sur
  `shaderPack=MakeUp-UltraFast-9.5c.zip`.
- Aucun flag global `IMM_PTL_*` cible n'etait actif.
- Le worktree contient toujours des anciens logs/artefacts non suivis et des
  suppressions historiques, non lies a cette phase.

## Fichiers modifies

- `src/main/java/qouteall/imm_ptl/core/render/renderer/RendererUsingFrameBuffer.java`
- `MIGRATION_PLAN_26.1.md`
- `PHASE10.1_FRAMEBUFFER_ORDER_DEPTH_MICRO_AUDIT.md`
- `git-diff-phase10.1.txt`

## Correctif minimal ajoute

Ajout d'un flag dev opt-in :

`IMM_PTL_FRAMEBUFFER_ORDER_MODE=<mode>`

Modes :

- `default` :
  - comportement historique ;
  - depth mask en `SubmitNodeCollector.order(0)` ;
  - quad framebuffer en `SubmitNodeCollector.order(1)`.
- `mask_first_explicit` :
  - meme ordre que `default`, mais force explicitement par flag ;
  - sert de temoin opt-in.
- `quad_first` :
  - quad framebuffer en `order(0)` ;
  - depth mask en `order(1)`.
- `no_mask_reference` :
  - garde le depth mode demande, mais desactive le masque profondeur ;
  - reference proche de `no_depth`, sans changer `IMM_PTL_FRAMEBUFFER_DEPTH_MODE`.

Comportement par defaut :

- inchange si `IMM_PTL_FRAMEBUFFER_ORDER_MODE` est absent ;
- valeur invalide : warning unique + fallback vers `default`.

Instrumentation non spammy ajoutee :

- mode profondeur ;
- mode ordre ;
- shaderpack Iris detecte via `run/config/iris.properties` ;
- texture framebuffer disponible ;
- taille framebuffer ;
- pipeline choisi ;
- depth mask tente/applique ou fallback ;
- quad SubmitNodeCollector soumis.

## Validation compilation

Commandes lancees :

- `.\gradlew.bat compileJava processResources --rerun-tasks --console=plain`
- `.\gradlew.bat compileJava processResources --rerun-tasks --console=plain -Penable_sodium_compat=true`
- `.\gradlew.bat compileJava processResources --rerun-tasks --console=plain -Penable_iris_compat=true`

Resultats :

- vanilla : BUILD SUCCESSFUL ;
- Sodium compile-only : BUILD SUCCESSFUL ;
- Iris compile-only : BUILD SUCCESSFUL.

Logs :

- `compile-phase10.1-vanilla.txt`
- `compile-phase10.1-sodium.txt`
- `compile-phase10.1-iris.txt`

## Tests runtime

Les mondes Phase 10.1 ont ete crees comme copies de `Phase50Test`, car les noms
de mondes cibles n'existaient pas encore.

Les runs ont ete lances avec Iris runtime, auto creation du portail visible, puis
la fenetre Minecraft a ete fermee proprement apres detection du marqueur :

`Minimal recursive portal textured quad submitted via SubmitNodeCollector`

### A - MakeUp default reference

Log : `runclient-phase10.1-makeup-default-reference.txt`

- shaderpack : `MakeUp-UltraFast-9.5c.zip` ;
- depth mode : `default` ;
- order mode : `default` ;
- pipeline : `depth-masked-equal` ;
- framebuffer : `854x480` ;
- depth mask tente : oui ;
- depth mask applique : oui ;
- quad SubmitNodeCollector soumis : oui ;
- portail present cote client sous Sodium/Iris : oui ;
- build/run : BUILD SUCCESSFUL.

### B - Complementary default reference

Log : `runclient-phase10.1-complementary-default-reference.txt`

- shaderpack : `ComplementaryReimagined_r5.8.1.zip` ;
- depth mode : `default` ;
- order mode : `default` ;
- pipeline : `depth-masked-equal` ;
- framebuffer : `854x480` ;
- depth mask tente : oui ;
- depth mask applique : oui ;
- quad SubmitNodeCollector soumis : oui ;
- build/run : BUILD SUCCESSFUL.

### C - Complementary lequal reference

Log : `runclient-phase10.1-complementary-lequal-reference.txt`

- shaderpack : `ComplementaryReimagined_r5.8.1.zip` ;
- depth mode : `lequal` ;
- order mode : `default` ;
- pipeline : `depth-masked-lequal` ;
- framebuffer : `854x480` ;
- depth mask tente : oui ;
- depth mask applique : oui ;
- quad SubmitNodeCollector soumis : oui ;
- build/run : BUILD SUCCESSFUL.

### D - Complementary no_depth reference

Log : `runclient-phase10.1-complementary-no-depth-reference.txt`

- shaderpack : `ComplementaryReimagined_r5.8.1.zip` ;
- depth mode : `no_depth` ;
- order mode : `default` ;
- pipeline : `non-depth-masked` ;
- framebuffer : `854x480` ;
- depth mask : desactive intentionnellement ;
- quad SubmitNodeCollector soumis : oui ;
- portail present cote client sous Sodium/Iris : oui ;
- build/run : BUILD SUCCESSFUL.

### Variante - Complementary default + mask_first_explicit

Log : `runclient-phase10.1-complementary-default-mask-first-explicit.txt`

- depth mode : `default` ;
- order mode : `mask_first_explicit` ;
- pipeline : `depth-masked-equal` ;
- depth mask applique : oui ;
- quad SubmitNodeCollector soumis : oui ;
- build/run : BUILD SUCCESSFUL.

Interpretation :

Cette variante est techniquement equivalente au defaut. Elle valide que le flag
opt-in ne modifie pas la baseline quand il force explicitement l'ordre existant.

### Variante - Complementary lequal + mask_first_explicit

Log : `runclient-phase10.1-complementary-lequal-mask-first-explicit.txt`

- depth mode : `lequal` ;
- order mode : `mask_first_explicit` ;
- pipeline : `depth-masked-lequal` ;
- depth mask applique : oui ;
- quad SubmitNodeCollector soumis : oui ;
- portail present cote client sous Sodium/Iris : oui ;
- build/run : BUILD SUCCESSFUL.

Interpretation :

Equivalent a `lequal` reference sur l'ordre logique. Aucun gain technique
attendu hors verification explicite.

### Variante - Complementary default + quad_first

Log : `runclient-phase10.1-complementary-default-quad-first.txt`

- depth mode : `default` ;
- order mode : `quad_first` ;
- pipeline : `depth-masked-equal` ;
- framebuffer : `854x480` ;
- depth mask tente : oui ;
- depth mask applique : oui, mais apres le quad ;
- quad SubmitNodeCollector soumis : oui ;
- portail present cote client sous Sodium/Iris : oui ;
- build/run : BUILD SUCCESSFUL.

Interpretation :

La variante est techniquement executable. Elle n'est pas candidate a promotion
automatique : si le quad passe avant le depth mask, le masque ne peut pas servir
de garde fiable pour le quad de la meme passe. Elle reste utile comme diagnostic
d'ordre.

### Variante - Complementary default + no_mask_reference

Log : `runclient-phase10.1-complementary-default-no-mask-reference.txt`

- depth mode : `default` ;
- order mode : `no_mask_reference` ;
- pipeline : `non-depth-masked` ;
- framebuffer : `854x480` ;
- depth mask : fallback/desactive par ordre opt-in ;
- quad SubmitNodeCollector soumis : oui ;
- build/run : BUILD SUCCESSFUL.

Interpretation :

Cette variante confirme que l'ordre opt-in peut isoler le quad sans masque meme
avec `IMM_PTL_FRAMEBUFFER_DEPTH_MODE=default`. Elle sert de reference de
diagnostic, mais ne remplace pas `IMM_PTL_FRAMEBUFFER_DEPTH_MODE=no_depth`.

## Erreurs recherchees

Sur tous les logs Phase 10.1 :

- `Missing program` : 0 ;
- `Buffer already closed` : 0 ;
- `ConcurrentModificationException` : 0 ;
- `Duplicate entity UUID` : 0 ;
- `UnsupportedOperationException` : 0 ;
- crash report : 0 ;
- `BUILD FAILED` : 0.

## Limites du test

La Phase 10.1 a valide le chemin technique et l'ordre SubmitNodeCollector dans
les logs. Elle n'a pas produit de preuve visuelle manuelle nouvelle.

Donc :

- le portail est bien soumis ;
- le framebuffer est bien disponible ;
- les variantes opt-in sont executables ;
- aucune variante ne peut etre declaree visuellement meilleure sous
  Complementary sans nouvelle inspection manuelle/video.

## Etat final

- `run/config/iris.properties` restaure sur
  `shaderPack=MakeUp-UltraFast-9.5c.zip` ;
- aucun flag global cible laisse actif ;
- defaut global inchange ;
- `no_depth` non promu automatiquement ;
- renderer Iris legacy non restaure ;
- stencil/shader clipping non reactive.

## Conclusion pour Phase 10.2

Phase 10.1 montre que les variantes d'ordre/profondeur sont techniquement sures
et reversibles, mais ne prouvent pas une amelioration visuelle de Complementary.

Recommandation :

1. Ne pas changer le mode par defaut.
2. Garder `no_depth` comme fallback manuel shaderpack-safe.
3. Garder `IMM_PTL_FRAMEBUFFER_ORDER_MODE` comme outil diagnostic dev.
4. Pour Phase 10.2, choisir l'une des deux routes :
   - validation visuelle/video ciblee de `lequal`, `quad_first` et
     `no_mask_reference` sous Complementary ;
   - ou micro-phase de clipping CPU limite, car l'ordre SubmitNodeCollector seul
     ne resout probablement pas les passes deferred/post-process du shaderpack.
