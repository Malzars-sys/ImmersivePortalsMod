# Phase 4.9 - Portail de test serveur minimal

Date : 12 juin 2026

## Commande ajoutee

`/imm_ptl_debug create_minimal_test_portal`

La racine `/imm_ptl_debug` est enregistree uniquement en environnement de
developpement. Elle ne depend pas des permissions de `/portal` et reste donc
utilisable par un joueur solo en survie sans permission operateur.

## Chemin de creation

`PortalCommand.register(...)` appelle
`PortalDebugCommands.registerDevelopmentCommands(...)`.

La commande place un portail vertical stable de 2 blocs par 3 blocs quatre
blocs devant le joueur. Sa destination est situee dix blocs plus loin dans la
meme dimension. Elle ajoute le tag `imm_ptl:minimal_test_portal`, puis appelle
`McHelper.spawnServerEntity(portal)`, qui ajoute et synchronise l'entite.

## Validation

Le monde propre `Phase49Test` a ete prepare sans anciennes donnees d'entites
ni de joueur.

- `compileJava` : BUILD SUCCESSFUL, 0 erreur
- `runClient` : BUILD SUCCESSFUL sur deux lancements, aucun crash
- commande disponible en survie solo : oui
- portail cree devant le joueur : oui
- portail present cote client : oui
- cadre cyan visible : oui
- traversee Overworld vers Overworld : oui
- `Client Teleported Statically` observe : oui
- `/imm_ptl_client_debug test_minimal_portal_traversal` : toujours fonctionnel
- sauvegarde et rechargement du portail : reussi

Capture : `run/screenshots/2026-06-12_19.25.27.png`

## Duplicate entity UUID

Les deux lancements de `Phase49Test`, y compris apres sauvegarde et
rechargement, ne produisent aucun avertissement `Duplicate entity UUID`.
Les avertissements de Phase 4.8 sont donc lies a l'ancien etat de
`Phase43Test3`. Aucun correctif supplementaire n'est applique dans cette phase.

## Fichiers modifies

- `src/main/java/qouteall/imm_ptl/core/commands/PortalCommand.java`
- `src/main/java/qouteall/imm_ptl/core/commands/PortalDebugCommands.java`
- `MIGRATION_PLAN_26.1.md`
- `PHASE4.9_MINIMAL_TEST_PORTAL.md`

Sodium, Iris, DimLib, AlternateDimensions et le rendu avance ne sont pas
reactives.
