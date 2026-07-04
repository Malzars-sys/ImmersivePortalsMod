# PHASE 10.3 - Micro-prototype de clipping CPU limite pour fallback no_depth

Date : 2026-06-30

## Objectif

Tester un prototype opt-in de clipping CPU/geometrique limite pour le chemin
minimal framebuffer sans depth mask, afin de voir s'il peut reduire les artefacts
du fallback `no_depth` sous Complementary.

## Contraintes respectees

- comportement par defaut inchange ;
- `no_depth` non promu automatiquement ;
- prototype non promu automatiquement ;
- aucun stencil reactive ;
- aucun appel `glStencil*` ajoute ;
- aucun shader clipping global ;
- aucun `MixinRenderSystem_Clipping` ;
- aucun mixin shader Sodium ;
- aucun renderer Iris legacy restaure ;
- DimLib et AlternateDimensions restent isoles.

## Etat initial

Baselines presentes :

- `404c5000 Stabilize Iris shaderpack fallback rendering baseline`
- `2fddda41 Add opt-in framebuffer order diagnostics`

Configuration initiale :

- `run/config/iris.properties` sur `shaderPack=MakeUp-UltraFast-9.5c.zip`
- aucun flag global cible actif.

## Fichiers modifies

- `src/main/java/qouteall/imm_ptl/core/render/renderer/RendererUsingFrameBuffer.java`
- `MIGRATION_PLAN_26.1.md`
- `PHASE10.3_NO_DEPTH_CPU_GEOMETRIC_CLIP_PROTOTYPE.md`
- `git-diff-phase10.3.txt`

## Prototype ajoute

Nouveau flag dev opt-in :

`IMM_PTL_PORTAL_CPU_CLIP_MODE=<mode>`

Modes :

- `off` :
  - defaut ;
  - aucun changement.
- `portal_quad_only` :
  - actif seulement quand le rendu framebuffer est non-depth-masked ;
  - valide les bounds CPU du portail ;
  - refuse le quad si width/height/axes sont invalides ;
  - documente que le quad soumis est strictement la geometrie du portail.
- `conservative_plane` :
  - ajoute un rejet experimental si la camera est clairement du mauvais cote du
    plan ;
  - non teste en Phase 10.3.
- `debug_bounds` :
  - loggue les bounds sans appliquer de rejet.

Alias historique/dev :

- `IMM_PTL_NO_DEPTH_GEOMETRIC_CLIP=true` active `portal_quad_only`.

Important :

Le chemin existant `MyRenderHelper.submitPortalAreaWithFramebuffer(...)` soumet
deja le quad exactement sur `axisW/axisH` et `width/height` du portail. Le
prototype Phase 10.3 ne peut donc pas clipper le contenu interne de la texture
destination. Il sert a verifier les bounds et a eviter des quads invalides, pas
a remplacer un vrai clipping scene.

## Logs ajoutes

Logs non spammy :

- mode CPU/geometric clip ;
- source du mode ;
- bounds valides ;
- width/height ;
- distance camera-plan ;
- mode applique ;
- fallback si bounds invalides.

Un premier run a revele un log applique trop bavard. Il a ete corrige avant la
validation finale : `Minimal portal CPU/geometric clip applied` apparait une
seule fois dans le run de controle.

## Validation compilation

Commandes :

- `.\gradlew.bat compileJava processResources --rerun-tasks --console=plain`
- `.\gradlew.bat compileJava processResources --rerun-tasks --console=plain -Penable_sodium_compat=true`
- `.\gradlew.bat compileJava processResources --rerun-tasks --console=plain -Penable_iris_compat=true`

Resultats :

- vanilla : BUILD SUCCESSFUL ;
- Sodium compile-only : BUILD SUCCESSFUL ;
- Iris compile-only : BUILD SUCCESSFUL.

Logs :

- `compile-phase10.3-vanilla.txt`
- `compile-phase10.3-sodium.txt`
- `compile-phase10.3-iris.txt`

## Tests runtime

Les mondes Phase 10.3 ont ete crees comme copies de `Phase50Test`.

### A - MakeUp default baseline

Monde : `Phase103MakeUpDefaultBaseline`

Configuration :

- shaderpack : `MakeUp-UltraFast-9.5c.zip`
- `IMM_PTL_FRAMEBUFFER_DEPTH_MODE` absent
- `IMM_PTL_PORTAL_CPU_CLIP_MODE` absent

Log : `runclient-phase10.3-makeup-default-baseline.txt`

Capture : `run/screenshots/phase10.3-makeup-default-baseline.png`

Resultat :

- depth mode : `default` ;
- order mode : `default` ;
- CPU/geometric clip : `off` ;
- pipeline : `depth-masked-equal` ;
- framebuffer : `854x480` ;
- quad soumis : oui ;
- screenshot native obtenue ;
- BUILD SUCCESSFUL.

### B - Complementary no_depth reference

Monde : `Phase103ComplementaryNoDepthReference`

Configuration :

- shaderpack : `ComplementaryReimagined_r5.8.1.zip`
- `IMM_PTL_FRAMEBUFFER_DEPTH_MODE=no_depth`
- `IMM_PTL_PORTAL_CPU_CLIP_MODE` absent

Log : `runclient-phase10.3-complementary-no-depth-reference.txt`

Capture : `run/screenshots/phase10.3-complementary-no-depth-reference.png`

Resultat :

- CPU/geometric clip : `off` ;
- pipeline : `non-depth-masked` ;
- framebuffer : `854x480` ;
- quad soumis : oui ;
- screenshot native obtenue ;
- BUILD SUCCESSFUL.

### C - Complementary no_depth + portal_quad_only

Monde : `Phase103ComplementaryNoDepthCpuClip`

Configuration :

- shaderpack : `ComplementaryReimagined_r5.8.1.zip`
- `IMM_PTL_FRAMEBUFFER_DEPTH_MODE=no_depth`
- `IMM_PTL_PORTAL_CPU_CLIP_MODE=portal_quad_only`

Log : `runclient-phase10.3-complementary-no-depth-cpu-clip.txt`

Capture : `run/screenshots/phase10.3-complementary-no-depth-cpu-clip.png`

Resultat :

- CPU/geometric clip : `portal_quad_only` ;
- bounds : `valid=true` ;
- width : `2.0` ;
- height : `3.0` ;
- clipping applique : oui, strict portal quad geometry only ;
- pipeline : `non-depth-masked` ;
- framebuffer : `854x480` ;
- quad soumis : oui ;
- screenshot native obtenue ;
- BUILD SUCCESSFUL.

Observation :

Le prototype n'ameliore pas visiblement l'occlusion : le contenu framebuffer
reste soumis au meme quad portal. Cela confirme que les artefacts restants sont
dans le contenu deja rendu de la texture, pas dans une geometrie de quad trop
large.

### D - Complementary no_mask_reference + portal_quad_only

Monde : `Phase103ComplementaryNoMaskCpuClip`

Configuration :

- shaderpack : `ComplementaryReimagined_r5.8.1.zip`
- `IMM_PTL_FRAMEBUFFER_DEPTH_MODE=default`
- `IMM_PTL_FRAMEBUFFER_ORDER_MODE=no_mask_reference`
- `IMM_PTL_PORTAL_CPU_CLIP_MODE=portal_quad_only`

Log : `runclient-phase10.3-complementary-no-mask-cpu-clip.txt`

Capture : `run/screenshots/phase10.3-complementary-no-mask-cpu-clip.png`

Resultat :

- test considere optionnel ;
- deux relances automatiques ont ete tentees apres correction anti-spam ;
- le client ne s'est pas ferme automatiquement dans la fenetre de validation ;
- le log final est donc tronque au lancement Gradle et n'est pas utilise comme
  preuve runtime finale.

Observation :

Le cas reste pertinent comme diagnostic futur, mais la validation Phase 10.3
s'appuie sur le cas principal `no_depth + portal_quad_only`, qui couvre le
chemin non-depth-masked cible par cette phase et dispose d'un log complet.

## Erreurs recherchees

Sur les logs Phase 10.3 complets :

- `Missing program` : 0 ;
- `Buffer already closed` : 0 ;
- `ConcurrentModificationException` : 0 ;
- `Duplicate entity UUID` : 0 ;
- `UnsupportedOperationException` : 0 ;
- crash report : 0 ;
- `BUILD FAILED` : 0.

Note :

- `runclient-phase10.3-complementary-no-mask-cpu-clip.txt` est conserve comme
  trace de tentative, mais il n'est pas compte dans les preuves finales car il
  est tronque.

## Etat final

- `run/config/iris.properties` restaure sur
  `shaderPack=MakeUp-UltraFast-9.5c.zip` ;
- aucun flag global cible laisse actif ;
- defaut global inchange ;
- `no_depth` reste manuel ;
- prototype reste opt-in.

## Conclusion

Le micro-prototype CPU/geometrique est stable et utile comme garde de bounds,
mais il ne reduit pas les artefacts visuels du fallback `no_depth`.

Cause :

Le quad framebuffer etait deja strictement limite a la geometrie du portail. Les
artefacts restants viennent du contenu deja rendu dans la texture destination, ce
qui demande un vrai clipping scene, un masque stencil/render-graph, ou une
strategie shader plus avancee.

## Recommandation Phase 10.4

Ne pas promouvoir `IMM_PTL_PORTAL_CPU_CLIP_MODE=portal_quad_only`.

Garder le mode comme outil diagnostic/fallback de securite pour bounds invalides.

Prochaine phase recommandee :

- audit d'un masque geometrique ou render-graph plus avance sans `glStencil*` ;
- ou gel de la baseline shaderpack fallback avant d'ouvrir une autre famille ;
- ne pas tenter davantage de corriger Complementary uniquement avec CPU clipping
  du quad, car le levier est epuise.
