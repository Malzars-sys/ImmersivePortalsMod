# Phase 4.8 - Audit des commandes

Date : 12 juin 2026

## Resultat

Les trois arbres de commandes sont correctement enregistres dans le profil
vanilla 26.1. Aucun filtre Gradle, entrypoint Fabric ou condition runtime ne
les exclut.

Les logs runtime confirment :

```text
(PortalCommand) Registering PortalCommand
(PortalDebugCommands) Registering PortalDebugCommands
(ClientDebugCommand) Registering ImmPtlClientDebugCommands
```

## Enregistrement

- `IPModMain.init()` enregistre `PortalCommand.register(...)` avec
  `CommandRegistrationCallback.EVENT`.
- `PortalCommand.register(...)` expose la racine serveur `/portal`.
- `PortalDebugCommands.registerDebugCommands(...)` est appele par
  `PortalCommand` et expose `/portal debug ...`, pas une racine separee.
- `IPModMainClient.init()` enregistre `ClientDebugCommand.register(...)` avec
  `ClientCommandRegistrationCallback.EVENT`.
- `ClientDebugCommand.register(...)` expose la racine client
  `/imm_ptl_client_debug`.

## Permissions et cause de l'absence apparente

La racine `/portal` utilise `requires(PortalCommand::canUsePortalCommand)`.
Elle est donc entierement masquee par Brigadier pour un joueur sans permission
de niveau 2.

Le monde `Phase43Test3` a charge le joueur de test en survie et sans permission
operateur. La configuration contient `easeCreativePermission=true`, mais cet
assouplissement ne s'applique qu'aux joueurs en mode creatif. Dans cet etat,
`/portal` et `/portal debug` apparaissent comme absents. C'est le comportement
attendu, pas un echec d'enregistrement.

La racine `/imm_ptl_client_debug` utilise `requires(commandSource -> true)`.
Elle est disponible sans permission serveur. Les phases 4.5 a 4.7 ont execute
avec succes `report_loaded_portals`, `report_player_status` et
`test_minimal_portal_traversal`.

## Commandes attendues

Pour un joueur creatif avec `easeCreativePermission=true`, ou un operateur :

- `/portal`
- `/portal animation`
- `/portal global`
- `/portal debug`
- `/portal euler`
- les nombreuses sous-commandes de modification et teleportation de portail

Pour tous les clients :

- `/imm_ptl_client_debug`
- `/imm_ptl_client_debug report_loaded_portals`
- `/imm_ptl_client_debug report_player_status`
- `/imm_ptl_client_debug test_minimal_portal_traversal`
- les autres diagnostics client enregistres par `ClientDebugCommand`

## Fichiers inspectes

- `src/main/java/qouteall/imm_ptl/core/IPModMain.java`
- `src/main/java/qouteall/imm_ptl/core/IPModMainClient.java`
- `src/main/java/qouteall/imm_ptl/core/commands/PortalCommand.java`
- `src/main/java/qouteall/imm_ptl/core/commands/PortalDebugCommands.java`
- `src/main/java/qouteall/imm_ptl/core/commands/ClientDebugCommand.java`
- `src/main/java/qouteall/imm_ptl/core/platform_specific/IPConfig.java`
- `src/main/resources/fabric.mod.json`
- `build/resources/main/fabric.mod.json`
- `build.gradle`
- `gradle.properties`
- `run/config/immersive_portals.json`

## Correctif minimal

Trois logs explicites ont ete ajoutes au debut de chaque construction d'arbre
de commandes. Aucune permission et aucun nom de commande n'ont ete modifies.

## Validation

- `compileJava` : BUILD SUCCESSFUL, 0 erreur
- `runClient` : monde solo atteint, aucun crash
- profil vanilla : commandes non exclues
- Sodium, Iris et DimLib : non touches
