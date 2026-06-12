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

Statut : termine le 11 juin 2026.

- compilation complete apres Phase 2.3 : 440 erreurs
- baisse depuis Phase 2.2 : 484 -> 440 (-44)
- erreurs `EntityRenderer` ciblees restantes : 0
- erreurs `EntityType` ciblees restantes : 0
- erreurs de rendu simple ciblees restantes : 0
- nouvelles erreurs introduites : 0
- overlays/debug de `PortalEntityRenderer` dependants de l'ancien buffer reportes
  avec le pipeline de rendu
- framebuffers, shaders, Sodium, Iris, worldgen lourd et DimLib non modifies

- nouvelles génériques `EntityRenderer<E, S>`
- création et enregistrement des `EntityType`
- états de rendu d'entité
- écrans et renderers simples ne dépendant pas des shaders de portail

### 2.4 Réseau et payloads

Statut : termine le 11 juin 2026.

- compilation complete apres Phase 2.4 : 399 erreurs
- baisse depuis Phase 2.3 : 440 -> 399 (-41)
- erreurs payload Fabric ciblees restantes : 0
- erreurs networking ciblees restantes : 0
- erreurs synchronisation chunk / entite ciblees restantes : 0
- nouvelles erreurs introduites : 0
- RenderPipeline, shaders, Sodium, Iris, worldgen lourd, DimLib et
  `AlternateDimensions` non modifies

- payloads Fabric
- codecs réseau
- remplacement des anciennes créations/envois de paquets
- synchronisation client/serveur

Baseline indicative : environ 55 erreurs.

### 2.5 Worldgen et API périphériques

Statut : termine le 11 juin 2026.

- compilation complete apres Phase 2.5 : 361 erreurs
- baisse depuis Phase 2.4 : 399 -> 361 (-38)
- erreurs worldgen ciblees restantes : 0
- erreurs DimLib / `AlternateDimensions` restantes : 15
- erreurs registres / dimensions natives restantes : 0
- nouvelles erreurs introduites : 0
- les 15 erreurs restantes du perimetre viennent du binaire DimLib 1.21.1 ;
  aucune version DimLib compatible Minecraft 26.1 n'est publiee

- `ChunkGenerator`
- carving
- listes pondérées
- biome et génération de dimensions
- API Fabric restantes

### 2.6 Nettoyage Minecraft général restant

Statut : termine le 11 juin 2026.

- compilation complete apres Phase 2.6 : 297 erreurs
- baisse depuis Phase 2.5 : 361 -> 297 (-64)
- jalon sous 300 erreurs atteint
- erreurs `PortalCommand` / `PortalDebugCommands` ciblees restantes : 0
- erreurs `Mesh2D` ciblees restantes : 0
- erreurs `GravityChangerInterface` / `IPPortingLibCompat` restantes : 0
- erreurs `ServerTeleportationManager` restantes : 1, causee par DimLib 1.21.1
- erreurs `ClientWorldLoader` restantes : 2, dont 1 DimLib et 1 construction
  du nouveau `LevelRenderer`
- erreurs DimLib restantes : 15
- erreurs de rendu pur identifiees : 177
- nouvelles erreurs introduites : 0

Gravity Changer est temporairement no-op tant qu'aucune API compatible Minecraft
26.1 n'est disponible. L'ecran Cloth Config retourne temporairement son parent.

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

### 3.1 Préparation du pipeline de rendu vanilla

Statut : terminé le 11 juin 2026.

- compilation complète après Phase 3.1 : 268 erreurs
- baisse depuis Phase 2.6 : 297 -> 268 (-29)
- erreurs de rendu pur identifiées : 148
- diagnostics `ShaderInstance` : 29 -> 23
- diagnostics `Program` : 7 -> 0
- diagnostics `BufferUploader` : 7 -> 7
- diagnostics simples `RenderTarget` / `TextureTarget` / `ON_OSX` : 29 -> 16
- nouvelles erreurs introduites : 0
- `processResources` validé ; les quatre mixins shader obsolètes sont absentes
  du profil vanilla généré

Les anciens mixins `MixinProgram`, `MixinShaderInstance`,
`MixinGameRenderer_Shaders` et `MixinRenderSystem_Clipping` sont temporairement
isolés dans le profil vanilla. `IPRenderPipelines` fournit le point d'ancrage
minimal pour enregistrer les futurs `RenderPipeline` et préparer un
`RenderPass`, sans porter tout `MyRenderHelper` en une seule passe.

Inventaire principal du rendu restant :

- `MyRenderHelper.java` : 59 diagnostics
- `MyGameRenderer.java` : 18 diagnostics
- `ViewAreaRenderer.java` : 10 diagnostics
- `FrontClipping.java` : 8 diagnostics
- `RendererUsingStencil.java` : 7 diagnostics
- `RendererUsingFrameBuffer.java` : 7 diagnostics

### 3.2 MyRenderHelper et BufferUploader

Statut : terminé le 12 juin 2026.

- compilation complète après Phase 3.2 : 203 erreurs
- baisse depuis Phase 3.1 : 268 -> 203 (-65)
- erreurs de rendu pur identifiées : 83
- diagnostics `MyRenderHelper.java` : 59 -> 0
- diagnostics `BufferUploader` : 7 -> 0
- dépendances directes `ShaderInstance` dans `MyRenderHelper` : 0
- nouvelles erreurs introduites : 0

`IPRenderPipelines` fournit maintenant un chemin de dessin minimal :

- construction de géométrie avec `BufferBuilder` / `MeshData`
- upload immédiat vers `GpuBuffer`
- création d'un `RenderPass`
- liaison du `RenderPipeline`, des uniformes et des buffers
- dessin indexé ou non indexé selon le `MeshData`

Le dessin simple `renderScreenTriangle` utilise ce chemin. Les anciens shaders
avancés de framebuffer et de zone de portail restent temporairement no-op.
`ViewAreaRenderer` n'a reçu que l'adaptation minimale nécessaire pour envoyer
son `MeshData` vers `IPRenderPipelines`, sans commencer son port complet.

### 3.3 Renderers avancés, framebuffers et stencil

Statut : terminé le 12 juin 2026.

- compilation complète après Phase 3.3 : 179 erreurs
- baisse depuis Phase 3.2 : 203 -> 179 (-24)
- erreurs de rendu pur identifiées : 59
- erreurs `RendererUsingStencil` restantes : 0
- erreurs `RendererUsingFrameBuffer` restantes : 0
- erreurs `RendererDebug` / `PortalRenderer` restantes : 0
- erreurs `SecondaryFrameBuffer` restantes : 0
- erreurs `GuiPortalRendering` framebuffer restantes : 0
- diagnostics `RenderTarget` / `RenderSystem` pertinents restants : 9
- nouvelles erreurs introduites : 0

`IPRenderPipelines` centralise maintenant le nettoyage couleur/profondeur des
`RenderTarget` via `CommandEncoder`. Les anciens `bindWrite`, `checkStatus`,
`ON_OSX`, `_clearColor`, `_clearDepth` et appels globaux de profondeur ont été
retirés des renderers ciblés.

Les effets stencil et framebuffer avancés restent partiellement dégradés tant
que leurs états complets ne sont pas exprimés dans des `RenderPipeline`.
Les diagnostics `RenderSystem` restants appartiennent uniquement aux zones
reportées : `MyGameRenderer`, `FrontClipping`, `MixinGameRenderer` et
`MixinLevelRenderer`.

### 3.4 FrontClipping et MyGameRenderer léger

Statut : terminé le 12 juin 2026.

- compilation complète après Phase 3.4 : 152 erreurs
- baisse depuis Phase 3.3 : 179 -> 152 (-27)
- erreurs `FrontClipping` restantes : 0
- erreurs `MyGameRenderer` restantes : 0
- erreurs `MixinGameRenderer` restantes : 0
- diagnostics `RenderSystem` restants : 1, dans `MixinLevelRenderer`
- nouvelles erreurs introduites : 0

Les accès directs à `ShaderInstance` et aux anciens uniformes de clipping ont
été retirés de `FrontClipping`. L'état du plan de clipping reste calculé, mais
son envoi aux shaders est temporairement no-op jusqu'à son branchement sur les
nouveaux `RenderPipeline`.

`MyGameRenderer` conserve la structure de changement et de restauration du
monde rendu. L'appel récursif à `LevelRenderer`, ainsi que la restauration du
fog et de l'éclairage, sont temporairement no-op car ils dépendent du port lourd
du nouveau pipeline. L'ancien redirect de projection de `MixinGameRenderer`,
ciblant une méthode supprimée, a été retiré.

### 3.5 LevelRenderer et ViewArea minimal

Statut : terminé le 12 juin 2026.

- compilation complète après Phase 3.5 : 133 erreurs
- baisse depuis Phase 3.4 : 152 -> 133 (-19)
- erreurs `MixinLevelRenderer` restantes : 0
- erreurs `ImmPtlViewArea` restantes : 0
- erreurs `ViewAreaRenderer` restantes : 0
- erreurs clouds / optional restantes : 0
- erreurs `WireRenderingHelper` restantes : 0
- nouvelles erreurs introduites : 0

`MixinLevelRenderer` utilise maintenant `setCameraPosition` et l'état
`SectionMesh` pour les vérifications minimales. L'ancien hook global
`RenderSystem.clear` a été retiré, le nettoyage étant désormais géré par le
pipeline 26.1.

`ImmPtlViewArea` utilise le constructeur `RenderSection(int, long)`, les nœuds
`SectionPos` et le reset contrôlé des sections. `ViewAreaRenderer` laisse les
masques de couleur aux `RenderPipeline`. Le rendu de boîte de
`WireRenderingHelper` est produit localement avec douze segments.

Les mixins `MixinLevelRenderer_Optional` et `MixinLevelRenderer_Clouds` sont
temporairement exclus du profil vanilla. Le tri translucide spécial et
l'optimisation des clouds seront réactivés après stabilisation du pipeline de
niveau.

### 3.6 Rendu contextuel, caméra, fog et transformations

Statut : terminé le 12 juin 2026.

- compilation complète après Phase 3.6 : 120 erreurs
- baisse depuis Phase 3.5 : 133 -> 120 (-13)
- erreurs `FogRendererContext` restantes : 0
- erreurs `CrossPortalViewRendering` restantes : 0
- erreurs `TransformationManager` restantes : 0
- erreurs `VisibleSectionDiscovery` restantes : 0
- erreurs de rendu ciblées restantes : 0
- nouvelles erreurs introduites : 0

Les accès directs à `Minecraft.cameraEntity` utilisent maintenant
`getCameraEntity()`. Le fog contextuel conserve la couleur mémorisée, mais son
recalcul avancé est temporairement no-op. La caméra cross-portal et la mise à
jour manuelle de caméra sont également no-op jusqu'au branchement sur l'état de
caméra extrait par le renderer 26.1.

`VisibleSectionDiscovery` utilise désormais `RenderSection.getSectionNode()` et
`SectionPos.x/y/z`. La collision entre le type de configuration local et
`com.mojang.blaze3d.shaders.ShaderType` a été corrigée. L'ancien hook
`Camera.setup` de `MixinCamera` a été retiré.

### 4.0 Migration finale des API Minecraft 26.1

Statut : terminé le 12 juin 2026.

- compilation complète après Phase 4.0 : 48 erreurs
- baisse depuis Phase 3.6 : 120 -> 48 (-72)
- erreurs NBT restantes : 16
- erreurs helper restantes : 0
- erreurs UI restantes : 0
- erreurs gameplay restantes : 17
- nouvelles erreurs introduites : 0

Les neuf cibles prioritaires (`IPMcHelper`, `McHelper`,
`MyNbtTextFormatter`, `PortalPlaceholderBlock`, `IPortalInitialScreen`,
`ScaleUtils`, `CommandStickItem`, `PortalHelperItem` et
`PortalWandInteraction`) ne produisent plus aucun diagnostic.

Les migrations couvrent notamment `Level.isClientSide()`, les nouveaux
`ClickEvent`, les messages système, les signatures de tooltip et
`Block.updateShape`, ainsi que les accès NBT optionnels ciblés. Les principaux
blocs restants sont `AlternateDimensions`, `GlobalPortalStorage`, les
gestionnaires de téléportation exclus et quelques classes client du portal
wand.

### 4.1 Finalisation hors DimLib

Statut : terminé le 12 juin 2026.

- compilation complète après Phase 4.1 : 16 erreurs
- baisse depuis Phase 4.0 : 48 -> 16 (-32)
- erreurs NBT restantes : 0
- erreurs gameplay restantes : 0
- erreurs wand restantes : 0
- erreurs DimLib / DimensionAPI restantes : 15
- nouvelles erreurs introduites : 0

Les dix cibles prioritaires sont à zéro diagnostic hors hook DimensionAPI de
`GlobalPortalStorage`, volontairement conservé. Le stockage global utilise
maintenant `SavedDataType`, un codec NBT et `TagValueInput` / `TagValueOutput`.
Les render types du wand utilisent `RenderTypes.lines()`.

Les 16 diagnostics restants sont composés de 15 erreurs DimLib / DimensionAPI
et du constructeur `LevelRenderer` de `ClientWorldLoader`, tous explicitement
exclus de cette phase.

### 4.2 Isolation DimLib et dernier LevelRenderer

Statut : terminé le 12 juin 2026.

- compilation complète après Phase 4.2 : 0 erreur
- baisse depuis Phase 4.1 : 16 -> 0 (-16)
- erreurs DimLib / DimensionAPI restantes : 0
- erreurs `LevelRenderer` restantes : 0
- nouvelles erreurs introduites : 0
- `compileJava` vanilla : réussi

Les abonnements DimensionAPI de `ClientWorldLoader`,
`ServerTeleportationManager`, `GlobalPortalStorage` et `DimensionIntId` sont
temporairement désactivés. `AlternateDimensions` conserve ses clés et
générateurs comme façade vanilla, mais n'enregistre plus de dimensions
dynamiques ni de templates DimLib.

La création du renderer secondaire de `ClientWorldLoader` réutilise
temporairement le renderer principal. Ce chemin est inactif tant que les
dimensions dynamiques DimLib sont isolées.

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

Tester le lancement du client vanilla et stabiliser le comportement minimal
des portails avant toute réactivation de Sodium, Iris ou DimLib.
