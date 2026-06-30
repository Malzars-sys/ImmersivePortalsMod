# PHASE 10.5 - Micro-prototype masque alpha texture pour framebuffer no_depth

Date : 2026-06-30

## Objectif

Ajouter un prototype opt-in pour tester une composition alpha texture du
framebuffer destination sur le chemin `no_depth`, sans stencil OpenGL, sans
changer le defaut global et sans reactiver les systemes avances.

## Contraintes respectees

- comportement par defaut inchange ;
- `no_depth` non promu automatiquement ;
- `alpha_texture` non promu automatiquement ;
- aucun `glStencil*` ajoute ;
- `RendererUsingStencil` non reactive ;
- shader clipping global non active ;
- mixins shader Sodium non actives ;
- renderers Iris legacy non restaures ;
- DimLib et AlternateDimensions restent isoles.

## Etat initial verifie

Commits attendus presents :

- `404c5000 Stabilize Iris shaderpack fallback rendering baseline`
- `2fddda41 Add opt-in framebuffer order diagnostics`
- `e196758b Add opt-in no-depth portal geometry clip diagnostics`

Configuration :

- `run/config/iris.properties` restaure sur `MakeUp-UltraFast-9.5c.zip`
- aucun flag global `IMM_PTL_*` actif avant les tests.

## Fichiers modifies

- `src/main/java/qouteall/imm_ptl/core/render/pipeline/IPRenderPipelines.java`
- `src/main/java/qouteall/imm_ptl/core/render/renderer/RendererUsingFrameBuffer.java`
- `MIGRATION_PLAN_26.1.md`
- `PHASE10.5_ALPHA_TEXTURE_MASK_PROTOTYPE.md`
- `git-diff-phase10.5.txt`

## Prototype ajoute

Nouveau flag dev opt-in :

`IMM_PTL_FRAMEBUFFER_MASK_MODE=<mode>`

Modes :

- `off` :
  - defaut ;
  - comportement actuel conserve.
- `alpha_texture` :
  - tente une composition alpha-aware uniquement pour le chemin
    non-depth-masked ;
  - ignore le mode si le depth mask normal est actif ;
  - fallback vers le chemin framebuffer existant si le pipeline est
    indisponible.

Valeur invalide :

- warning unique ;
- fallback vers `off` ;
- aucun crash attendu.

## Implementation

`IPRenderPipelines` ajoute :

- slot `DRAW_FRAMEBUFFER_IN_AREA_ALPHA_TEXTURE` ;
- pipeline `pipeline/imm_ptl_draw_framebuffer_in_area_alpha_texture` ;
- shader vanilla `core/position_tex` ;
- sampler `Sampler0` ;
- `BlendFunction.TRANSLUCENT` ;
- depth test `ALWAYS_PASS` sans write depth ;
- mapping Iris reflectif vers `TEXTURED`.

`RendererUsingFrameBuffer` ajoute :

- resolution du flag `IMM_PTL_FRAMEBUFFER_MASK_MODE` ;
- logs uniques du mode ;
- tentative de pipeline alpha seulement quand `useDepthMask == false` ;
- fallback vers `getMinimalPortalFramebufferRenderType(...)` si indisponible.

## Limite du prototype

Ce prototype ne cree pas encore une texture masque separee. Il teste la partie
la plus petite et la moins risquee de la strategie : une passe de composition
alpha-aware sur le quad framebuffer non-depth-masked.

Raison :

- une vraie texture masque separee demanderait une cible de rendu additionnelle,
  une passe d'ecriture du masque et probablement un pipeline de composition
  multi-sampler ;
- ce chemin augmente fortement le risque Iris shaderpack ;
- Phase 10.5 garde donc un prototype minimal et reversible.

Conclusion technique preliminaire :

- si ce pipeline ne change pas le resultat visuel, la prochaine etape utile
  devra etre un vrai masque texture separe + composition, pas seulement un
  pipeline alpha sur le quad final.

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

- `compile-phase10.5-vanilla.txt`
- `compile-phase10.5-sodium.txt`
- `compile-phase10.5-iris.txt`

## Validation runtime

Mondes prepares :

- `Phase105MakeUpDefaultBaseline`
- `Phase105ComplementaryNoDepthReference`
- `Phase105ComplementaryNoDepthAlphaMask`
- `Phase105InvalidMaskMode`

Tentative effectuee :

- `Phase105MakeUpDefaultBaseline`
- shaderpack : `MakeUp-UltraFast-9.5c.zip`
- flags communs :
  - `IMM_PTL_AUTO_VISIBLE_TEST_PORTAL=true`
  - `IMM_PTL_AUTO_MINIMAL_TRAVERSAL_TEST=false`
  - `IMM_PTL_CAPTURE_MINIMAL_RECURSIVE_PORTAL=true`

Resultat :

- le client ne s'est pas ferme automatiquement dans la fenetre de validation ;
- le log `runclient-phase10.5-makeup-default-baseline.txt` est tronque au
  lancement Gradle ;
- aucun log Minecraft exploitable n'a ete produit avant interruption ;
- le processus `runClient` a ete arrete proprement ;
- `run/config/iris.properties` a ete restaure sur MakeUp.

Decision :

- les runs B/C/D n'ont pas ete lances ensuite, pour eviter de multiplier des
  logs tronques non exploitables ;
- la validation runtime Phase 10.5 reste donc non concluante dans cette session.

## Etat final

- `run/config/iris.properties` : `shaderPack=MakeUp-UltraFast-9.5c.zip`
- aucun flag global `IMM_PTL_*` laisse actif ;
- aucun process `runClient` Phase 10.5 restant.

## Interpretation

Le prototype compile dans les trois profils et reste strictement opt-in.

Il ne prouve pas encore une amelioration visuelle sous Complementary, car le
runtime automatique n'a pas fourni de run exploitable. Le mode `alpha_texture`
doit donc rester experimental et non promu.

## Recommandation Phase 10.6

Deux options raisonnables :

1. Refaire une validation runtime manuelle/interactive du mode
   `IMM_PTL_FRAMEBUFFER_MASK_MODE=alpha_texture` sous Complementary `no_depth`.
2. Si aucun gain visuel n'est observe, abandonner ce micro-mode et concevoir le
   vrai prototype suivant :
   - texture masque separee ;
   - passe de composition dediee ;
   - fallback strict ;
   - mapping Iris explicite.

Ne pas changer le defaut global avant preuve visuelle claire.
