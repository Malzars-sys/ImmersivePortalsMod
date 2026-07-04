# Phase 11.3 - Bug cible traversée interdimensionnelle vanilla

## Objectif

Identifier pourquoi les tests Phase 11.2 ne produisaient pas `Client Teleported Statically`
sur les portails interdimensionnels vanilla, sans modifier le renderer, les pipelines, Iris,
Sodium, DimLib ou AlternateDimensions.

## Fichiers inspectés

- `ClientTeleportationManager.java`
- `ServerTeleportationManager.java`
- `ClientDebugCommand.java`
- `PortalDebugCommands.java`
- `IPModEntryClient.java`
- `PortalCollisionHandler.java`
- `CollisionHelper.java`
- `MixinAbstractClientPlayer.java`
- `MixinServerPlayer.java`
- `imm_ptl.mixins.json`

## Correctifs appliqués

- Ajout d'un flag dev client :
  - `IMM_PTL_AUTO_DIMENSION_TEST_PORTAL=<dimension>`
  - permet d'envoyer `/imm_ptl_debug create_dimension_test_portal <dimension>` depuis le client,
    avec une vraie source joueur.
- Stabilisation de `/imm_ptl_client_debug test_minimal_portal_traversal` :
  - logs du portail choisi ;
  - logs des dimensions source/destination ;
  - logs des positions monde/locales avant mouvement ;
  - reset client sur la position de départ avant l'impulsion de test.
- Instrumentation limitée de `ClientTeleportationManager` :
  - logs des positions `last/current eye` ;
  - logs du `localZ` devant/derrière le portail ;
  - compteur limité pour éviter le spam.
- Portails dev `imm_ptl:minimal_test_portal` :
  - `crossPortalCollisionEnabled=false` pour isoler la téléportation de test du blocage
    "destination chunk not loaded".
- Transition client interdimensionnelle :
  - `LocalPlayer.clientLevel` n'existe plus comme champ accessible en 26.1 ;
  - le code continue maintenant avec `IEEntity.ip_setWorld(toWorld)` et `client.level = toWorld`
    si l'ancien champ est absent.
- Transition serveur :
  - fallback vanilla si `IEServerPlayerEntity` n'est pas appliqué à `ServerPlayer` ;
  - `stopRiding` / `startRiding` utilisent les méthodes vanilla ;
  - `portal_worldChanged` est ignoré avec warning clair si la façade est absente.

## Diagnostic

Les échecs Phase 11.2 mélangeaient trois causes :

1. La commande de création lancée depuis datapack n'avait pas de source joueur.
2. Le test client partait trop tôt ou après une chute du joueur, donc hors du rectangle du portail.
3. En interdimensionnel, la collision cross-portal stoppait le mouvement si le chunk destination
   n'était pas encore chargé côté client.

Après correction du harness, le contrôle Overworld -> Overworld produit bien :

- `localZ 0.2500 -> -0.4500`
- `candidates: 1`
- `Client Teleported Statically`

Le test Overworld -> Nether produit maintenant :

- `Client World Created minecraft:the_nether`
- portail choisi côté client ;
- `Client Changed Dimension from minecraft:overworld to minecraft:the_nether`
- `Client Teleported Statically`

## Limites restantes

- Le hook serveur `portal_worldChanged` reste en fallback parce que `IEServerPlayerEntity`
  n'est pas appliqué au `ServerPlayer` runtime actuel.
- Ce fallback évite le crash et valide la traversée, mais les triggers/advancements exacts
  liés à `ServerPlayer#changeDimension` devront être audités dans une phase dédiée.
- La collision cross-portal du portail minimal dev est désactivée pour le test. Le vrai
  chargement de chunks à travers portail reste un sujet séparé.

## Validations

- `compileJava processResources` : BUILD SUCCESSFUL.
- Contrôle Overworld -> Overworld :
  - portail créé : oui ;
  - portail trouvé : oui ;
  - `Client Teleported Statically` : oui ;
  - crash : non.
- Overworld -> Nether :
  - portail créé : oui ;
  - monde client Nether créé : oui ;
  - `Client Changed Dimension ... to minecraft:the_nether` : oui ;
  - `Client Teleported Statically` : oui ;
  - crash client final : non ;
  - erreur paquet serveur : non après fallback.

## Logs

- `compile-phase11.3-26.1.txt`
- `runclient-phase11.3-overworld-control.txt`
- `runclient-phase11.3-overworld-to-nether.txt`

## Conclusion

La traversée interdimensionnelle minimale vanilla est débloquée pour Overworld -> Nether.
Le bug bloquant était une combinaison de harness trop fragile, collision cross-portal sans
chunk destination client, puis façades client/serveur 26.1 absentes. Le rendu shaderpack,
Sodium, Iris, DimLib et AlternateDimensions n'ont pas été modifiés.

## Recommandation Phase 11.4

Faire une phase ciblée `ServerPlayer` / triggers dimension :

- restaurer proprement le hook équivalent à `portal_worldChanged` ;
- vérifier `enteredNetherPosition` et advancement triggers ;
- revalider Nether -> Overworld ;
- tester End seulement après confirmation serveur complète.
