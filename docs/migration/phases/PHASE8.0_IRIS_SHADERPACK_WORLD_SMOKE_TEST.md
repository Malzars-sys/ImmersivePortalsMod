# Phase 8.0 - Iris shaderpack world smoke test

Date : 28 juin 2026

## Objectif

Premier test Iris avec shaderpack, sans portail, en conservant les baselines
vanilla, Sodium non-shader et Iris sans shaderpack.

## Baselines verifiees

`git log --oneline -5` confirme :

- `bd3a080c` - `Stabilize Iris no-shaderpack portal rendering baseline`
- `545169c1` - `Stabilize Sodium non-shader portal rendering baseline`
- `5545115e` - `Stabilize vanilla minimal portal renderer baseline`

## Etat Git initial

Le worktree contenait encore les anciens logs et artefacts non suivis des phases
precedentes, ainsi que les suppressions historiques de logs Phase 4. Aucun de
ces fichiers n'a ete stage ni restaure pendant cette phase.

## Validations compilation

- Vanilla :
  `.\gradlew.bat compileJava processResources --rerun-tasks --console=plain`
  - resultat : BUILD SUCCESSFUL
  - log : `compile-phase8.0-vanilla.txt`

- Sodium compile-only :
  `.\gradlew.bat compileJava processResources --rerun-tasks --console=plain -Penable_sodium_compat=true`
  - resultat : BUILD SUCCESSFUL
  - log : `compile-phase8.0-sodium.txt`
  - note : une premiere tentative parallele avait echoue sur un verrouillage de
    `build/classes/java/main`; la validation finale a ete relancee seule et a
    reussi.

- Iris compile-only :
  `.\gradlew.bat compileJava processResources --rerun-tasks --console=plain -Penable_iris_compat=true`
  - resultat : BUILD SUCCESSFUL
  - log : `compile-phase8.0-iris.txt`

## Runtime Iris sans shaderpack

Commande :

```powershell
.\gradlew.bat runClient --console=plain -Penable_iris_compat=true -Penable_iris=true --args=--quickPlaySingleplayer=Phase72IrisNoShaderTest
```

Resultat :

- runClient : BUILD SUCCESSFUL
- monde charge : oui
- joueur connecte : oui
- fermeture normale : oui
- Iris charge : oui, `iris 1.10.8+mc26.1`
- Sodium charge via Iris : oui, `sodium 0.8.7+mc26.1`
- shaderpack charge : non
- log Iris attendu : `Shaders are disabled because no valid shaderpack is selected`
- fallback FRAPI Iris/Sodium 0.8.7 enregistre : oui
- crash : non
- erreur mixin bloquante : non
- `Duplicate entity UUID` : 0
- `ConcurrentModificationException` : 0
- `Buffer already closed` : 0

Log : `runclient-phase8.0-iris-no-shaderpack-world.txt`

Notes non bloquantes :

- `IPModInfoChecking` signale un 404 reseau pour l'info mod, deja non bloquant.
- Realms signale une erreur d'autorisation FabricMC, non bloquante pour le test
  local.

## Verification shaderpacks

Inspection :

```powershell
Get-ChildItem run\shaderpacks -Force
```

Resultat : `COUNT=0`

Aucun shaderpack local n'est disponible dans `run/shaderpacks`.
Conformement a la consigne, aucun shaderpack n'a ete telecharge
automatiquement et le test runtime avec shaderpack n'a pas ete lance.

Le monde temoin `Phase80IrisShaderpackWorldTest` n'a pas ete utilise pour un
test shaderpack, car la phase doit s'arreter avant runtime shaderpack lorsqu'il
n'existe aucun pack local.

## Portails

Cette phase est un test shaderpack sans portail.

- aucun portail n'a ete cree ;
- aucun flag dev de portail n'a ete active ;
- les variables suivantes ont ete supprimees de l'environnement du lancement :
  - `IMM_PTL_AUTO_VISIBLE_TEST_PORTAL`
  - `IMM_PTL_AUTO_MINIMAL_TRAVERSAL_TEST`
  - `IMM_PTL_CAPTURE_MINIMAL_RECURSIVE_PORTAL`
  - `IMM_PTL_MINIMAL_RECURSIVE_PORTAL_SCREENSHOT`

## Conclusion

La partie prealable de la Phase 8.0 est validee :

- vanilla compile toujours ;
- Sodium compile-only compile toujours ;
- Iris compile-only compile toujours ;
- Iris runtime sans shaderpack atteint le monde et se ferme sans crash.

Blocage exact pour continuer la Phase 8.0 :

`run/shaderpacks` est vide. Il faut fournir un shaderpack local a tester avant
de lancer le smoke test Iris avec shaderpack.

## Livrables

- `PHASE8.0_IRIS_SHADERPACK_WORLD_SMOKE_TEST.md`
- `MIGRATION_PLAN_26.1.md`
- `git-diff-phase8.0.txt`
- `compile-phase8.0-vanilla.txt`
- `compile-phase8.0-sodium.txt`
- `compile-phase8.0-iris.txt`
- `runclient-phase8.0-iris-no-shaderpack-world.txt`
