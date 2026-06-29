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

### 4.3 Test de lancement client vanilla

Statut : terminé le 12 juin 2026.

- `compileJava` vanilla : réussi, 0 erreur
- menu principal : atteint
- mod chargé : confirmé (`immersive_portals 7.0.0-alpha.1`)
- monde solo : atteint, joueur connecté et stable pendant le test
- portail visible / traversable : non vérifié, rendu avancé maintenu no-op
- nouvelles erreurs de compilation : 0

Les mixins runtime obsolètes liés à DimLib, au rendu avancé et à la
synchronisation cross-dimensionnelle ont été isolés du profil vanilla.
Le monde de test est `run/saves/Phase43Test3`.

### 4.4 Test portail minimal vanilla

Statut : terminé le 12 juin 2026.

- portail créé : oui
- portail sauvegardé et rechargé : oui
- portail visible : non
- portail traversable : non
- crash final : non
- `compileJava` vanilla : réussi, 0 erreur
- nouvelles erreurs de compilation : 0

Le portail Overworld vers Overworld est créé avec
`/portal euler make_portal` et son entité persiste après rechargement du monde
`Phase43Test3`. Le profil vanilla minimal ne synchronise cependant pas encore
l'entité portail vers le client : le portail reste invisible et la traversée
ne se déclenche pas.

Trois blocages runtime directement liés au test ont été corrigés :

- callbacks `IEChunkMap` facultatifs lorsque les mixins cross-dimensionnels
  sont isolés ;
- validation réseau sans cast forcé vers `IEServerPlayNetworkHandler` ;
- mise à jour du contexte fog en no-op lorsque son mixin est isolé.

Les crash reports complets des blocages corrigés sont :

- `run/crash-reports/crash-2026-06-12_14.28.49-server.txt`
- `run/crash-reports/crash-2026-06-12_14.34.09-client.txt`

### 4.5 Synchronisation client minimale des portails

Statut : terminé le 12 juin 2026.

- `compileJava` vanilla : réussi, 0 erreur
- `runClient` vanilla : réussi
- portail créé : oui
- portail présent côté client : oui
- portail visible : non, rendu avancé maintenu no-op
- portail traversable : non confirmé pendant ce test
- crash final : non
- nouvelles erreurs de compilation : 0

Le suivi d'entités cross-dimensionnel complet reste isolé. Il remplace trop de
comportements vanilla et dépend du gestionnaire de chunks encore désactivé.
Une façade minimale synchronise désormais les `PortalSyncPacket` :

- lors de la création d'un portail ;
- lors de la connexion d'un joueur ;
- lors d'une demande de resynchronisation d'un portail.

La commande client `imm_ptl_client_debug report_loaded_portals` confirme que
les portails persistants du monde `Phase43Test3` sont présents côté client.
`IEChunkMap` et `IEServerPlayNetworkHandler` restent optionnels et sécurisés.

### 4.6 Rendu minimal visible des portails

Statut : terminé le 12 juin 2026.

- `compileJava` vanilla : réussi, 0 erreur
- `runClient` vanilla : réussi
- portail présent côté client : oui
- portail visible : oui, sous forme de cadre cyan minimal
- portail traversable : non testé pendant cette phase
- crash final : non
- nouvelles erreurs de compilation : 0

`PortalEntityRenderer` soumet désormais un cadre translucide et ses diagonales
avec `SubmitNodeCollector.submitCustomGeometry` et `RenderTypes.linesTranslucent`.
La géométrie utile est copiée dans `PortalRenderState`, sans réactiver le rendu
récursif, les shaders hérités, le stencil ou les framebuffers avancés.

Le monde `Phase43Test3` se charge sans crash. La commande client
`imm_ptl_client_debug report_loaded_portals` confirme cinq portails côté client,
et la capture `run/screenshots/2026-06-12_15.07.02.png` montre le cadre minimal
dans le monde.

### 4.7 Traversée minimale des portails vanilla

Statut : terminé le 12 juin 2026.

- `compileJava` vanilla : réussi, 0 erreur
- `runClient` vanilla : réussi
- portail visible : oui
- traversée Overworld vers Overworld déclenchée : oui
- téléportation correcte : oui
- Nether / End : non testés
- crash final : non
- nouvelles erreurs de compilation : 0

La commande de développement
`imm_ptl_client_debug test_minimal_portal_traversal` prépare un franchissement
reproductible du portail client le plus proche. Le test final produit
`Client Teleported Statically`, puis la position du joueur correspond à la
destination du portail avant sa chute naturelle.

La chaîne existante `ClientTeleportationManager` vers le paquet Fabric
`TeleportPacket` et `ServerTeleportationManager` fonctionne donc sans réactiver
les mixins de synchronisation lourde. Aucun repli serveur supplémentaire n'a
été conservé.

Un crash découvert pendant le test a été corrigé dans le rendu minimal :
`RenderTypes.linesTranslucent()` exige l'attribut `LineWidth` sur chaque sommet.
Le stacktrace complet est conservé dans
`run/crash-reports/crash-2026-06-12_15.13.43-client.txt`.

### 4.8 Audit des commandes

Statut : terminé le 12 juin 2026.

- `compileJava` vanilla : réussi, 0 erreur
- `runClient` vanilla : réussi
- `PortalCommand` enregistré : oui
- `PortalDebugCommands` enregistré sous `/portal debug` : oui
- `ClientDebugCommand` enregistré sous `/imm_ptl_client_debug` : oui
- commandes exclues par le profil vanilla : non
- crash final : non

Les trois callbacks sont confirmés par des logs runtime explicites. `/portal`
est masqué pour le joueur de test car celui-ci est en survie sans permission
opérateur. `easeCreativePermission=true` autorise seulement les joueurs en
créatif. `/imm_ptl_client_debug` reste disponible pour tous les clients.

Le rapport complet est `PHASE4.8_COMMAND_AUDIT.md`.

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

Tester la création et la traversée minimale d'un portail avec le profil
vanilla avant toute réactivation de Sodium, Iris ou DimLib.

### 4.9 Portail de test serveur minimal

Statut : terminé le 12 juin 2026.

- `compileJava` vanilla : réussi, 0 erreur
- `runClient` vanilla : réussi sur deux lancements
- commande : `/imm_ptl_debug create_minimal_test_portal`
- utilisable en survie solo sans permission opérateur : oui
- portail présent côté client et cadre cyan visible : oui
- traversée Overworld vers Overworld : oui
- sauvegarde et rechargement : réussis
- régression de `test_minimal_portal_traversal` : aucune
- `Duplicate entity UUID` dans le monde propre : 0
- crash final : non

La commande est réservée aux environnements de développement et utilise
`McHelper.spawnServerEntity`, donc le chemin serveur de sauvegarde et
synchronisation déjà validé. Le monde propre `Phase49Test`, sans anciennes
données d'entités, ne reproduit pas les avertissements UUID de
`Phase43Test3`.

Rapport : `PHASE4.9_MINIMAL_TEST_PORTAL.md`.

### 5.0 Rendu récursif minimal vanilla

Statut : terminé avec blocage technique documenté le 12 juin 2026.

- `compileJava` vanilla : réussi, 0 erreur
- `runClient` vanilla : réussi sur deux lancements
- portail client et cadre cyan fallback : fonctionnels
- traversée Overworld vers Overworld : fonctionnelle
- sauvegarde/rechargement : réussi
- `Duplicate entity UUID` : 0
- première vue destination visible : non
- crash final : non

Le chemin framebuffer minimal, limité à une récursion et à la dimension
courante, est préparé. Son exécution est cependant bloquée proprement car le
profil vanilla exclut `MixinGameRenderer` dans `build.gradle`. Sans ce mixin,
`GameRenderer` n'implémente pas `IEGameRenderer`, interface requise pour
changer la caméra, la lightmap et le contexte de rendu. Le renderer détecte
ce cas et conserve le cadre cyan avec un log de fallback non spammy.

Rapport : `PHASE5.0_MINIMAL_RECURSIVE_RENDERING.md`.

### 5.1 Activation minimale de MixinGameRenderer

Statut : termine avec nouveau blocage technique documente le 12 juin 2026.

- `compileJava` vanilla : reussi, 0 erreur
- `runClient` vanilla final : reussi sur deux lancements, aucun crash
- `IEGameRenderer` actif au runtime : oui
- portail client et cadre cyan fallback : fonctionnels
- traversee Overworld vers Overworld : fonctionnelle
- sauvegarde/rechargement : reussi
- `Duplicate entity UUID` : 0
- premiere vue destination visible : non

Le filtre vanilla de `build.gradle` ne retire plus `MixinGameRenderer`.
Le mixin a ete reduit a une facade `IEGameRenderer` sans anciennes injections
de rendu ou de shaders. Le chemin recursif depasse donc le blocage de la phase
5.0, puis s'arrete proprement sur le nouveau blocage exact : `MixinCamera`
reste isole et la camera vanilla n'implemente pas `IECamera`.

Rapport : `PHASE5.1_ENABLE_GAME_RENDERER_MIXIN.md`.

### 5.2 Activation minimale de MixinCamera

Statut : termine avec nouveau blocage technique documente le 12 juin 2026.

- `compileJava` et `processResources` : reussis
- `runClient` vanilla final : reussi sur deux lancements, aucun crash
- `IEGameRenderer` actif au runtime : oui
- `IECamera` actif au runtime : oui
- portail client et cadre cyan fallback : fonctionnels
- sauvegarde/rechargement : reussi
- `Duplicate entity UUID` : 0
- premiere vue destination visible : non

Le filtre vanilla de `build.gradle` ne retire plus `MixinCamera`. Le mixin a
ete reduit a la facade `IECamera`, sans injections historiques de fog ou de
camera detachee. Le rendu recursif depasse le blocage camera, puis s'arrete
proprement sur le nouveau blocage exact : `MixinLevelRenderer` reste isole et
`LevelRenderer` n'implemente pas `IEWorldRenderer`.

Rapport : `PHASE5.2_ENABLE_CAMERA_MIXIN.md`.

### 5.3 Activation minimale de MixinLevelRenderer

Statut : termine avec nouveau blocage technique documente le 13 juin 2026.

- `compileJava` et `processResources` : reussis
- `runClient` vanilla final : reussi sur deux lancements, aucun crash
- `IEGameRenderer` actif au runtime : oui
- `IECamera` actif au runtime : oui
- `IEWorldRenderer` actif au runtime : oui
- portail client et cadre cyan fallback : fonctionnels
- sauvegarde/rechargement : reussi
- `Duplicate entity UUID` : 0
- premiere vue destination visible : non

Le filtre vanilla de `build.gradle` ne retire plus `MixinLevelRenderer`. Le
mixin a ete remplace par une facade `IEWorldRenderer` sans injections
historiques de rendu. Le chemin recursif depasse le blocage LevelRenderer,
puis s'arrete proprement sur le nouveau blocage exact : le contexte fog
avance reste isole et `FogRendererContext.swappingManager` n'est pas
initialise.

Rapport : `PHASE5.3_ENABLE_LEVEL_RENDERER_MIXIN.md`.

### 5.4 Fallback fog vanilla minimal

Statut : termine avec nouveau blocage technique documente le 13 juin 2026.

- `compileJava` et `processResources` : reussis
- `runClient` vanilla final : BUILD SUCCESSFUL, aucun crash
- `IEGameRenderer`, `IECamera` et `IEWorldRenderer` actifs : oui
- fallback fog vanilla : actif lorsque le contexte avance est absent
- portail client et cadre cyan fallback : fonctionnels
- traversee Overworld vers Overworld : observee deux fois
- sauvegarde a la fermeture : reussie
- `Duplicate entity UUID` : 0
- premiere vue destination visible : non

Le chemin recursif depasse maintenant le blocage
`FogRendererContext.swappingManager` sans reactiver les mixins fog. Lorsque le
contexte avance est absent, le fog vanilla courant est conserve. Le nouveau
blocage exact est `MixinParticleEngine`, encore isole du profil vanilla :
`ParticleEngine` n'implemente donc pas `IEParticleManager`. Une garde preflight
conserve le fallback cyan sans crash ni mutation partielle du contexte client.

Rapport : `PHASE5.4_FOG_FALLBACK.md`.

### 5.5 Activation minimale de MixinParticleEngine

Statut : termine avec nouveau blocage technique documente le 18 juin 2026.

- `compileJava` et `processResources` : reussis
- `runClient` vanilla final : BUILD SUCCESSFUL, aucun crash
- `IEGameRenderer`, `IECamera`, `IEWorldRenderer` actifs : oui
- `IEParticleManager` actif : oui
- fallback fog vanilla : conserve
- portail client et cadre cyan fallback : fonctionnels
- sauvegarde a la fermeture : reussie
- `Duplicate entity UUID` : 0
- premiere vue destination visible : non

Le filtre vanilla de `build.gradle` ne retire plus
`client.particle.MixinParticleEngine`. Le mixin a ete reduit a une facade
`IEParticleManager` minimale, avec uniquement le changement temporaire de monde
du `ParticleEngine`. Le chemin recursif depasse le blocage particules, puis
echoue proprement dans `GameRenderer.lightmap(...)` car le champ runtime
`Lightmap` de `GameRenderer` est nul pendant le rendu recursif minimal. Le
fallback cyan capture l'echec sans crash.

Rapport : `PHASE5.5_ENABLE_PARTICLE_ENGINE_MIXIN.md`.

### 5.6 Etat Lightmap runtime du rendu recursif minimal

Statut : termine avec nouveau blocage technique documente le 18 juin 2026.

- `compileJava` et `processResources` : reussis
- `runClient` vanilla final : BUILD SUCCESSFUL, aucun crash
- `IEGameRenderer`, `IECamera`, `IEWorldRenderer` actifs : oui
- `IEParticleManager` actif : oui
- lightmap runtime valide pendant le preflight recursif : oui
- portail client et cadre cyan fallback : fonctionnels
- sauvegarde a la fermeture : reussie
- `Duplicate entity UUID` : 0
- premiere vue destination visible : non

`GameRenderer` 26.1 possede un champ `private final Lightmap lightmap`,
initialise dans son constructeur et utilise par `lightmap()` /
`levelLightmap()`. Le crash venait du cas Overworld vers Overworld :
`DimensionRenderHelper.lightmapTexture` vaut volontairement `null` pour le
monde principal, et `MyGameRenderer` passait ce `null` a la facade
`IEGameRenderer`. Le rendu recursif reutilise maintenant le lightmap principal
quand aucun lightmap de dimension dedie n'existe.

Le chemin a ensuite expose le nouveau blocage exact :
`LevelRenderer.submitEntities` n'est pas reentrant lorsque le rendu recursif
est declenche depuis `PortalEntityRenderer.submit`. Ce chemin est donc garde en
fallback explicite jusqu'a deplacer le rendu recursif vers un hook plus sur.

Rapport : `PHASE5.6_LIGHTMAP_RUNTIME_STATE.md`.

### 5.7 Hook de rendu non reentrant

Statut : termine avec nouveau blocage technique documente le 18 juin 2026.

- `compileJava` et `processResources` : reussis
- `runClient` vanilla final : BUILD SUCCESSFUL, aucun crash
- `ConcurrentModificationException` : 0
- portail client et cadre cyan fallback : conserves
- sauvegarde a la fermeture : reussie
- `Duplicate entity UUID` : 0
- premiere vue destination visible : non

`PortalEntityRenderer.submit` ne lance plus directement le rendu recursif. Il
collecte seulement un portail candidat. Un hook minimal dans
`MixinGameRenderer.renderLevel` tente ensuite le rendu framebuffer au debut
d'une frame, hors de l'iteration `LevelRenderer.submitEntities`.

Le deplacement elimine le blocage de reentrance. Le nouveau blocage exact est
le blit du framebuffer secondaire vers la cible principale : dans ce hook, le
pipeline GPU 26.1 peut avoir un buffer deja ferme (`Buffer already closed`).
L'echec est capture et ramene au fallback cyan sans crash.

Rapport : `PHASE5.7_NON_REENTRANT_RENDER_HOOK.md`.

### 5.8 Blit framebuffer GPU 26.1

Statut : termine avec correctif minimal applique le 18 juin 2026.

- `compileJava` et `processResources` : reussis
- `runClient` vanilla final : BUILD SUCCESSFUL, aucun crash
- `runClient --quickPlaySingleplayer=Phase50Test` : BUILD SUCCESSFUL, monde charge
- `ConcurrentModificationException` : 0
- `Buffer already closed` : 0 dans les logs de validation apres correctif
- `Duplicate entity UUID` : 0
- portail client et cadre cyan fallback : conserves
- Sodium, Iris, DimLib, shaders, fog mixins et clipping mixins : non touches
- premiere vue destination visible : pas encore confirmee automatiquement

La cause du blocage etait dans `IPRenderPipelines.drawMesh`. Les buffers
retournes par `VertexFormat.uploadImmediateVertexBuffer(...)` et
`uploadImmediateIndexBuffer(...)` sont les buffers immediats internes de Mojang,
caches et reutilises par le `VertexFormat`. Immersive Portals les fermait apres
le draw avec un try-with-resources, donc l'upload suivant pouvait ecrire dans un
`GpuBuffer` deja ferme.

Le correctif ferme maintenant uniquement le `MeshData`, qui reste propriete du
draw appelant. Les `GpuBuffer` immediats restent geres par Mojang. Des logs
uniques indiquent maintenant la tentative, le succes ou l'echec du blit
framebuffer minimal.

Les tests automatises chargent le monde et confirment l'absence de crash et de
`Buffer already closed`. La tentative d'injection clavier de
`/imm_ptl_debug create_minimal_test_portal` n'a pas ete confirmee dans les logs,
donc l'apparition visuelle de la vue destination reste a revalider manuellement
avec le portail dans le champ.

Rapport : `PHASE5.8_FRAMEBUFFER_BLIT_GPU_PIPELINE.md`.

### 5.9 Validation controlee du rendu recursif minimal

Statut : termine avec validation runtime controlee le 18 juin 2026.

- `compileJava` et `processResources` : reussis
- `runClient --quickPlaySingleplayer=Phase50Test` : BUILD SUCCESSFUL
- portail cree explicitement par commande dev : oui
- joueur place face au portail : oui
- portail collecte par `PortalEntityRenderer` : oui
- rendu declenche depuis le hook `GameRenderer.renderLevel` : oui
- tentative de blit framebuffer : oui
- blit framebuffer reussi : oui
- `Buffer already closed` : 0
- `ConcurrentModificationException` : 0
- `Duplicate entity UUID` : 0
- Sodium, Iris, DimLib, shaders, fog mixins et clipping mixins : non touches

Une commande de developpement a ete ajoutee :
`/imm_ptl_debug create_visible_test_portal`. Elle cree un portail minimal et
replace le joueur face a lui pour rendre le test reproductible.

Un declencheur client dev optionnel a aussi ete ajoute pour les tests
automatises :
`IMM_PTL_AUTO_VISIBLE_TEST_PORTAL=true`. En environnement de developpement, il
envoie `imm_ptl_debug create_visible_test_portal` apres quelques ticks en monde.

Les logs valident toute la chaine runtime :
`Queued minimal recursive portal from PortalEntityRenderer`,
`Rendering minimal recursive portal from GameRenderer renderLevel hook`,
`Attempting minimal recursive portal framebuffer blit`, puis
`Minimal recursive portal framebuffer blit succeeded`.

La capture visuelle automatique n'a pas ete utilisable car elle a pris une
autre fenetre au premier plan. La preuve screenshot n'est donc pas retenue. La
vue destination minimale est validee cote pipeline/logs jusqu'au blit reussi,
mais l'inspection visuelle humaine fenetre Minecraft au premier plan reste a
faire avant d'evaluer les artefacts de projection, profondeur, stencil ou
clipping.

Rapport : `PHASE5.9_VISUAL_RECURSIVE_RENDER_VALIDATION.md`.

### 5.10 Inspection visuelle interactive du rendu recursif minimal

Statut : termine avec preuve visuelle native Minecraft le 18 juin 2026.

- `compileJava` et `processResources` : reussis
- `runClient --quickPlaySingleplayer=Phase50Test` : BUILD SUCCESSFUL
- portail cree automatiquement par commande dev : oui
- joueur place face au portail : oui
- portail collecte par `PortalEntityRenderer` : oui
- rendu declenche depuis `GameRenderer.renderLevel` : oui
- blit framebuffer tente : oui
- blit framebuffer reussi : oui
- capture Minecraft native obtenue : oui
- `Buffer already closed` : 0
- `ConcurrentModificationException` : 0
- `Duplicate entity UUID` : 0
- Sodium, Iris, DimLib, shaders, fog mixins et clipping mixins : non touches

La capture native `run/screenshots/phase5.10-minimal-recursive-portal.png`
confirme qu'une texture framebuffer destination est visible dans la zone du
portail. La vue destination minimale est donc restauree au niveau pipeline.

Artefacts observes :
- image destination fortement retournee/inversee ;
- mauvais cadrage ;
- rendu visible dans un petit rectangle central ;
- exterieur noir dans la capture native ;
- clipping/profondeur non fiables ;
- fog en fallback vanilla ;
- une seule recursion.

Le prochain micro-correctif prioritaire n'est pas Sodium/Iris ni stencil avance.
Il faut d'abord corriger le cadrage/orientation du quad texture :
ordre des sommets, UV, orientation Y du framebuffer et projection utilisee au
moment du blit.

Rapport : `PHASE5.10_VISUAL_ARTIFACT_AUDIT.md`.

### 5.11 Orientation et cadrage du quad framebuffer

Statut : termine avec blocage de cadrage documente le 18 juin 2026.

- `compileJava` et `processResources` : reussis
- `runClient --quickPlaySingleplayer=Phase50Test` : BUILD SUCCESSFUL
- portail cree automatiquement par commande dev : oui
- portail collecte par `PortalEntityRenderer` : oui
- rendu destination declenche depuis `GameRenderer.renderLevel` : oui
- blit framebuffer differe depuis `PortalEntityRenderer` : oui
- blit framebuffer reussi : oui
- capture Minecraft native obtenue : oui
- `Buffer already closed` : 0
- `ConcurrentModificationException` : 0
- `Duplicate entity UUID` : 0
- crash : non
- Sodium, Iris, DimLib, shaders, fog mixins et clipping mixins : non touches

Correctifs appliques :
- inversion minimale des UV V du framebuffer ;
- diagnostics uniques des quatre sommets du quad, de la taille framebuffer et de
  la taille fenetre ;
- nom de capture configurable via
  `IMM_PTL_MINIMAL_RECURSIVE_PORTAL_SCREENSHOT` ;
- routage explicite du mode minimal vers `rendererUsingFrameBuffer` ;
- tentative de blit du quad depuis le chemin `PortalEntityRenderer`, apres rendu
  non reentrant du framebuffer depuis le hook `GameRenderer`.

Resultat visuel :
- la texture framebuffer reste visible ;
- le framebuffer secondaire a la bonne taille (`fb=854x480`, fenetre
  `854x480`) ;
- l'image n'est plus le meme petit rectangle central que Phase 5.10 ;
- le quad texture reste mal cadre, projete en bande oblique sur le bord de
  l'ecran ;
- l'exterieur noir reste present dans la capture opt-in ;
- clipping, profondeur et stencil restent volontairement incomplets.

Cause restante :
le quad framebuffer est encore dessine via un `RenderPass` immediat
(`IPRenderPipelines.drawTexturedMesh`) et non comme vraie geometrie d'entite
soumise par `SubmitNodeCollector`. Meme avec le `PoseStack` du renderer
d'entite, ce draw immediat ne participe pas au meme graphe de rendu, au meme
tri et au meme contexte de matrices que le cadre cyan vanilla.

Prochaine etape recommandee :
creer un pont minimal render-graph/SubmitNodeCollector capable de dessiner un
quad d'entite avec une texture `GpuTextureView` de framebuffer, ou une petite
abstraction equivalente compatible 26.1. Ne pas commencer Sodium/Iris/DimLib ni
stencil avance avant ce pont.

Rapport : `PHASE5.11_FRAMEBUFFER_QUAD_ORIENTATION.md`.

### 5.12 Pont framebuffer vers SubmitNodeCollector

Statut : termine avec pont render graph valide le 19 juin 2026.

- `compileJava` et `processResources` : BUILD SUCCESSFUL
- `runClient --quickPlaySingleplayer=Phase50Test` : BUILD SUCCESSFUL
- portail collecte : oui
- framebuffer destination rendu depuis le hook non reentrant : oui
- texture framebuffer dynamique disponible : oui (`854x480`)
- quad texture soumis via `SubmitNodeCollector` : oui
- capture Minecraft native obtenue : oui
- `Buffer already closed` : 0
- `ConcurrentModificationException` : 0
- `Duplicate entity UUID` : 0
- crash : non
- fallback/cadre cyan conserve : oui
- Sodium, Iris, DimLib, shaders, fog mixins et clipping mixins : non touches

Le pont utilise un alias `AbstractTexture` non proprietaire enregistre dans le
`TextureManager` sous `imm_ptl:minimal_portal_framebuffer`. Un `RenderType`
minimal resout cet identifiant et reutilise le pipeline
`DRAW_FRAMEBUFFER_IN_AREA`. L'alias ne ferme jamais les ressources GPU possedees
par le framebuffer secondaire.

`PortalEntityRenderer.submit` transmet maintenant son `SubmitNodeCollector` au
renderer minimal. Le quad `POSITION_TEX` est soumis avec les memes coins locaux
et le meme `PoseStack` que le cadre cyan. Le rendu destination reste execute au
HEAD de `GameRenderer.renderLevel`; aucune reentrance monde n'est introduite
depuis le renderer d'entite.

Resultat visuel : la bande oblique de Phase 5.11 a disparu. La texture
destination est maintenant placee avec le rectangle du portail au centre de la
vue. Les cadres cyan imbriques, le clipping absent, la profondeur imparfaite et
le fog vanilla restent des limites volontaires du rendu minimal.

Capture : `run/screenshots/phase5.12-submit-node-quad.png`.

Rapport : `PHASE5.12_SUBMIT_NODE_FRAMEBUFFER_QUAD.md`.

### 5.13 Clipping minimal vanilla du rendu destination

Statut : termine avec prefiltrage CPU limite le 19 juin 2026.

- `compileJava` et `processResources` : BUILD SUCCESSFUL
- `runClient --quickPlaySingleplayer=Phase50Test` : BUILD SUCCESSFUL
- portail collecte et framebuffer destination rendu : oui
- texture framebuffer disponible : oui
- quad texture `SubmitNodeCollector` conserve : oui
- clipping minimal tente : oui
- plan destination calcule : oui
- prefiltrage CPU applique pendant le framebuffer : oui
- une entite portail derriere le plan effectivement ignoree : oui
- capture native obtenue : oui
- `Buffer already closed` : 0
- `ConcurrentModificationException` : 0
- `Duplicate entity UUID` : 0
- crash : non
- cadre cyan fallback conserve : oui
- Sodium, Iris, DimLib, shaders et mixins clipping/fog non touches

Le fallback utilise `PortalRendering.getActiveClippingPlane()` uniquement
pendant le rendu du framebuffer secondaire. `PortalEntityRenderer` ignore une
entite portail seulement si son centre et ses quatre coins sont tous derriere
le plan. L'etat est restaure dans un `finally`.

La capture `run/screenshots/phase5.13-minimal-clipping.png` confirme que le pont
framebuffer reste stable et qu'un portail hors demi-espace a ete filtre. La
reduction visuelle reste modeste : les blocs, entites vanilla et portails du
cote conserve ne sont pas decoupes.

Limite exacte : les shaders vanilla 26.1 n'exposent pas de plan global et
n'ecrivent pas `gl_ClipDistance`. `GL_CLIP_PLANE0` seul ne fournit donc plus de
clipping general. Un vrai clipping de scene demandera plus tard un pipeline
shader dedie ou un masque stencil/profondeur. `MixinRenderSystem_Clipping` n'a
pas ete reactive.

Rapport : `PHASE5.13_MINIMAL_FRONT_CLIPPING.md`.

### 5.14 Audit et prototype de masque stencil/profondeur

Statut : termine avec prototype profondeur valide le 19 juin 2026.

- `compileJava` et `processResources` : BUILD SUCCESSFUL
- `runClient --quickPlaySingleplayer=Phase50Test` : BUILD SUCCESSFUL
- framebuffer destination et pont `SubmitNodeCollector` : conserves
- profondeur cible principale : disponible
- profondeur framebuffer secondaire : disponible
- stencil utilisable via `RenderPipeline` : non
- masque profondeur tente : oui
- masque profondeur applique : oui
- capture native obtenue : oui
- `Buffer already closed` : 0
- `ConcurrentModificationException` : 0
- `Duplicate entity UUID` : 0
- crash : non
- cadre cyan fallback conserve : oui
- Sodium, Iris, DimLib et ancien stencil avance non touches

Le prototype utilise deux passes ordonnees dans le render graph :

1. rectangle portail sans ecriture couleur, test profondeur
   `LESS_THAN_OR_EQUAL`, ecriture profondeur active ;
2. quad framebuffer avec test profondeur `EQUAL`, sans reecriture profondeur.

Les deux passes utilisent les memes coins locaux et le meme `PoseStack`. Si la
profondeur ou un pipeline manque, le renderer reprend automatiquement le quad
non masque de Phase 5.12.

Audit stencil : en 26.1, `DepthStencilState` ne contient que le test/ecriture de
profondeur et le biais. Il n'expose aucune operation, reference ou masque
stencil. Le chemin public `RenderPipeline` ne permet donc pas un stencil minimal
isole dans ce profil.

Resultat visuel : l'occlusion du quad contre la profondeur principale est
explicite et stable, mais l'amelioration reste faible. Un masque rectangulaire
ne peut pas retirer le contenu deja rendu dans la texture destination. Les
cadres imbriques demandent encore un stencil integre ou un pipeline shader de
clipping, tous deux hors scope.

Capture : `run/screenshots/phase5.14-minimal-mask.png`.

Rapport : `PHASE5.14_MINIMAL_STENCIL_DEPTH_MASK.md`.

### 5.15 Consolidation du rendu minimal vanilla

Statut : termine et stabilise le 20 juin 2026.

- `compileJava` et `processResources` : BUILD SUCCESSFUL
- run menu sans flags dev : BUILD SUCCESSFUL, menu atteint
- run Phase50Test : BUILD SUCCESSFUL
- framebuffer destination visible et pont `SubmitNodeCollector` : conserves
- masque profondeur : conserve avec fallback
- cadre cyan fallback : conserve
- traversée Overworld vers Overworld : revalidee
- `Client Teleported Statically` : observe deux fois
- fermeture/sauvegarde puis rechargement : reussis
- portail sauvegarde recollecte apres rechargement : oui
- capture native differee obtenue : oui
- `Buffer already closed` : 0
- `ConcurrentModificationException` : 0
- `Duplicate entity UUID` : 0
- crash et crash fermeture : 0
- Sodium, Iris, DimLib et rendu avance : non touches

Nettoyage applique :
- log du premier rendu recursif rendu strictement unique ;
- separation documentee entre dessin immediat legacy et soumission vanilla ;
- alias texture documente comme non proprietaire ;
- nom de capture limite au basename avec fallback en cas de chemin invalide ;
- capture opt-in differee de trois secondes pour laisser le portail test se
  placer ;
- ancien fallback de blit immediat decouple du chemin SubmitNode minimal.

Flags de developpement verifies :
- `IMM_PTL_AUTO_VISIBLE_TEST_PORTAL` : dev-only ;
- `IMM_PTL_CAPTURE_MINIMAL_RECURSIVE_PORTAL` : capture opt-in unique ;
- `IMM_PTL_MINIMAL_RECURSIVE_PORTAL_SCREENSHOT` : nom assaini ;
- `IMM_PTL_AUTO_MINIMAL_TRAVERSAL_TEST` : nouveau pilote dev-only reproductible
  de la commande de traversée existante.

La commande de test traversée avait son sens inverse : elle partait derriere le
portail vers la normale positive, alors que la forme rectangulaire accepte
`local z > 0` vers `local z < 0`. Le pilote de test part maintenant devant le
portail et se deplace contre la normale. Le gameplay de production n'est pas
modifie.

Audit GPU : un framebuffer secondaire singleton, un alias `AbstractTexture`
enregistre une fois, aucun `GpuBuffer` immediat ferme par le mod, et `MeshData`
ferme une fois sur le chemin de compatibilite. Aucune allocation persistante de
texture ou `RenderType` par frame n'a ete identifiee.

Rapport : `PHASE5.15_MINIMAL_RENDERER_STABILIZATION.md`.

### 5.16 Decision technique : clipping shader vanilla ou Phase 6

Statut : audit termine le 20 juin 2026.

Decision : **Option C, puis Option B**.

1. Figer le renderer minimal vanilla stable dans un commit de reference.
2. Ouvrir ensuite une Phase 6.0 Sodium seul, avec un profil dedie.
3. Attendre la stabilisation Sodium avant Iris.
4. Garder DimLib et AlternateDimensions isoles.

L'API 26.1 autorise des uniformes custom dans les pipelines controles par le
mod, mais elle ne permet pas d'ajouter globalement un plan de clipping aux
pipelines vanilla existants. Un pipeline dedie au quad final ne peut pas
decouper le contenu deja rendu dans la texture destination.

Un clipping general demanderait des variantes pour au moins le terrain, les
blocs et block entities, les entites, les particules, le ciel/nuages, la meteo
et les rendus speciaux. Cela revient a intercepter ou dupliquer une part
importante du rendu monde, puis a refaire ce travail pour Sodium et Iris.

La Phase 5.17 ne doit donc pas etre un chantier shader : elle sert au gel et au
commit de la baseline vanilla. La Phase 6.0 recommandee reactive ensuite Sodium
en compile-only, mixin par mixin, sans Iris ni DimLib. Iris attend Sodium ;
DimLib attend une base 26.1 compatible ou un port dedie.

Rapport : `PHASE5.16_RENDERING_DECISION_AUDIT.md`.

### 5.17 Gel technique et commit de reference vanilla

Statut : baseline preparee et validee le 20 juin 2026.

- aucun nouveau code de rendu ajoute ;
- renderer minimal, facades runtime et outils dev reproductibles selectionnes ;
- rapports uniques des phases 4.8 a 5.17 inclus ;
- copies, logs compile/run et diffs generes exclus ;
- suppressions historiques de logs laissees hors commit ;
- `compileJava` et `processResources` : BUILD SUCCESSFUL ;
- mixins shader, fog et clipping complets toujours isoles ;
- Sodium, Iris, DimLib et AlternateDimensions toujours inactifs.

La baseline conserve une recursion, le framebuffer secondaire, le pont
`SubmitNodeCollector`, le masque profondeur minimal et le cadre cyan fallback.
Ses limites documentees sont le clipping general incomplet, l'absence de
stencil public et le fog vanilla fallback.

Commit de reference : `Stabilize vanilla minimal portal renderer baseline`.

Rapport : `PHASE5.17_VANILLA_BASELINE_FREEZE.md`.

## Phase 6 - Compatibilites progressives

### 6.0 Profil Sodium compile-only separe

Statut : termine le 20 juin 2026.

- nouvelle propriete `enable_sodium_compat=false` par defaut ;
- profil vanilla par defaut inchange ;
- Sodium 0.8.7 ajoute uniquement en `compileOnly` avec
  `-Penable_sodium_compat=true` ;
- Iris absent du profil ;
- sept mixins Sodium non-shader compilables mais non actifs au runtime ;
- deux mixins de clipping shader toujours exclus ;
- premiere compilation Sodium : 2 erreurs API ;
- compilation Sodium finale : 0 erreur ;
- compilation vanilla finale : 0 erreur ;
- menu avec Sodium explicitement charge : atteint sans crash ;
- `imm_ptl_compat.mixins.json` toujours exclu des ressources runtime ;
- renderer minimal vanilla non modifie.

Corrections locales : `OcclusionCuller.Visitor` devient
`RenderSectionVisitor`, et `Camera.getPosition()` devient `Camera.position()`.

Le profil runtime de test exige les deux proprietes
`enable_sodium_compat=true` et `enable_sodium=true`. Sans le second flag, le
profil reste strictement compile-only.

La Phase 6.1 pourra activer les sept mixins par petits groupes. Les mixins
`MixinSodiumDefaultShaderInterface` et `MixinSodiumShaderLoader` restent hors
scope tant que le clipping shader n'est pas repris.

Rapport : `PHASE6.0_SODIUM_COMPILE_PROFILE.md`.

### 6.1 Activation runtime progressive des mixins Sodium non-shader

Statut : arrete proprement au Groupe A le 20 juin 2026.

- selecteur cumulatif runtime ajoute : `none`, `A`, `B`, `C` ;
- ressource compat generee avec uniquement les mixins Sodium selectionnes ;
- Groupe A actif : `IESodiumWorldRenderer` et
  `MixinSodiumFlawlessFrames` ;
- menu Groupe A : BUILD SUCCESSFUL ;
- monde Groupe A : joueur connecte puis crash FRAPI ;
- monde groupe `none` : crash FRAPI identique ;
- Groupes B et C non tentes ;
- portail Sodium non teste ;
- deux mixins shader Sodium toujours exclus ;
- Iris, DimLib et AlternateDimensions non reactives.

Blocage exact : Sodium 0.8.7 declare contenir un renderer Fabric API, ce qui
desactive Indigo, mais son `FRAPIProvider` utilise une implementation no-op.
`Renderer.get()` echoue alors au premier rendu d'objet tenu. Ce blocage est
anterieur aux mixins Immersive Portals et doit etre resolu par un alignement
Sodium/Fabric API ou un provider FRAPI Sodium compatible.

Rapport : `PHASE6.1_SODIUM_RUNTIME_MIXINS.md`.

### 6.2 Resolution du provider FRAPI Sodium

Statut : termine le 21 juin 2026.

- cause 0.8.7 confirmee : aucun service `FRAPIProvider` dans le jar ;
- Sodium 0.8.9 officiellement compatible Minecraft 26.1 ;
- service 0.8.9 present : `SodiumProvider` ;
- Fabric API 0.145.1 satisfait le minimum demande par Sodium 0.8.9 ;
- `sodium_path` migre vers `mc26.1.1-0.8.9-fabric` ;
- contrainte Sodium assouplie uniquement dans le profil compat ;
- monde `group=none` : charge, stable et ferme normalement ;
- monde Groupe A : charge, stable et ferme normalement ;
- crash FRAPI : 0 ;
- Groupes B/C et portail non testes ;
- Iris, DimLib et mixins shader toujours inactifs ;
- profil vanilla et renderer minimal inchanges.

Le prototype consistant a enregistrer Indigo manuellement reste retire : Indigo
desactive ses propres mixins en presence de `contains_renderer`, ce qui rend ce
fallback invalide. La correction correcte est le provider FRAPI fourni par
Sodium 0.8.9.

Rapport : `PHASE6.2_SODIUM_FRAPI_RENDERER_PROVIDER.md`.

### 6.3 Activation runtime des groupes Sodium B et C

Statut : termine le 21 juin 2026.

- Groupe B stable en monde avec quatre mixins non-shader ;
- signature `SodiumWorldRenderer.setupTerrain` alignee sur Sodium 0.8.9 ;
- redirection du frustum deplacee vers `Viewport.isBoxVisibleDirect` ;
- Groupe C stable avec les trois mixins de culling/regions supplementaires ;
- monde charge, joueur connecte, 15 secondes stables et fermeture normale ;
- crash FRAPI, erreur mixin, `ConcurrentModificationException`,
  `Buffer already closed` et `Duplicate entity UUID` : 0 ;
- portail de test cree et traversee client declenchee sous Groupe C ;
- rendu du portail non confirme sous Sodium : aucune collecte
  `PortalEntityRenderer`, aucun blit et aucune capture ;
- profil vanilla, Iris, DimLib, AlternateDimensions et mixins shader inchanges.

Le Groupe C devient la baseline Sodium non-shader. La suite doit raccorder le
chemin visuel des entites portail au renderer Sodium sans modifier le renderer
minimal vanilla.

Rapport : `PHASE6.3_SODIUM_RUNTIME_GROUPS_BC.md`.

### 6.4 Pont visuel minimal des portails sous Sodium

Statut : termine le 21 juin 2026.

- cause identifiee : deux filtres Sodium eliminaient les portails avant
  `PortalEntityRenderer` ;
- bypass limite aux entites `Portal` dans `EntityRenderer.shouldRender` ;
- bypass limite aux portails sans section terrain Sodium compilee ;
- Groupe C etendu a neuf mixins non-shader ;
- portail present cote client, extraction et soumission confirmees ;
- cadre cyan restaure sous Sodium ;
- collecte non reentrante et framebuffer minimal atteints ;
- masque profondeur et quad texture `SubmitNodeCollector` appliques ;
- capture native Sodium obtenue ;
- traversee Overworld vers Overworld revalidee ;
- facade de tracking serveur optionnelle securisee sans reactiver entity sync ;
- crash, `ConcurrentModificationException`, `Buffer already closed`,
  `Duplicate entity UUID` et `AbstractMethodError` finals : 0 ;
- vanilla, Iris, DimLib et mixins shader inchanges.

Les limites restent celles du renderer minimal : clipping incomplet, cadres
imbriques, une recursion et fog vanilla fallback.

Rapport : `PHASE6.4_SODIUM_PORTAL_VISUAL_BRIDGE.md`.

### 6.5 Gel de la baseline Sodium non-shader

Statut : termine le 21 juin 2026.

- Groupes A et B verifies inchanges ;
- Groupe C fige avec neuf mixins non-shader ;
- deux bypass portail limites strictement aux instances de `Portal` ;
- instrumentation redondante retiree, logs utiles gardes en occurrence unique ;
- vanilla `compileJava processResources` : BUILD SUCCESSFUL ;
- Sodium compile-only : BUILD SUCCESSFUL ;
- Groupe C dans `Phase49Test` : 30 secondes stables, fermeture normale ;
- portail Sodium : cadre cyan, framebuffer, masque profondeur et quad texture
  confirmes ;
- capture native de baseline obtenue ;
- crash, CME, buffer ferme, UUID duplique et `AbstractMethodError` sur les
  temoins finaux : 0 ;
- sauvegarde `Phase50Test` ecartee du temoin final car polluee par un ancien
  UUID de portail duplique issu des campagnes automatiques ;
- Iris, DimLib, AlternateDimensions et mixins shader toujours exclus.

Commit prepare : `Stabilize Sodium non-shader portal rendering baseline`.

Rapport : `PHASE6.5_SODIUM_BASELINE_FREEZE.md`.

### 7.0 Profil Iris compile-only separe

Statut : termine le 25 juin 2026.

- propriete `enable_iris_compat=false` ajoutee par defaut ;
- profil Iris compile-only active avec `-Penable_iris_compat=true` ;
- Iris 1.10.8 ajoute uniquement en `compileOnly` dans ce profil ;
- Sodium 0.8.9 ajoute aussi en `compileOnly` dans ce profil, car le mixin
  Iris/Sodium reference les interfaces shader Sodium ;
- aucun runtime Iris active ;
- aucun shaderpack charge ;
- `imm_ptl_compat.mixins.json` reste exclu des ressources tant que les mixins
  runtime ne sont pas explicitement actives ;
- sources Iris reintegrees en compilation compile-only ;
- renderers Iris legacy remplaces par des facades no-op compile-only :
  `ExperimentalIrisPortalRenderer`, `IrisPortalRenderer` et
  `IrisCompatibilityPortalRenderer` ;
- `IPIrisHelper` neutralise les copies couleur/stencil legacy et conserve
  seulement une copie profondeur publique minimale ;
- `MixinIrisTransformPatcher` migre de `Program.Type.VERTEX` vers
  `ShaderType.VERTEX` ;
- vanilla `compileJava processResources` : BUILD SUCCESSFUL ;
- Sodium compile-only : BUILD SUCCESSFUL ;
- Iris compile-only : BUILD SUCCESSFUL ;
- Iris runtime, DimLib, AlternateDimensions, shaderpack et mixins shader Sodium
  restent inactifs.

Limite volontaire : le rendu Iris runtime n'est pas restaure dans cette phase.
Les facades no-op servent uniquement a ouvrir une baseline de compilation Iris
mesurable avant les phases runtime.

Rapport : `PHASE7.0_IRIS_COMPILE_PROFILE.md`.

### 7.1 Runtime Iris minimal sans shaderpack

Statut : termine le 25 juin 2026.

- `enable_iris=true` devient effectif seulement avec
  `enable_iris_compat=true` ;
- Iris runtime ajoute uniquement dans le profil runtime Iris ;
- blocage initial documente : Iris 1.10.8 exige Sodium `0.8.x` ;
- `sodium_path` 0.8.9 de la baseline Sodium reste inchange ;
- ajout d'un `iris_sodium_path` separe vers Sodium 0.8.7 pour le runtime Iris
  menu-only, car Sodium 0.8.9 declare casser Iris `<=1.10.8` ;
- shaderpack absent : dossier `run/shaderpacks` absent ;
- `imm_ptl_compat.mixins.json` reste exclu des ressources runtime Iris ;
- mixins Iris Immersive Portals actifs au runtime : aucun ;
- Iris charge ses propres mixins et atteint le menu ;
- log Iris confirme : `Shaders are disabled because no valid shaderpack is selected` ;
- vanilla `compileJava processResources` : BUILD SUCCESSFUL ;
- Sodium compile-only : BUILD SUCCESSFUL ;
- Iris compile-only : BUILD SUCCESSFUL ;
- Iris runtime menu : BUILD SUCCESSFUL, fermeture normale ;
- aucun monde lance dans cette phase ;
- DimLib, AlternateDimensions, shaderpack, renderer Iris avance et mixins shader
  Sodium restent inactifs.

Limite volontaire : ce profil runtime utilise Sodium 0.8.7 uniquement pour
satisfaire Iris 1.10.8 au menu. La baseline Sodium non-shader reste sur 0.8.9.
Le test monde Iris est reporte a 7.2.

Rapport : `PHASE7.1_IRIS_RUNTIME_MENU.md`.

### 7.2 Runtime Iris monde sans shaderpack

Statut : termine le 25 juin 2026.

- vanilla `compileJava processResources` : BUILD SUCCESSFUL ;
- Sodium compile-only : BUILD SUCCESSFUL ;
- Iris compile-only : BUILD SUCCESSFUL ;
- Iris runtime menu sans shaderpack : BUILD SUCCESSFUL ;
- Iris runtime monde sans shaderpack : BUILD SUCCESSFUL ;
- monde temoin : `Phase72IrisNoShaderTest` ;
- `Phase50Test` non utilise comme temoin final ;
- Iris charge au runtime avec Sodium `0.8.7+mc26.1`, version separee requise par
  Iris `1.10.8+mc26.1` ;
- shaderpack charge : aucun ;
- log Iris confirme : `Shaders are disabled because no valid shaderpack is selected` ;
- `imm_ptl_compat.mixins.json` reste exclu des ressources runtime Iris ;
- mixins Iris Immersive Portals actifs au runtime : aucun ;
- renderer Iris avance toujours no-op/non restaure ;
- blocage monde initial corrige : Sodium 0.8.7 declare un renderer FRAPI mais
  n'enregistre aucun provider, ce qui faisait crasher `Renderer.get()` pendant
  le rendu de la main/blocs ;
- ajout d'un provider FRAPI fallback minimal limite a Iris + Sodium en
  environnement de developpement, uniquement si aucun provider Fabric Renderer
  API n'est deja actif ;
- joueur connecte, monde stable plus de 20 secondes, fermeture normale ;
- `UnsupportedOperationException`, crash, `Duplicate entity UUID`,
  `ConcurrentModificationException`, `Buffer already closed` et
  `Mixin apply failed` finals : 0 ;
- DimLib, AlternateDimensions, shaderpack, mixins shader Sodium et clipping
  shader restent inactifs.

Limite volontaire : le fallback FRAPI est une rustine de compatibilite pour
Iris/Sodium 0.8.7 sans shaderpack. Il ne remplace pas le provider Sodium 0.8.9
de la baseline Sodium et ne restaure pas le rendu Iris avance.

Rapport : `PHASE7.2_IRIS_RUNTIME_WORLD_NO_SHADERPACK.md`.

### 7.3 Test portail Iris runtime sans shaderpack

Statut : termine le 26 juin 2026.

- vanilla `compileJava processResources` : BUILD SUCCESSFUL ;
- Sodium compile-only : BUILD SUCCESSFUL ;
- Iris compile-only : BUILD SUCCESSFUL ;
- Iris runtime monde sans shaderpack : BUILD SUCCESSFUL ;
- Iris runtime portail sans shaderpack : BUILD SUCCESSFUL ;
- Iris `1.10.8+mc26.1` charge ;
- Sodium runtime Iris `0.8.7+mc26.1` charge ;
- fallback FRAPI Iris/Sodium 0.8.7 enregistre ;
- shaderpack charge : aucun ;
- monde temoin : `Phase72IrisNoShaderTest` ;
- portail cree via flag dev : oui ;
- portail present cote client : oui ;
- traversee declenchee : oui ;
- `Client Teleported Statically` observe ;
- crash, `UnsupportedOperationException`, `Duplicate entity UUID`,
  `ConcurrentModificationException`, `Buffer already closed` et erreur mixin : 0 ;
- DimLib, AlternateDimensions, renderer Iris avance et mixins shader Sodium
  restent inactifs.

Blocage visuel identifie : le portail n'atteint pas `PortalEntityRenderer` sous
Iris/Sodium 0.8.7. `imm_ptl_compat.mixins.json` reste exclu dans ce profil, donc
les deux bypass portail du Groupe C Sodium ne sont pas actifs :
`MixinSodiumPortalEntityRenderer` et `MixinSodiumPortalLevelRenderer`. Le portail
est synchronise et traversable, mais filtre avant la soumission du renderer ;
aucune capture framebuffer n'est donc produite.

Suite recommandee : ouvrir une phase 7.4 mini-groupe Iris/Sodium 0.8.7 limite
aux deux bypass `Portal`, sans activer tout le Groupe C, sans shaderpack, sans
renderer Iris avance et sans mixins shader Sodium.

Rapport : `PHASE7.3_IRIS_PORTAL_NO_SHADERPACK.md`.

### 7.4 Mini-groupe Iris/Sodium 0.8.7 limite aux bypass Portal

Statut : termine le 26 juin 2026.

- ajout d'un mini-groupe runtime Iris/Sodium limite a deux mixins :
  `sodium.MixinSodiumPortalEntityRenderer` et
  `sodium.MixinSodiumPortalLevelRenderer` ;
- `imm_ptl_compat.mixins.json` est genere dans le profil runtime Iris avec
  exactement ces deux mixins ;
- Groupe C Sodium complet non active dans le profil Iris ;
- mixins shader Sodium toujours inactifs ;
- baseline Sodium inchangee : `sodium_path` reste sur Sodium 0.8.9 ;
- runtime Iris inchange : `iris_sodium_path` reste sur Sodium 0.8.7 ;
- vanilla `compileJava processResources` : BUILD SUCCESSFUL ;
- Sodium compile-only : BUILD SUCCESSFUL ;
- Iris compile-only : BUILD SUCCESSFUL ;
- Iris runtime monde sans shaderpack : BUILD SUCCESSFUL ;
- Iris runtime portail sans shaderpack : BUILD SUCCESSFUL ;
- fallback FRAPI Iris/Sodium 0.8.7 enregistre ;
- shaderpack charge : aucun ;
- bypass `EntityRenderer.shouldRender` actif pour `Portal` : oui ;
- bypass `LevelRenderer.isSectionCompiledAndVisible` actif pour `Portal` : oui ;
- `PortalEntityRenderer.submit` atteint sous Iris/Sodium 0.8.7 ;
- cadre cyan visible ;
- framebuffer minimal atteint ;
- texture framebuffer disponible et quad texture soumis via `SubmitNodeCollector` ;
- masque profondeur minimal applique ;
- capture native obtenue :
  `run/screenshots/phase7.4-iris-portal-bypass.png` ;
- traversee revalidee avec `Client Teleported Statically` ;
- crash, `UnsupportedOperationException`, `Duplicate entity UUID`,
  `ConcurrentModificationException`, `Buffer already closed` et erreur mixin : 0 ;
- DimLib, AlternateDimensions, shaderpack, renderer Iris avance et clipping
  shader restent inactifs.

Limite volontaire : le rendu reste le renderer minimal existant, avec clipping
incomplet, une recursion et fog fallback. Aucun shaderpack ni chemin Iris avance
n'est restaure dans cette phase.

Rapport : `PHASE7.4_IRIS_SODIUM_PORTAL_BYPASS.md`.

### 7.5 Gel baseline Iris runtime sans shaderpack

Statut : termine le 26 juin 2026.

- baseline Iris runtime sans shaderpack figee ;
- Iris runtime : `1.10.8+mc26.1` ;
- Sodium runtime Iris : `0.8.7+mc26.1` via `iris_sodium_path` separe ;
- baseline Sodium conservee sur `sodium_path` Sodium 0.8.9 ;
- `enable_iris=false` et `enable_iris_compat=false` restent les valeurs par
  defaut ;
- fallback FRAPI Iris/Sodium 0.8.7 confirme limite au dev, a Iris + Sodium et
  seulement lorsqu'aucun provider Fabric Renderer API n'est deja actif ;
- mini-groupe Iris/Sodium runtime confirme avec exactement :
  `sodium.MixinSodiumPortalEntityRenderer` et
  `sodium.MixinSodiumPortalLevelRenderer` ;
- Groupe C complet non active dans le profil Iris ;
- mixins shader Sodium inactifs ;
- renderer Iris avance toujours no-op ;
- DimLib et AlternateDimensions inactifs ;
- vanilla `compileJava processResources` : BUILD SUCCESSFUL ;
- Sodium compile-only : BUILD SUCCESSFUL ;
- Iris compile-only : BUILD SUCCESSFUL ;
- `processResources` Iris runtime : BUILD SUCCESSFUL et JSON avec exactement
  deux mixins `Portal` ;
- Iris runtime monde sans shaderpack : BUILD SUCCESSFUL ;
- Iris runtime portail sans shaderpack : BUILD SUCCESSFUL ;
- `PortalEntityRenderer.submit` atteint ;
- cadre cyan visible ;
- framebuffer minimal, texture framebuffer et quad `SubmitNodeCollector`
  confirmes ;
- capture obtenue :
  `run/screenshots/phase7.5-iris-no-shaderpack-baseline.png` ;
- traversee revalidee avec `Client Teleported Statically` ;
- crash, `UnsupportedOperationException`, `Duplicate entity UUID`,
  `ConcurrentModificationException`, `Buffer already closed` et erreur mixin : 0.

Limites conservees : pas de shaderpack, pas de renderer Iris avance, clipping
general incomplet, fog vanilla fallback et une seule recursion.

Commit prevu : `Stabilize Iris no-shaderpack portal rendering baseline`.

Rapport : `PHASE7.5_IRIS_NO_SHADERPACK_BASELINE_FREEZE.md`.

### 8.0 Premier test Iris avec shaderpack, sans portail

Statut : partiellement valide le 28 juin 2026, bloque avant runtime shaderpack
car aucun shaderpack local n'est disponible.

- baseline Iris no-shaderpack confirmee :
  `bd3a080c Stabilize Iris no-shaderpack portal rendering baseline` ;
- vanilla `compileJava processResources` : BUILD SUCCESSFUL ;
- Sodium compile-only : BUILD SUCCESSFUL ;
- Iris compile-only : BUILD SUCCESSFUL ;
- Iris runtime monde sans shaderpack :
  `Phase72IrisNoShaderTest` BUILD SUCCESSFUL ;
- Iris charge : `1.10.8+mc26.1` ;
- Sodium runtime Iris charge : `0.8.7+mc26.1` ;
- fallback FRAPI Iris/Sodium 0.8.7 enregistre ;
- log Iris confirme : `Shaders are disabled because no valid shaderpack is selected` ;
- crash, erreur mixin bloquante, `Duplicate entity UUID`,
  `ConcurrentModificationException` et `Buffer already closed` : 0 ;
- flags dev portail/capture explicitement retires du lancement ;
- aucun portail cree ou teste dans cette phase ;
- `run/shaderpacks` existe mais contient `0` shaderpack ;
- aucun shaderpack n'a ete telecharge automatiquement ;
- test runtime shaderpack non lance, conformement a la consigne.

Blocage exact : fournir un shaderpack local dans `run/shaderpacks` avant de
reprendre le smoke test Iris avec shaderpack.

Rapport : `PHASE8.0_IRIS_SHADERPACK_WORLD_SMOKE_TEST.md`.

### 8.0B Reprise test Iris avec shaderpack local, sans portail

Statut : bloque le 28 juin 2026 avant runtime shaderpack.

- baseline Iris no-shaderpack confirmee :
  `bd3a080c Stabilize Iris no-shaderpack portal rendering baseline` ;
- verification `run/shaderpacks` : `COUNT=0` ;
- aucun shaderpack `.zip` local disponible ;
- aucun shaderpack telecharge automatiquement ;
- aucune configuration Iris shaderpack modifiee ;
- aucun monde shaderpack lance ;
- aucun portail cree ou teste ;
- variables dev portail/capture absentes de l'environnement courant.

Blocage exact : placer manuellement un seul shaderpack `.zip` dans
`run/shaderpacks/`, puis relancer la Phase 8.0B.

Rapport : `PHASE8.0B_IRIS_SHADERPACK_WORLD_SMOKE_TEST.md`.

### 8.0C Test Iris avec shaderpack local, sans portail

Statut : termine le 28 juin 2026.

- shaderpack local detecte :
  `run/shaderpacks/MakeUp-UltraFast-9.5c.zip` ;
- nombre de shaderpacks `.zip` : 1 ;
- activation controlee via `run/config/iris.properties` :
  `shaderPack=MakeUp-UltraFast-9.5c.zip` ;
- vanilla `compileJava processResources` : BUILD SUCCESSFUL ;
- Sodium compile-only : BUILD SUCCESSFUL ;
- Iris compile-only : BUILD SUCCESSFUL ;
- monde temoin separe : `Phase80IrisShaderpackWorldTest` ;
- le temoin a ete prepare depuis `Phase72IrisNoShaderTest`, pas depuis
  `Phase50Test` ;
- les donnees d'entites du temoin ont ete nettoyees avant le run final pour
  garantir un test sans portail ;
- Iris runtime shaderpack : BUILD SUCCESSFUL ;
- Iris charge : `1.10.8+mc26.1` ;
- Sodium runtime Iris charge : `0.8.7+mc26.1` ;
- fallback FRAPI Iris/Sodium 0.8.7 enregistre ;
- log Iris confirme :
  `Using shaderpack: MakeUp-UltraFast-9.5c.zip` ;
- pipeline Iris cree pour `minecraft:overworld` ;
- joueur connecte ;
- stabilite apres connexion : environ 23 secondes ;
- fermeture normale ;
- crash, erreur mixin bloquante, `UnsupportedOperationException`,
  `Duplicate entity UUID`, `ConcurrentModificationException` et
  `Buffer already closed` : 0 ;
- `PortalEntityRenderer` dans le run final : 0 ;
- aucun portail cree ;
- aucune traversee testee.

Notes : le shaderpack emet des warnings Iris sur des entrees
`betterendforge::*`, non bloquants. `IPModInfoChecking` 404 et Realms auth
restent non bloquants.

Rapport : `PHASE8.0C_IRIS_SHADERPACK_WORLD_SMOKE_TEST.md`.

### 8.1 Test portail Iris avec shaderpack actif

Statut : termine le 28 juin 2026 avec blocage visuel documente.

- shaderpack actif : `MakeUp-UltraFast-9.5c.zip` ;
- Iris selectionne toujours le shaderpack via `run/config/iris.properties` ;
- vanilla `compileJava processResources` : BUILD SUCCESSFUL ;
- Sodium compile-only : BUILD SUCCESSFUL ;
- Iris compile-only : BUILD SUCCESSFUL ;
- monde temoin separe : `Phase81IrisShaderpackPortalTest` ;
- le temoin a ete prepare depuis `Phase80IrisShaderpackWorldTest`, pas depuis
  `Phase50Test` ;
- les donnees d'entites du temoin ont ete nettoyees avant le run pour eviter
  les anciens portails ;
- Iris runtime shaderpack + portail : BUILD SUCCESSFUL ;
- Iris charge : `1.10.8+mc26.1` ;
- Sodium runtime Iris charge : `0.8.7+mc26.1` ;
- fallback FRAPI Iris/Sodium 0.8.7 enregistre ;
- log Iris confirme :
  `Using shaderpack: MakeUp-UltraFast-9.5c.zip` ;
- pipeline Iris cree pour `minecraft:overworld` ;
- portail cree par flag dev ;
- portail present cote client ;
- bypass `EntityRenderer.shouldRender` pour `Portal` actif ;
- bypass `LevelRenderer.isSectionCompiledAndVisible` pour `Portal` actif ;
- `PortalEntityRenderer.submit` appele ;
- framebuffer minimal atteint ;
- texture framebuffer disponible : `854x480` ;
- quad texture `SubmitNodeCollector` soumis ;
- capture obtenue :
  `run/screenshots/phase8.1-iris-shaderpack-portal.png` ;
- traversee declenchee ;
- `Client Teleported Statically` observe ;
- fermeture normale ;
- crash, erreur mixin bloquante, `UnsupportedOperationException`,
  `Duplicate entity UUID`, `ConcurrentModificationException` et
  `Buffer already closed` : 0.

Blocage visuel restant : Iris avec shaderpack signale deux erreurs non fatales
sur les pipelines minimaux Immersive Portals :
`minecraft:pipeline/imm_ptl_portal_depth_mask` et
`minecraft:pipeline/imm_ptl_draw_framebuffer_in_area_depth_masked` absents de
la liste d'overrides. Le cadre cyan est visible et le portail est traversable,
mais la texture destination n'est pas clairement lisible dans la capture.

Rapport : `PHASE8.1_IRIS_SHADERPACK_PORTAL_TEST.md`.

### 8.2 Audit overrides Iris pour pipelines Immersive Portals minimaux

Statut : termine le 28 juin 2026.

- cause exacte identifiee : Iris redirige les `RenderPipeline` via
  `MixinShaderManager_Overrides.redirectIrisProgram`, puis consulte
  `IrisPipelines.coreShaderMap` ;
- les pipelines custom Immersive Portals n'etaient pas dans cette table ;
- `IrisPipelines.getPipeline(...)` retournait donc `null` ;
- Iris loggait ensuite `Missing program ... in override list` ;
- le namespace `minecraft` de nos locations `pipeline/...` rendait le log plus
  severe, mais n'etait pas la cause racine ;
- correctif minimal applique dans `IPRenderPipelines` par reflexion, sans
  dependance directe Iris :
  - `imm_ptl_portal_depth_mask` -> `ShaderKey.BASIC_COLOR` ;
  - `imm_ptl_draw_framebuffer_in_area_depth_masked` -> `ShaderKey.TEXTURED` ;
  - `imm_ptl_draw_framebuffer_in_area` -> `ShaderKey.TEXTURED` ;
- aucun renderer Iris avance restaure ;
- aucun shader clipping restaure ;
- aucun mixin shader Sodium active ;
- DimLib et AlternateDimensions toujours inactifs ;
- vanilla `compileJava processResources` : BUILD SUCCESSFUL ;
- Sodium compile-only : BUILD SUCCESSFUL ;
- Iris compile-only : BUILD SUCCESSFUL ;
- runtime Iris + shaderpack + portail : BUILD SUCCESSFUL ;
- shaderpack actif : `MakeUp-UltraFast-9.5c.zip` ;
- mapping Iris enregistre en runtime ;
- portail cree ;
- portail present cote client ;
- `PortalEntityRenderer.submit` appele ;
- framebuffer minimal atteint ;
- texture framebuffer disponible : `854x480` ;
- quad `SubmitNodeCollector` soumis ;
- erreurs `Missing program` : 0 dans le run final ;
- capture obtenue :
  `run/screenshots/phase8.2-iris-pipeline-override-audit.png` ;
- traversee declenchee ;
- `Client Teleported Statically` observe ;
- fermeture normale ;
- crash, erreur mixin bloquante, `UnsupportedOperationException`,
  `Duplicate entity UUID`, `ConcurrentModificationException` et
  `Buffer already closed` : 0.

Limite restante : la capture automatique Phase 8.2 reste tres sombre et n'est
pas concluante. Elle ne prouve pas que le portail est invisible : une
observation interactive apres rotation de la camera a permis de voir le portail.
Le blocage d'override Iris est corrige, mais l'audit visuel shaderpack doit
continuer dans une phase separee.

Rapport : `PHASE8.2_IRIS_PIPELINE_OVERRIDE_AUDIT.md`.

### 8.3 Validation visuelle Iris avec shaderpack

Etat :

- vanilla compile : BUILD SUCCESSFUL ;
- Sodium compile-only : BUILD SUCCESSFUL ;
- Iris compile-only : BUILD SUCCESSFUL ;
- runtime Iris + shaderpack + portail lance sur `Phase81IrisShaderpackPortalTest` ;
- shaderpack actif : `MakeUp-UltraFast-9.5c.zip` ;
- mapping Iris fallback enregistre ;
- `PortalEntityRenderer.submit` appele ;
- framebuffer minimal atteint ;
- texture framebuffer disponible : `854x480` ;
- depth mask applique ;
- quad texture `SubmitNodeCollector` soumis ;
- portail present cote client sous Sodium/Iris ;
- capture automatique obtenue :
  `run/screenshots/phase8.3-iris-shaderpack-visual-validation.png` ;
- capture automatique concluante : non, image encore trop sombre ;
- observation interactive precedente apres rotation camera : portail vu ;
- traversee observee dans le run visuel : `Client Teleported Statically` ;
- erreurs `Missing program` : 0 ;
- crash, `Duplicate entity UUID`, `ConcurrentModificationException`,
  `Buffer already closed` et `UnsupportedOperationException` : 0.

Conclusion : le correctif Phase 8.2 reste valide et le portail fonctionne sous
Iris avec shaderpack. La preuve automatique reste insuffisante : la capture est
trop sombre et doit etre amelioree ou remplacee par une capture F2 manuelle
fiable. Aucun changement renderer n'a ete effectue pendant cette phase.

Rapport : `PHASE8.3_IRIS_SHADERPACK_VISUAL_VALIDATION.md`.

### 8.4 Preuve visuelle manuelle Iris avec shaderpack

Etat :

- aucun changement de renderer ;
- aucun changement des mappings Iris Phase 8.2 ;
- aucun changement de pipeline ;
- shader clipping, Sodium shader mixins, DimLib, AlternateDimensions et renderer
  Iris avance toujours inactifs ;
- run final Iris + shaderpack + portail : BUILD SUCCESSFUL ;
- shaderpack actif : `MakeUp-UltraFast-9.5c.zip` ;
- mapping Iris fallback enregistre ;
- `PortalEntityRenderer.submit` appele ;
- framebuffer minimal atteint ;
- texture framebuffer disponible : `854x480` ;
- depth mask applique ;
- quad texture `SubmitNodeCollector` soumis ;
- portail cree par commande dev ;
- portail present cote client sous Sodium/Iris ;
- erreurs `Missing program` : 0 ;
- crash, `Duplicate entity UUID`, `ConcurrentModificationException`,
  `Buffer already closed` et `UnsupportedOperationException` : 0.

Captures :

- captures F2 locales obtenues :
  `run/screenshots/2026-06-28_20.01.16.png` et
  `run/screenshots/2026-06-28_20.03.33.png` ;
- ces captures locales ne cadrent pas clairement le portail ;
- preuve visuelle fiable : capture manuelle fournie dans le chat pendant la
  Phase 8.4, montrant le cadre cyan et la texture framebuffer/shaderpack dans le
  portail.

Conclusion : le portail Iris avec shaderpack est visible en observation
interactive. La capture automatique reste imparfaite, mais le probleme n'est
plus classe comme invisibilite du portail.

Rapport : `PHASE8.4_IRIS_SHADERPACK_MANUAL_VISUAL_PROOF.md`.

### 8.6 Test deuxieme shaderpack Iris avec portail minimal

Shaderpack teste :

- `ComplementaryReimagined_r5.8.1.zip`

Etat :

- baseline de depart : `8c09eb37 Stabilize Iris shaderpack minimal portal rendering` ;
- `run/config/iris.properties` bascule temporairement sur Complementary, puis
  restaure sur `MakeUp-UltraFast-9.5c.zip` ;
- vanilla compile : BUILD SUCCESSFUL ;
- Sodium compile-only : BUILD SUCCESSFUL ;
- Iris compile-only : BUILD SUCCESSFUL ;
- monde de test : `Phase86IrisSecondShaderpackPortalTest`, copie nettoyee des
  dossiers `entities` ;
- Iris charge avec Complementary ;
- mapping Iris fallback enregistre ;
- erreurs `Missing program` : 0 ;
- portail cree ;
- portail present cote client ;
- `PortalEntityRenderer.submit` appele ;
- framebuffer minimal atteint ;
- texture framebuffer disponible : `854x480` ;
- depth mask applique ;
- quad texture `SubmitNodeCollector` soumis ;
- traversee confirmee : `Client Teleported Statically` ;
- crash, `Duplicate entity UUID`, `ConcurrentModificationException`,
  `Buffer already closed` et `UnsupportedOperationException` : 0.

Observation visuelle : compatibilite partielle. Le cadre cyan est visible, mais
le rendu framebuffer/shaderpack clignote et peut etre invisible au moment de la
capture. Complementary n'est donc pas promu nouvelle baseline visuelle. MakeUp
reste la baseline shaderpack validee.

Rapport : `PHASE8.6_IRIS_SECOND_SHADERPACK_PORTAL_TEST.md`.

### 9.0 Audit clignotement framebuffer Iris shaderpack

Objectif :

Auditer le clignotement du contenu framebuffer sous certains shaderpacks, sans
modifier le renderer ni restaurer les chemins Iris avances.

Etat :

- baseline importante : `8c09eb37 Stabilize Iris shaderpack minimal portal rendering` ;
- `run/config/iris.properties` verifie puis restaure sur
  `MakeUp-UltraFast-9.5c.zip` ;
- aucun changement de code ;
- shader clipping, Sodium shader mixins, DimLib, AlternateDimensions et renderer
  Iris avance toujours inactifs.

Tests :

- MakeUp :
  - monde `Phase90IrisShaderpackFramebufferAuditMakeUp` ;
  - shaderpack actif : `MakeUp-UltraFast-9.5c.zip` ;
  - mapping Iris fallback enregistre ;
  - `Missing program` : 0 ;
  - framebuffer disponible : `854x480` ;
  - depth mask applique ;
  - quad texture `SubmitNodeCollector` soumis ;
  - portail present cote client ;
  - crash/CME/buffer ferme/UUID duplique/UOE : 0.
- Complementary :
  - monde `Phase90IrisShaderpackFramebufferAuditComplementary` ;
  - shaderpack actif : `ComplementaryReimagined_r5.8.1.zip` ;
  - mapping Iris fallback enregistre ;
  - `Missing program` : 0 ;
  - framebuffer disponible : `854x480` ;
  - depth mask applique ;
  - quad texture `SubmitNodeCollector` soumis ;
  - portail present cote client ;
  - cadre cyan visible ;
  - contenu framebuffer intermittent/clignotant ;
  - crash/CME/buffer ferme/UUID duplique/UOE : 0.

Conclusion : le probleme n'est pas une absence de texture ni une erreur
d'override Iris. Les logs MakeUp et Complementary suivent le meme chemin
Immersive Portals. La cause probable est une interaction shaderpack specifique
entre le depth mask minimal `CompareOp.EQUAL`, le fallback `ShaderKey.TEXTURED`
et les passes deferred/temporales/post-process de Complementary.

Recommandation Phase 9.1 : micro-experience unique et reversible pour forcer le
quad framebuffer sans depth mask afin de confirmer ou non la piste profondeur.

Rapport : `PHASE9.0_IRIS_SHADERPACK_FRAMEBUFFER_FLICKER_AUDIT.md`.

### 9.1 Micro-experience framebuffer sans depth mask

Objectif :

Tester si le clignotement Complementary vient principalement du depth mask
minimal `CompareOp.EQUAL`.

Modification :

- ajout du flag dev `IMM_PTL_FORCE_FRAMEBUFFER_NO_DEPTH_MASK=true` ;
- portee limitee a `RendererUsingFrameBuffer` ;
- comportement par defaut inchange quand le flag est absent ;
- avec le flag : utilisation de `DRAW_FRAMEBUFFER_IN_AREA` et saut de la passe
  `PORTAL_DEPTH_MASK` / `DRAW_FRAMEBUFFER_IN_AREA_DEPTH_MASKED`.

Validation :

- vanilla compile : BUILD SUCCESSFUL ;
- Sodium compile-only : BUILD SUCCESSFUL ;
- Iris compile-only : BUILD SUCCESSFUL.

Runs :

- MakeUp depth baseline :
  - shaderpack : `MakeUp-UltraFast-9.5c.zip` ;
  - flag : false ;
  - pipeline : `depth-masked` ;
  - framebuffer : `854x480` ;
  - depth mask applique ;
  - quad soumis ;
  - BUILD SUCCESSFUL.
- MakeUp no-depth-mask :
  - shaderpack : `MakeUp-UltraFast-9.5c.zip` ;
  - flag : true ;
  - pipeline : `non-depth-masked` ;
  - framebuffer : `854x480` ;
  - quad soumis ;
  - BUILD SUCCESSFUL.
- Complementary depth baseline :
  - shaderpack : `ComplementaryReimagined_r5.8.1.zip` ;
  - flag : false ;
  - pipeline : `depth-masked` ;
  - framebuffer : `854x480` ;
  - clignotement/intermittence deja reproduits ;
  - BUILD SUCCESSFUL.
- Complementary no-depth-mask :
  - shaderpack : `ComplementaryReimagined_r5.8.1.zip` ;
  - flag : true ;
  - pipeline : `non-depth-masked` ;
  - framebuffer : `854x480` ;
  - capture F2 `run/screenshots/2026-06-28_20.52.46.png` montrant du contenu
    framebuffer visible ;
  - BUILD SUCCESSFUL.

Conclusion : la piste depth mask / `CompareOp.EQUAL` est fortement confirmee.
Le mode sans depth mask n'est pas un rendu final, mais il reduit le symptome
principal sous Complementary et isole le probleme autour de la comparaison de
profondeur shaderpack.

Rapport : `PHASE9.1_IRIS_FRAMEBUFFER_NO_DEPTH_MASK_EXPERIMENT.md`.

### 9.2 Conception d'un chemin framebuffer shaderpack-safe

Objectif :

Transformer l'experience Phase 9.1 en modes de profondeur explicites pour
comparer proprement les shaderpacks Iris sans toucher au renderer avance.

Modification :

- ajout de `IMM_PTL_FRAMEBUFFER_DEPTH_MODE=<mode>` ;
- modes disponibles :
  - `default` : comportement actuel, depth mask + textured pass `EQUAL` ;
  - `no_depth` : textured pass sans masque profondeur ;
  - `lequal` : depth mask + textured pass `LESS_THAN_OR_EQUAL` ;
  - `always` : depth mask + textured pass `ALWAYS_PASS` ;
- `IMM_PTL_FORCE_FRAMEBUFFER_NO_DEPTH_MASK=true` reste accepte comme alias
  historique de `no_depth` si le nouveau mode n'est pas defini ;
- comportement par defaut inchange.

Validation compilation :

- vanilla : BUILD SUCCESSFUL ;
- Sodium compile-only : BUILD SUCCESSFUL ;
- Iris compile-only : BUILD SUCCESSFUL.

Runs :

- MakeUp `default` :
  - shaderpack : `MakeUp-UltraFast-9.5c.zip` ;
  - pipeline : `depth-masked-equal` ;
  - portail client present ;
  - quad framebuffer soumis.
- Complementary `default` :
  - shaderpack : `ComplementaryReimagined_r5.8.1.zip` ;
  - pipeline : `depth-masked-equal` ;
  - portail client present ;
  - quad framebuffer soumis.
- Complementary `no_depth` :
  - pipeline : `non-depth-masked` ;
  - portail client present ;
  - quad framebuffer soumis.
- Complementary `lequal` :
  - pipeline : `depth-masked-lequal` ;
  - portail client present ;
  - quad framebuffer soumis.
- Complementary `always` :
  - pipeline : `depth-masked-always` ;
  - portail client present ;
  - quad framebuffer soumis.

Resultat :

- les cinq chemins runtime atteignent le portail sans crash ;
- `Buffer already closed` : 0 ;
- `ConcurrentModificationException` : 0 ;
- `Duplicate entity UUID` : 0 ;
- `run/config/iris.properties` restaure sur `MakeUp-UltraFast-9.5c.zip` ;
- Sodium shader mixins, Iris renderer avance, DimLib, AlternateDimensions,
  shader clipping et stencil avance restent inactifs.

Conclusion :

`no_depth` reste le fallback le plus robuste mais degrade l'occlusion minimale.
`lequal` est le meilleur candidat shaderpack-safe a valider visuellement, car il
conserve le masque profondeur tout en evitant la comparaison stricte `EQUAL`.
`always` reste un mode de diagnostic permissif.

Rapport : `PHASE9.2_IRIS_SHADERPACK_SAFE_DEPTH_MODES.md`.

### 9.3 Validation visuelle du mode LEQUAL shaderpack-safe

Objectif :

Valider visuellement si `IMM_PTL_FRAMEBUFFER_DEPTH_MODE=lequal` peut devenir le
candidat shaderpack-safe principal pour Iris + Complementary, sans changer le
mode par defaut global.

Etat :

- aucun changement de code renderer ;
- `run/config/iris.properties` verifie au depart et restaure a la fin sur
  `MakeUp-UltraFast-9.5c.zip` ;
- Sodium shader mixins, Iris renderer avance, shader clipping, DimLib et
  AlternateDimensions restent inactifs.

Mondes prepares :

- `Phase93MakeUpDefaultVisual` ;
- `Phase93ComplementaryDefaultVisual` ;
- `Phase93ComplementaryLequalVisual` ;
- `Phase93ComplementaryNoDepthVisual`.

Runs :

- MakeUp `default` :
  - shaderpack : `MakeUp-UltraFast-9.5c.zip` ;
  - pipeline : `depth-masked-equal` ;
  - framebuffer : `854x480` ;
  - quad soumis ;
  - portail present cote client.
- Complementary `default` :
  - pipeline : `depth-masked-equal` ;
  - framebuffer : `854x480` ;
  - quad soumis ;
  - portail present cote client.
- Complementary `lequal` :
  - pipeline : `depth-masked-lequal` ;
  - framebuffer : `854x480` ;
  - quad soumis ;
  - portail present cote client.
- Complementary `no_depth` :
  - pipeline : `non-depth-masked` ;
  - framebuffer : `854x480` ;
  - quad soumis ;
  - portail present cote client.

Erreurs runtime :

- `Buffer already closed` : 0 ;
- `ConcurrentModificationException` : 0 ;
- `Duplicate entity UUID` : 0 ;
- `UnsupportedOperationException` : 0 ;
- `Missing program` : 0.

Captures :

- captures F2 obtenues pour Complementary `default` et `lequal`, mais elles sont
  non concluantes car prises sur `Loading terrain...` ;
- tentatives de capture Windows non retenues comme preuve visuelle : une capture
  a pris une autre fenetre, l'autre a pris un onglet terminal intitule par le
  run au lieu de la surface Minecraft.

Conclusion :

`lequal` est valide techniquement au runtime et reste le meilleur candidat
shaderpack-safe, mais la preuve visuelle de stabilisation du clignotement sous
Complementary manque encore. Ne pas promouvoir `lequal` comme defaut global
avant une validation manuelle fiable de la fenetre Minecraft au premier plan.
`no_depth` reste le fallback robuste connu.

Rapport : `PHASE9.3_IRIS_LEQUAL_VISUAL_VALIDATION.md`.

### 9.3B Preuve visuelle manuelle LEQUAL sous Complementary

Objectif :

Obtenir une preuve visuelle manuelle fiable du portail sous Complementary avec
`IMM_PTL_FRAMEBUFFER_DEPTH_MODE=lequal`.

Run :

- monde : `Phase93ComplementaryLequalVisual` ;
- shaderpack : `ComplementaryReimagined_r5.8.1.zip` ;
- mode : `lequal` ;
- pipeline : `depth-masked-lequal` ;
- framebuffer : `854x480` ;
- quad SubmitNodeCollector soumis ;
- portail present cote client.

Captures F2 retenues :

- `run/screenshots/2026-06-29_02.00.30.png` :
  - portail visible ;
  - cadre cyan visible ;
  - contenu framebuffer visible.
- `run/screenshots/2026-06-29_02.00.30_2.png` :
  - frame sans portail/framebuffer visible.
- `run/screenshots/2026-06-29_02.03.31.png` :
  - frame sans portail/framebuffer visible.

Resultat :

- `lequal` visible : oui ;
- `lequal` stable : non, intermittent ;
- clignotement : encore present ;
- occlusion utile : partielle/non fiable a cause de l'intermittence ;
- `Missing program` : 0 ;
- `Buffer already closed` : 0 ;
- `ConcurrentModificationException` : 0 ;
- `Duplicate entity UUID` : 0 ;
- `UnsupportedOperationException` : 0.

Conclusion :

`lequal` atteint bien le rendu visuel sous Complementary mais ne stabilise pas le
contenu framebuffer. Il ne doit pas etre promu comme defaut global ni comme
strategie automatique. `no_depth` reste le fallback shaderpack-safe le plus
robuste connu, avec une occlusion moins stricte. Une video serait utile pour
quantifier la frequence du clignotement, mais les captures fixes suffisent a
classer `lequal` comme intermittent.

Rapport : `PHASE9.3B_IRIS_LEQUAL_MANUAL_VISUAL_PROOF.md`.

### 9.4 Formalisation du fallback shaderpack-safe no_depth

Objectif :

Formaliser `no_depth` comme fallback manuel shaderpack-safe, sans changer le
mode par defaut global.

Modification :

- parsing explicite de `IMM_PTL_FRAMEBUFFER_DEPTH_MODE` ;
- logs non spammy indiquant :
  - mode choisi ;
  - test de profondeur ;
  - origine du mode ;
- warning unique si `no_depth` est actif :
  - fallback compatibilite ;
  - occlusion potentiellement reduite ;
- warning unique et fallback `default` pour valeur invalide ;
- compatibilite conservee avec
  `IMM_PTL_FORCE_FRAMEBUFFER_NO_DEPTH_MASK=true` comme alias de `no_depth` si
  `IMM_PTL_FRAMEBUFFER_DEPTH_MODE` n'est pas defini.

Modes conserves :

- `default` : baseline historique, depth mask + `EQUAL` ;
- `no_depth` : fallback shaderpack-safe manuel, robuste mais occlusion reduite ;
- `lequal` : visible mais intermittent sous Complementary ;
- `always` : diagnostic seulement.

Validation compilation :

- vanilla : BUILD SUCCESSFUL ;
- Sodium compile-only : BUILD SUCCESSFUL ;
- Iris compile-only : BUILD SUCCESSFUL.

Tests runtime :

- MakeUp default :
  - source : `default implicit` ;
  - pipeline : `depth-masked-equal` ;
  - framebuffer : `854x480` ;
  - quad soumis ;
  - portail present cote client.
- Complementary no_depth :
  - source : `IMM_PTL_FRAMEBUFFER_DEPTH_MODE` ;
  - warning fallback compatibilite : oui ;
  - pipeline : `non-depth-masked` ;
  - framebuffer : `854x480` ;
  - quad soumis ;
  - portail present cote client.
- Valeur invalide `banana` :
  - warning unique : oui ;
  - fallback : `default` ;
  - source : `invalid IMM_PTL_FRAMEBUFFER_DEPTH_MODE fallback` ;
  - pipeline : `depth-masked-equal` ;
  - quad soumis ;
  - portail present cote client.

Erreurs runtime :

- `Missing program` : 0 ;
- `Buffer already closed` : 0 ;
- `ConcurrentModificationException` : 0 ;
- `Duplicate entity UUID` : 0 ;
- `UnsupportedOperationException` : 0.

Etat final :

- `run/config/iris.properties` restaure sur `MakeUp-UltraFast-9.5c.zip` ;
- aucun flag global laisse actif ;
- pas de promotion automatique de `no_depth` ;
- pas de changement du defaut global.

Rapport : `PHASE9.4_IRIS_SHADERPACK_SAFE_FALLBACK_MODE.md`.

### 9.5 Regression traversee Iris shaderpack fallback

Objectif :

Verifier que la baseline MakeUp `default` et le fallback manuel Complementary
`no_depth` permettent toujours la traversee du portail.

Validation compilation :

- vanilla : BUILD SUCCESSFUL ;
- Sodium compile-only : BUILD SUCCESSFUL ;
- Iris compile-only : BUILD SUCCESSFUL.

Tests runtime :

- MakeUp default + traversee :
  - monde : `Phase95MakeUpDefaultTraversal` ;
  - shaderpack : `MakeUp-UltraFast-9.5c.zip` ;
  - mode : `default` ;
  - source : `default implicit` ;
  - pipeline : `depth-masked-equal` ;
  - framebuffer : `854x480` ;
  - quad soumis ;
  - portail present cote client ;
  - `Client Teleported Statically` : oui.
- Complementary no_depth + traversee :
  - monde : `Phase95ComplementaryNoDepthTraversal` ;
  - shaderpack : `ComplementaryReimagined_r5.8.1.zip` ;
  - mode : `no_depth` ;
  - source : `IMM_PTL_FRAMEBUFFER_DEPTH_MODE` ;
  - pipeline : `non-depth-masked` ;
  - warning compatibilite `no_depth` : oui ;
  - framebuffer : `854x480` ;
  - quad soumis ;
  - portail present cote client ;
  - `Client Teleported Statically` : oui.
- Complementary alias historique + traversee :
  - monde : `Phase95ComplementaryLegacyAliasTraversal` ;
  - `IMM_PTL_FRAMEBUFFER_DEPTH_MODE` absent ;
  - `IMM_PTL_FORCE_FRAMEBUFFER_NO_DEPTH_MASK=true` ;
  - mode reel : `no_depth` ;
  - source : `IMM_PTL_FORCE_FRAMEBUFFER_NO_DEPTH_MASK` ;
  - pipeline : `non-depth-masked` ;
  - framebuffer : `854x480` ;
  - quad soumis ;
  - portail present cote client ;
  - `Client Teleported Statically` : oui.

Erreurs runtime :

- `Missing program` : 0 ;
- `Buffer already closed` : 0 ;
- `ConcurrentModificationException` : 0 ;
- `Duplicate entity UUID` : 0 ;
- `UnsupportedOperationException` : 0.

Etat final :

- `run/config/iris.properties` restaure sur `MakeUp-UltraFast-9.5c.zip` ;
- aucun flag global laisse actif ;
- pas de changement du defaut global ;
- pas de promotion automatique de `no_depth`.

Conclusion :

Le fallback manuel `no_depth` est valide cote traversee/runtime sous
Complementary, et la baseline MakeUp `default` reste saine.

Rapport : `PHASE9.5_IRIS_SHADERPACK_FALLBACK_TRAVERSAL_REGRESSION.md`.

### 10.0 Audit rendu avance, clipping et occlusion propre

Objectif :

Auditer la suite technique apres la baseline Iris shaderpack fallback, sans
modifier le renderer ni reactiver les chemins avances.

Etat de depart :

- baseline vanilla : `5545115e` ;
- baseline Sodium non-shader : `545169c1` ;
- baseline Iris shaderpack minimal : `8c09eb37` ;
- baseline Iris shaderpack fallback : `404c5000` ;
- MakeUp reste le shaderpack par defaut dans `run/config/iris.properties` ;
- aucun flag global de test laisse actif ;
- modes framebuffer conserves :
  - `default` -> `depth-masked-equal` ;
  - `no_depth` -> fallback manuel shaderpack-safe ;
  - `lequal` -> visible mais intermittent sous Complementary ;
  - `always` -> diagnostic.

Constat :

- le chemin framebuffer minimal + `SubmitNodeCollector` est sain ;
- le rendu destination est visible et traversable ;
- le fallback cyan reste conserve ;
- le prefiltrage CPU de `FrontClipping` ne clippe que des cas limites ;
- `GL_CLIP_PLANE0` seul reste insuffisant avec les shaders 26.1 ;
- `DepthStencilState` public expose la profondeur utile, mais pas un stencil
  exploitable pour le masque complet ;
- l'ancien `RendererUsingStencil` repose sur des manipulations GL directes trop
  risquees pour etre reactive brutalement ;
- les renderers Iris legacy restent volontairement no-op ;
- les mixins shader Sodium, shader clipping, fog avance et DimLib restent
  exclus.

Decision :

Ne pas commencer maintenant le shader clipping global ni l'ancien stencil avance.
La prochaine phase recommandee est une micro-phase `10.1` limitee a l'ordre et a
la profondeur du quad framebuffer :

- instrumentation non spammy ;
- aucune modification du defaut global ;
- aucune promotion automatique de `no_depth` ;
- tests MakeUp `default` et Complementary `default` / `lequal` / `no_depth` ;
- variantes opt-in seulement ;
- fallback cyan conserve.

Compilation :

Non relancee pendant 10.0, car aucun code runtime n'a ete modifie.

Rapport : `PHASE10.0_ADVANCED_RENDERING_CLIPPING_OCCLUSION_AUDIT.md`.

### 10.1 Micro-audit ordre/profondeur du quad framebuffer

Objectif :

Auditer, uniquement en opt-in, l'ordre `SubmitNodeCollector` et le masque
profondeur du quad framebuffer minimal.

Modification :

- ajout du flag dev `IMM_PTL_FRAMEBUFFER_ORDER_MODE` ;
- modes disponibles :
  - `default` : comportement historique, depth mask `order(0)` puis quad
    framebuffer `order(1)` ;
  - `mask_first_explicit` : meme ordre, force explicitement par flag ;
  - `quad_first` : quad `order(0)`, depth mask `order(1)` ;
  - `no_mask_reference` : quad sans depth mask, reference diagnostic ;
- instrumentation non spammy :
  - shaderpack detecte ;
  - mode profondeur ;
  - mode ordre ;
  - pipeline ;
  - framebuffer et taille ;
  - depth mask tente/applique/fallback ;
  - quad SubmitNodeCollector soumis.

Comportement par defaut :

- inchange si `IMM_PTL_FRAMEBUFFER_ORDER_MODE` est absent ;
- `IMM_PTL_FRAMEBUFFER_DEPTH_MODE` conserve ses modes existants :
  `default`, `no_depth`, `lequal`, `always` ;
- `no_depth` reste manuel et non promu automatiquement.

Validation compilation :

- vanilla : BUILD SUCCESSFUL ;
- Sodium compile-only : BUILD SUCCESSFUL ;
- Iris compile-only : BUILD SUCCESSFUL.

Tests runtime Iris + shaderpack :

- MakeUp default reference :
  - shaderpack : `MakeUp-UltraFast-9.5c.zip` ;
  - depth mode : `default` ;
  - order mode : `default` ;
  - pipeline : `depth-masked-equal` ;
  - framebuffer : `854x480` ;
  - depth mask applique ;
  - quad SubmitNodeCollector soumis ;
  - BUILD SUCCESSFUL.
- Complementary default reference :
  - depth mode : `default` ;
  - order mode : `default` ;
  - pipeline : `depth-masked-equal` ;
  - quad soumis ;
  - BUILD SUCCESSFUL.
- Complementary lequal reference :
  - depth mode : `lequal` ;
  - pipeline : `depth-masked-lequal` ;
  - quad soumis ;
  - BUILD SUCCESSFUL.
- Complementary no_depth reference :
  - depth mode : `no_depth` ;
  - pipeline : `non-depth-masked` ;
  - quad soumis ;
  - BUILD SUCCESSFUL.
- Complementary variantes opt-in :
  - `default + mask_first_explicit` : BUILD SUCCESSFUL ;
  - `lequal + mask_first_explicit` : BUILD SUCCESSFUL ;
  - `default + quad_first` : BUILD SUCCESSFUL ;
  - `default + no_mask_reference` : BUILD SUCCESSFUL.

Erreurs runtime recherchees :

- `Missing program` : 0 ;
- `Buffer already closed` : 0 ;
- `ConcurrentModificationException` : 0 ;
- `Duplicate entity UUID` : 0 ;
- `UnsupportedOperationException` : 0 ;
- crash report : 0.

Etat final :

- `run/config/iris.properties` restaure sur `MakeUp-UltraFast-9.5c.zip` ;
- aucun flag global laisse actif ;
- pas de changement du defaut global ;
- aucun stencil/shader clipping reactive ;
- aucun renderer Iris legacy restaure.

Conclusion :

Les variantes d'ordre/profondeur sont techniquement sures et reversibles, mais
aucune amelioration visuelle de Complementary n'est prouvee par les logs seuls.
`quad_first` et `no_mask_reference` restent des diagnostics, pas des candidats a
promotion automatique.

Recommandation Phase 10.2 :

- soit validation visuelle/video ciblee des variantes sous Complementary ;
- soit micro-phase de clipping CPU limite, car l'ordre SubmitNodeCollector seul
  ne resoud probablement pas les passes deferred/post-process du shaderpack.

Rapport : `PHASE10.1_FRAMEBUFFER_ORDER_DEPTH_MICRO_AUDIT.md`.
