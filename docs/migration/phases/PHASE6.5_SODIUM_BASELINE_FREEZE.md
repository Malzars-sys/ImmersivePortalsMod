# Phase 6.5 - Gel de la baseline Sodium non-shader

Date : 21 juin 2026

## Baseline precedente

- commit vanilla : `5545115e50e2b98b46303d7cf319d5d96f41ba5b` ;
- message : `Stabilize vanilla minimal portal renderer baseline`.

Le commit Sodium prepare porte le message :
`Stabilize Sodium non-shader portal rendering baseline`.

## Profil Sodium fige

Sodium utilise `mc26.1.1-0.8.9-fabric`. Le profil reste desactive par defaut et
s'active explicitement avec `-Penable_sodium_compat=true`.

Les groupes A et B sont inchanges :

- A : `IESodiumWorldRenderer`, `MixinSodiumFlawlessFrames` ;
- B : groupe A, `MixinSodiumWorldRenderer`, `MixinSodiumViewport`.

Le Groupe C contient neuf mixins non-shader :

1. `IESodiumWorldRenderer` ;
2. `MixinSodiumFlawlessFrames` ;
3. `MixinSodiumWorldRenderer` ;
4. `MixinSodiumViewport` ;
5. `MixinSodiumPortalEntityRenderer` ;
6. `MixinSodiumPortalLevelRenderer` ;
7. `MixinSodiumOcclusionCuller` ;
8. `MixinSodiumRenderRegion` ;
9. `MixinSodiumRenderSectionManager`.

Toujours exclus :

- `MixinSodiumDefaultShaderInterface` ;
- `MixinSodiumShaderLoader` ;
- Iris, DimLib et AlternateDimensions dynamique ;
- mixins shader, fog et clipping avances.

## Audit des mixins portail

`MixinSodiumPortalEntityRenderer` ne force que les instances de `Portal` a
franchir le premier filtre de `EntityRenderer.shouldRender`.

`MixinSodiumPortalLevelRenderer` ne remplace la visibilite de section que pour
la variable locale qui est une instance de `Portal`. Les autres entites et le
culling terrain Sodium conservent leur comportement normal.

Ces mixins ne referencent ni Iris ni DimLib, ne dessinent rien directement et
ne lancent aucun rendu monde reentrant.

## Instrumentation conservee

Les logs sont limites a une occurrence par processus :

- presence client du portail pendant le test dev opt-in ;
- bypass entite et section Sodium ;
- appel de `PortalEntityRenderer.submit` ;
- collecte, framebuffer, masque et fallback du renderer minimal.

Le log redondant de `extractRenderState` a ete retire.

## Validations

### Vanilla

`compileJava processResources --rerun-tasks` : BUILD SUCCESSFUL.

Sodium runtime, Iris et DimLib ne sont pas ajoutes au profil par defaut. Le
renderer minimal vanilla reste le renderer fige en Phase 5.17.

### Sodium compile-only

`compileJava processResources --rerun-tasks -Penable_sodium_compat=true` :
BUILD SUCCESSFUL, sans erreur Sodium, Iris ou DimLib.

### Sodium Groupe C - monde

`Phase49Test` : joueur connecte, 30 secondes stables, fermeture normale et
BUILD SUCCESSFUL.

- crash : 0 ;
- `ConcurrentModificationException` : 0 ;
- `Buffer already closed` : 0 ;
- `Duplicate entity UUID` : 0 ;
- `AbstractMethodError` : 0.

`Phase50Test` a signale un ancien UUID de portail duplique apres les nombreuses
campagnes automatiques. Le probleme est propre a cette sauvegarde de travail ;
elle n'est pas utilisee comme temoin final et n'est pas modifiee dans ce commit.

### Sodium Groupe C - portail

`Phase49Test` avec creation/capture opt-in :

- portail present cote client : oui ;
- `PortalEntityRenderer.submit` : oui ;
- cadre cyan : visible ;
- framebuffer secondaire : atteint ;
- masque profondeur minimal : applique ;
- quad texture `SubmitNodeCollector` : soumis ;
- capture native : obtenue ;
- fermeture normale : BUILD SUCCESSFUL ;
- regressions runtime ciblees : 0.

La traversee Overworld vers Overworld a ete revalidee en Phase 6.4 apres le
correctif final de facade de tracking serveur.

Capture generee, volontairement hors commit :
`run/screenshots/phase6.5-sodium-baseline-portal.png`.

## Limites connues

- clipping general incomplet ;
- cadres imbriques visibles ;
- une seule recursion ;
- fog vanilla fallback ;
- aucun shader clipping ;
- pas d'Iris ;
- pas de DimLib.

## Fichiers inclus dans le commit

- configuration Gradle Sodium et proprietes de version ;
- adaptations API et mixins Sodium non-shader ;
- deux bypass cibles des portails ;
- garde optionnelle du tracking serveur ;
- instrumentation runtime limitee ;
- rapports `PHASE6.0` a `PHASE6.5` ;
- `MIGRATION_PLAN_26.1.md`.

## Fichiers volontairement exclus

- tous les `compile-phase*.txt`, `runclient-phase*.txt` et
  `git-diff-phase*.txt` ;
- captures PNG et sauvegardes de test ;
- copies de rapports ou du plan ;
- suppressions historiques de logs suivis par Git ;
- autres artefacts non suivis des phases precedentes.
