# Phase 11.7 - Regression sauvegarde / rechargement des portails interdimensionnels

## Objectif

Valider que les portails minimaux interdimensionnels crees par le harnais Phase 11.5/11.6 sont sauvegardes, recharges, resynchronises cote client et restent traversables apres relance du monde.

Directions testees :

- Overworld -> Nether
- Nether -> Overworld
- Overworld -> End
- End -> Overworld

Cette phase ne modifie pas le renderer, les pipelines, les shaders, Sodium, Iris, DimLib, AlternateDimensions ou les fallbacks shaderpack.

## Etat initial verifie

- Commit Phase 10.8 present : `5c4b7dcf Freeze Phase 10 shaderpack rendering baseline`
- Commit Phase 11.6 present : `6b26453c Stabilize dimension test harness for End traversal`
- `run/config/iris.properties` : `shaderPack=MakeUp-UltraFast-9.5c.zip`
- Aucun flag global `IMM_PTL_*` actif avant les tests
- Aucun process `runClient` Phase 11.x actif avant les tests

## Methode

Pour chaque direction :

1. Lancer un monde propre ou dedie avec :
   - `IMM_PTL_AUTO_DIMENSION_TEST_SOURCE=<source>`
   - `IMM_PTL_AUTO_DIMENSION_TEST_PORTAL=<destination>`
   - `IMM_PTL_AUTO_MINIMAL_TRAVERSAL_TEST=true`
2. Verifier creation du portail et premiere traversee.
3. Ajouter un datapack local de test dans le monde pour replacer le joueur dans la dimension source au rechargement.
4. Relancer le meme monde sans flag de creation de portail.
5. Garder seulement :
   - `IMM_PTL_AUTO_MINIMAL_TRAVERSAL_TEST=true`
6. Verifier que la traversee utilise le portail deja sauvegarde/recharge.

Le datapack de reload ne cree aucun portail. Il sert uniquement a repositionner le joueur pres du portail source existant.

## Pieges de harnais corriges pendant le test

Deux faux negatifs ont ete identifies dans les artefacts de test `run/saves`, sans modification du code runtime :

- trois fichiers `.mcfunction` avaient ete ecrits sur une seule ligne ; Minecraft executait tout comme un seul `say`, donc le joueur n'etait pas repositionne ;
- la reecriture PowerShell initiale en `UTF8` ajoutait un BOM, refuse par le parser `.mcfunction` de Minecraft 26.1.

Correctif de harnais applique dans les mondes de test :

- fonctions reecrites en plusieurs lignes ;
- encodage UTF-8 sans BOM ;
- ajout d'un message de verification :
  - `Phase 11.7 reload source position verified in <dimension>`

## Resultats par direction

### Overworld -> Nether

- Creation portail : oui
- Premiere traversee : oui
- Reload sans recreation : oui
- Portail recharge cote client : oui
- Traversee apres reload : oui
- Log cle :
  - `Minimal traversal debug command selected portal Portal{5,... minecraft:overworld -> minecraft:the_nether ...}`
  - `Client Changed Dimension from minecraft:overworld to minecraft:the_nether`
  - `Client Teleported Statically`

### Nether -> Overworld

- Creation portail : oui
- Premiere traversee : oui
- Reload sans recreation : oui
- Reposition source verifie : oui
- Portail recharge cote client : oui
- Traversee apres reload : oui
- Log cle :
  - `Phase 11.7 reload source position verified in minecraft:the_nether`
  - `Minimal traversal debug command selected portal Portal{101,... minecraft:the_nether -> minecraft:overworld ...}`
  - `Client Changed Dimension from minecraft:the_nether to minecraft:overworld`
  - `Client Teleported Statically`

Observation importante :

- le log Nether -> Overworld contient deux occurrences de :
  - `ImmPtlClientChunkMap Error deserializing chunk packet minecraft:overworld`
  - `Client disconnected with reason: Network Protocol Error`
- cela se produit apres la traversee Nether -> Overworld, y compris apres reload.
- La traversee et le reload du portail sont donc valides, mais un bug reseau/chunk client reste a isoler dans une phase ciblee.

### Overworld -> End

- Creation portail : oui
- Premiere traversee : oui
- Reload sans recreation : oui
- Reposition source verifie : oui
- Portail recharge cote client : oui
- Traversee apres reload : oui
- Log cle :
  - `Phase 11.7 reload source position verified in minecraft:overworld`
  - `Minimal traversal debug command selected portal Portal{12,... minecraft:overworld -> minecraft:the_end ...}`
  - `Client Changed Dimension from minecraft:overworld to minecraft:the_end`
  - `Client Teleported Statically`

### End -> Overworld

- Creation portail : oui
- Premiere traversee : oui
- Reload sans recreation : oui
- Reposition source verifie : oui
- Portail recharge cote client : oui
- Traversee apres reload : oui
- Log cle :
  - `Phase 11.7 reload source position verified in minecraft:the_end`
  - `Minimal traversal debug command selected portal Portal{52,... minecraft:the_end -> minecraft:overworld ...}`
  - `Client Changed Dimension from minecraft:the_end to minecraft:overworld`
  - `Client Teleported Statically`

## Stabilite

Sur les validations finales :

- `Duplicate entity UUID` : 0
- `ConcurrentModificationException` : 0
- `Buffer already closed` : 0
- `Missing program` : 0
- `UnsupportedOperationException` : 0
- crash report : 0
- build failed : 0

Exception documentee :

- Nether -> Overworld produit une erreur de deserialisation de chunk et une deconnexion `Network Protocol Error` apres traversee.

## Logs produits

- `runclient-phase11.7-overworld-to-nether-reload.txt`
- `runclient-phase11.7-nether-to-overworld-reload.txt`
- `runclient-phase11.7-overworld-to-end-reload.txt`
- `runclient-phase11.7-end-to-overworld-reload.txt`
- fichiers `stderr` correspondants

Ces logs sont des artefacts runtime et ne doivent pas etre commit par defaut.

## Compilation

Aucun code source n'a ete modifie pendant cette phase. `compileJava` n'a donc pas ete relance pour Phase 11.7.

La derniere baseline compilee reste celle de Phase 11.6 :

- `compileJava processResources` : BUILD SUCCESSFUL

## Conclusion

La sauvegarde/rechargement des portails interdimensionnels minimaux est validee pour :

- Overworld -> Nether
- Nether -> Overworld
- Overworld -> End
- End -> Overworld

Le portail recharge est bien retrouve cote client et la traversee se declenche sans recreation du portail.

## Recommandation Phase 11.8

Ouvrir une phase ciblee sur le bug reseau/chunk observe apres Nether -> Overworld :

- classe impliquee : `ImmPtlClientChunkMap`
- symptome : `Error deserializing chunk packet minecraft:overworld`
- consequence : `Network Protocol Error`
- perimetre : synchronisation chunk interdimensionnelle apres retour Nether -> Overworld
- hors perimetre : renderer, shaderpack, Sodium, Iris, DimLib
