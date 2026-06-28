# Phase 8.0C - Iris shaderpack world smoke test

Date : 28 juin 2026

## Objectif

Tester Iris runtime avec un shaderpack local dans un monde simple, sans portail.

## Shaderpack teste

- fichier : `MakeUp-UltraFast-9.5c.zip`
- chemin :
  `run/shaderpacks/MakeUp-UltraFast-9.5c.zip`
- taille : 398840 octets, environ 389 KiB
- nombre de shaderpacks `.zip` dans `run/shaderpacks` : 1

## Activation Iris

Fichier modifie :

```text
run/config/iris.properties
```

Modification controlee :

```properties
shaderPack=MakeUp-UltraFast-9.5c.zip
```

Aucune autre option graphique n'a ete modifiee volontairement.

## Validations compilation

- Vanilla :
  `.\gradlew.bat compileJava processResources --rerun-tasks --console=plain`
  - resultat : BUILD SUCCESSFUL
  - log : `compile-phase8.0C-vanilla.txt`

- Sodium compile-only :
  `.\gradlew.bat compileJava processResources --rerun-tasks --console=plain -Penable_sodium_compat=true`
  - resultat : BUILD SUCCESSFUL
  - log : `compile-phase8.0C-sodium.txt`

- Iris compile-only :
  `.\gradlew.bat compileJava processResources --rerun-tasks --console=plain -Penable_iris_compat=true`
  - resultat : BUILD SUCCESSFUL
  - log : `compile-phase8.0C-iris.txt`

## Monde temoin

Monde utilise :

```text
Phase80IrisShaderpackWorldTest
```

Le monde a ete prepare a partir de `Phase72IrisNoShaderTest`, pas de
`Phase50Test`.

Important : la premiere execution a montre que la copie initiale contenait
encore une entite portail. Pour respecter le perimetre "sans portail", les
dossiers `entities` ont ete supprimes uniquement dans la copie
`Phase80IrisShaderpackWorldTest`, puis le test final a ete relance.

## Runtime Iris avec shaderpack

Commande :

```powershell
.\gradlew.bat runClient --console=plain -Penable_iris_compat=true -Penable_iris=true --args=--quickPlaySingleplayer=Phase80IrisShaderpackWorldTest
```

Resultat final :

- runClient : BUILD SUCCESSFUL
- Iris charge : oui, `iris 1.10.8+mc26.1`
- Sodium runtime Iris : oui, `sodium 0.8.7+mc26.1`
- fallback FRAPI Iris/Sodium 0.8.7 enregistre : oui
- shaderpack selectionne : oui
- log shaderpack : `Using shaderpack: MakeUp-UltraFast-9.5c.zip`
- pipeline Iris cree pour `minecraft:overworld` : oui
- monde charge : oui
- joueur connecte : oui
- stabilite apres connexion : environ 23 secondes
- fermeture normale : oui
- crash : 0
- erreur mixin bloquante : 0
- `UnsupportedOperationException` : 0
- `Duplicate entity UUID` : 0
- `ConcurrentModificationException` : 0
- `Buffer already closed` : 0
- `PortalEntityRenderer` dans le run final : 0
- portail cree : non
- traversee testee : non

Logs :

- `runclient-phase8.0C-iris-shaderpack-world.txt`
- `runclient-phase8.0C-iris-shaderpack-world-stderr.txt`

## Notes non bloquantes

- Le shaderpack emet des avertissements Iris sur certaines entrees de block ID
  `betterendforge::*`. Ces avertissements viennent du shaderpack et n'ont pas
  empeche le chargement du monde.
- `IPModInfoChecking` signale un 404 reseau pour l'info mod, deja non bloquant.
- Realms signale une erreur d'autorisation FabricMC, non bloquante pour le test
  local.

## Variables dev portail

Les variables suivantes ont ete retirees avant le lancement :

- `IMM_PTL_AUTO_VISIBLE_TEST_PORTAL`
- `IMM_PTL_AUTO_MINIMAL_TRAVERSAL_TEST`
- `IMM_PTL_CAPTURE_MINIMAL_RECURSIVE_PORTAL`
- `IMM_PTL_MINIMAL_RECURSIVE_PORTAL_SCREENSHOT`

## Conclusion

Critere Phase 8.0C atteint : Iris charge un monde simple avec le shaderpack
local actif, via Sodium runtime Iris 0.8.7, sans portail et sans crash.

Suite logique : Phase 8.1, test portail avec shaderpack, a ouvrir separement.
