# Plan de migration Minecraft 26.1

## Objectif

Obtenir d'abord une compilation Fabric vanilla sans Sodium ni Iris, puis descendre
sous 300 erreurs avant de réécrire le rendu avancé.

Baseline du 9 juin 2026 :

- `./gradlew compileJava` : échec
- compilation complète : 1 092 erreurs
- profil actuel : Sodium et Iris absents au runtime, mais leurs sources sont encore compilées

Baseline vanilla après la phase 0 :

- `vanilla_core_compile=true`
- compilation complète : 1 015 erreurs
- erreurs Sodium/Iris restantes : 0
- dépendances Sodium/Iris dans `compileClasspath` : 0
- ressources et mixins compat chargés dans le profil vanilla : 0

Les nombres par famille ci-dessous se chevauchent parfois. Ils servent à ordonner
le travail, pas à calculer le total exact.

## Règles de travail

1. Ne traiter qu'une famille d'API à la fois.
2. Après chaque famille, lancer une compilation complète avec `-Xmaxerrs 2000`.
3. Mettre à jour :
   - `compile-errors-26.1.txt`
   - `compile-errors-list-26.1.txt`
   - `compile-errors-raw.txt`
4. Noter le nombre avant/après et les nouvelles erreurs introduites.
5. Ne pas commencer `RenderPipeline` tant que le profil vanilla dépasse 300 erreurs.
6. Ne pas réactiver Sodium/Iris avant que le cœur vanilla compile.

## Phase 0 - Profil de compilation vanilla

Statut : terminé le 9 juin 2026.

But : faire en sorte que `compileJava` ne compile réellement aucune intégration
Sodium/Iris.

Travail :

- Ajouter une propriété Gradle explicite, par exemple `vanilla_core_compile=true`.
- Dans ce profil, ne pas ajouter Sodium/Iris en `compileOnly` ou `runtimeOnly`.
- Exclure des sources Java :
  - `core/compat/mixin/iris/**`
  - `core/compat/mixin/sodium/**`
  - les implémentations Iris/Sodium dépendant directement de leurs bibliothèques
  - `MixinShaderInstanceForIris`
- Ne pas charger `imm_ptl_compat.mixins.json` dans le profil vanilla, ou générer une
  configuration compat vide.
- Garder des façades vanilla/no-op pour `IrisInterface` et `SodiumInterface`, car le
  cœur les référence encore. Elles doivent retourner `false`, `null` ou ne rien faire.
- Vérifier qu'aucun import `net.irisshaders.*` ou `net.caffeinemc.*` n'est compilé.

Critères de sortie :

- compilation sans sources Iris/Sodium ;
- zéro erreur provenant de packages Iris/Sodium ;
- rapport de référence `compile-vanilla-baseline-26.1.txt`.

Estimation actuelle :

- 27 fichiers directement dans les dossiers compat Iris/Sodium ;
- 77 erreurs explicitement classées Iris/Sodium ;
- 43 fichiers référencent encore leurs façades ou intégrations.

## Phase 1 - Portal.java et sérialisation NBT

But : migrer le contrat de sauvegarde des entités avant les renommages généraux.

Progression du 9 juin 2026 :

- compilation complète après Phase 1 bis : 894 erreurs
- erreurs directes dans `Portal.java` : 0
- erreurs NBT directes dans `Portal.java` : 0
- erreurs dans les classes NBT ciblées de Phase 1 bis : 0
- erreurs NBT restantes hors `Portal.java` : 45
- les sous-classes de portail utilisent le nouveau crochet NBT commun

Ordre :

1. Migrer les méthodes d'entité vers `ValueInput` / `ValueOutput`.
2. Implémenter les nouvelles signatures de :
   - `readAdditionalSaveData`
   - `addAdditionalSaveData`
3. Adapter les lectures qui retournent `Optional<T>`.
4. Adapter `getList`, `CompoundTag`, `StringTag` et UUID.
5. Migrer les classes qui partagent le format NBT de portail :
   - `PortalExtension`
   - `PortalState`
   - animations de portail
   - stockage des portails globaux
6. Ajouter des tests ou vérifications de round-trip lecture/écriture si possible.

Critères de sortie :

- zéro erreur NBT dans `Portal.java` ;
- les signatures abstraites d'`Entity` sont satisfaites ;
- les anciennes données de portail restent lisibles ;
- compilation complète et rapports mis à jour.

Baseline indicative :

- environ 152 erreurs liées à `Portal.java` / NBT ;
- `Portal.java` contient actuellement environ 40 diagnostics directs.

## Phase 2 - Changements Minecraft généraux

But : réduire massivement les erreurs mécaniques sans toucher au rendu avancé.

Traiter dans cet ordre, avec une compilation entre chaque sous-famille :

### 2.1 Identifiants, positions et directions

Statut : termine le 9 juin 2026.

- compilation complete apres Phase 2.1 : 643 erreurs
- baisse depuis Phase 1 bis : 894 -> 643 (-251)
- zero diagnostic restant pour les signatures ciblees de Phase 2.1
- 4 diagnostics DimLib 1.21.1 reveles dans `AlternateDimensions` sont reportes
  a la phase 2.5 API peripheriques

- `ResourceKey.location()` vers `ResourceKey.identifier()`
- champs privés `ChunkPos.x/z` vers leurs accesseurs
- constructeurs `ChunkPos` supprimés
- `Direction.getNormal()` et `Direction.fromDelta(...)`
- `getMinSection()` / `getMaxSection()`

Baseline indicative :

- 102 erreurs contenant `location()`
- 59 erreurs liées à `ChunkPos`
- 59 erreurs liées aux directions/vecteurs

### 2.2 Serveur, progression et interaction

Statut : termine le 9 juin 2026.

- compilation complete apres Phase 2.2 : 484 erreurs
- baisse depuis Phase 2.1 : 643 -> 484 (-159)
- erreurs serveur / profiler ciblees restantes : 0
- erreurs interaction ciblees restantes : 0
- erreurs ecran / clavier ciblees restantes : 0
- nouvelles erreurs introduites : 0
- les erreurs DimLib d'`AlternateDimensions` restent reportees a la phase 2.5

- `Entity.getServer()`
- `Minecraft.getProfiler()`
- `InteractionResultHolder`
- `ChunkProgressListener` / `ChunkProgressListenerFactory`
- `ReceivingLevelScreen`
- signatures d'événements clavier/souris

Baseline indicative : environ 105 erreurs.

### 2.3 Entités, rendu simple et enregistrement

- nouvelles génériques `EntityRenderer<E, S>`
- création et enregistrement des `EntityType`
- états de rendu d'entité
- écrans et renderers simples ne dépendant pas des shaders de portail

### 2.4 Réseau et payloads

- payloads Fabric
- codecs réseau
- remplacement des anciennes créations/envois de paquets
- synchronisation client/serveur

Baseline indicative : environ 55 erreurs.

### 2.5 Worldgen et API périphériques

- `ChunkGenerator`
- carving
- listes pondérées
- biome et génération de dimensions
- API Fabric restantes

Critère de sortie global de la phase 2 :

- profil vanilla sous 300 erreurs ;
- aucune erreur NBT structurante ;
- aucune erreur mécanique évidente dans les familles ci-dessus.

## Phase 3 - Nouveau pipeline de rendu Minecraft 26.1

Cette phase commence uniquement lorsque le profil vanilla est sous 300 erreurs.

But : remplacer l'ancien rendu immédiat par les passes GPU et `RenderPipeline`.

Ordre :

1. Définir les pipelines et uniformes du cœur du mod.
2. Remplacer `ShaderInstance` et `Program`.
3. Remplacer `BufferUploader`.
4. Migrer `MyRenderHelper`.
5. Migrer `FrontClipping`.
6. Migrer `ViewAreaRenderer`.
7. Réactiver le rendu de portail minimal.
8. Réactiver les overlays et effets secondaires.

Ne pas mélanger cette phase avec Sodium/Iris.

Baseline indicative :

- environ 103 erreurs directement liées à l'ancien pipeline ;
- `MyRenderHelper.java` est actuellement le fichier le plus touché avec environ
  59 diagnostics.

Critères de sortie :

- compilation Fabric vanilla réussie ;
- lancement client vanilla réussi ;
- portail visible et traversable ;
- aucune dépendance Sodium/Iris.

## Phase 4 - Réactivation Sodium puis Iris

Ordre strict :

1. Réactiver Sodium seul.
2. Compiler et tester.
3. Migrer les mixins Sodium.
4. Réactiver Iris seulement après Sodium.
5. Migrer les transformations de shaders et les pipelines Iris.
6. Tester sans shaderpack, puis avec shaderpack.

Critères de sortie :

- compilation vanilla toujours valide ;
- compilation Sodium valide ;
- compilation Sodium + Iris valide ;
- chaque profil possède son propre rapport et son test de lancement.

## Jalons

| Jalon | Condition |
| --- | --- |
| J0 | Profil vanilla excluant réellement Iris/Sodium |
| J1 | `Portal.java` et NBT migrés |
| J2 | Moins de 500 erreurs |
| J3 | Moins de 300 erreurs |
| J4 | Pipeline de rendu vanilla compilé |
| J5 | Client vanilla lancé |
| J6 | Sodium réactivé |
| J7 | Iris réactivé |

## Prochaine action autorisée

Préparer la prochaine sous-phase Minecraft générale sans commencer `RenderPipeline`,
`ShaderInstance`, `Program`, `BufferUploader`, Sodium, Iris ou le worldgen lourd.
Conserver DimLib / `AlternateDimensions` pour la phase 2.5.
