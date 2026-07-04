# Phase 5.16 - Decision rendu : clipping shader vanilla ou Phase 6

Date : 20 juin 2026

## Decision

Recommandation : **Option C, puis Option B**.

1. Figer le renderer minimal vanilla dans un commit de reference.
2. Commencer ensuite une Phase 6.0 avec **Sodium seul**, dans un profil dedie.
3. Ne commencer Iris qu'apres stabilisation de Sodium.
4. Garder DimLib et AlternateDimensions isoles jusqu'a disponibilite ou portage
   d'une version compatible Minecraft 26.1.

Une Phase 5.17 de clipping shader vanilla n'est pas recommandee maintenant.
Le probleme n'est pas l'ecriture d'un petit shader de quad : il faudrait rendre
la scene destination avec des variantes clippees de presque tous les pipelines
du monde.

## Fichiers et API inspectes

- `build.gradle`, `gradle.properties`
- `src/main/resources/imm_ptl.mixins.json`
- `src/main/resources/imm_ptl_compat.mixins.json`
- `IPRenderPipelines`, `MyRenderHelper`, `FrontClipping`
- `MyGameRenderer`, `RendererUsingFrameBuffer`, `PortalEntityRenderer`
- `MixinLevelRenderer`, `MixinRenderSystem_Clipping`
- `MixinProgram`, `MixinShaderInstance`, `MixinGameRenderer_Shaders`
- `ShaderCodeTransformation` et `shader_transformation.yaml`
- anciens shaders sous `assets/immersive_portals/shaders/core`
- facades et mixins Sodium/Iris
- `ClientWorldLoader`, `ServerTeleportationManager` et AlternateDimensions
- API 26.1 inspectee avec `javap` : `RenderPipeline.Builder`, `RenderPass` et
  `RenderPipelines`

## Audit du clipping shader vanilla 26.1

### Uniformes custom

`RenderPipeline.Builder.withUniform(name, UniformType)` et
`RenderPass.setUniform(name, GpuBuffer/GpuBufferSlice)` permettent bien a un
pipeline appartenant au mod de recevoir un plan de clipping.

Cela suffit pour un quad, une primitive debug ou un type de rendu entierement
controle par Immersive Portals. Cela ne modifie pas les pipelines vanilla deja
utilises par `LevelRenderer`.

### Pipeline dedie au framebuffer destination

Un pipeline dedie peut dessiner le **quad final** ou des geometries soumises
explicitement par le mod. Il ne peut pas reclipper apres coup la couleur deja
produite dans la texture du framebuffer secondaire.

Pour clipper pendant le rendu destination, chaque draw concerne doit employer
un vertex/fragment shader qui connait le plan. Il faudrait donc substituer ou
dupliquer les RenderTypes/pipelines appeles par le rendu du monde.

### Familles concernees

Le minimum fonctionnel couvre au moins sept familles :

1. terrain solide, cutout, cutout mipped et translucide ;
2. modeles de blocs et block entities ;
3. entites, items, armures, beams, emissif et outlines ;
4. particules opaques et translucides ;
5. ciel, astres et nuages ;
6. meteo ;
7. lignes, effets et rendus speciaux de mods.

La classe 26.1 `RenderPipelines` expose plusieurs dizaines de pipelines monde.
Les anciens transforms Immersive Portals couvraient deja quatre shaders terrain
et environ onze noms d'entite/particule, plus des chemins distincts Sodium et
Iris. Cette couverture serait encore incomplete pour les block entities et les
pipelines speciaux actuels.

### gl_ClipDistance et discard

- `gl_ClipDistance` est adapte, mais doit etre ecrit par chaque vertex shader et
  son activation doit etre coherente avec chaque passe.
- `discard` en fragment demande egalement une position monde ou vue fiable dans
  chaque fragment shader. Les interfaces varient selon les familles.
- Le plan ne peut pas etre injecte globalement dans les pipelines vanilla via
  la seule API publique `RenderPipeline`.
- Reactiver les anciens mixins shader ne convient pas : ils ciblent
  `ShaderInstance`, `Program` et `RenderSystem.setShader`, APIs retirees ou
  remplacees en 26.1.

### Reponse centrale

Le clipping shader vanilla minimal **n'est pas assez petit pour etre fait
maintenant**. Un prototype limiterait probablement le clipping au terrain et
donnerait une fausse impression de completion. Une solution generale imposerait
une interception transversale des RenderTypes ou une duplication importante du
rendu monde, avec un cout eleve juste avant la migration Sodium/Iris.

## Audit Phase 6

### Modules encore isoles

- Sodium : 9 mixins de compatibilite, interfaces de contexte et culling,
  dependance compile/runtime desactivee dans le profil vanilla.
- Iris : 7 mixins, plusieurs renderers/pipelines de compatibilite et anciens
  hooks de transformation shader ; Iris depend aussi du chemin Sodium.
- DimLib : binaire configure en `v1.1.0-mc1.21.1`, hooks dynamiques neutralises,
  dimensions secondaires et AlternateDimensions isoles.
- Ancien clipping shader : `MixinProgram`, `MixinShaderInstance`,
  `MixinGameRenderer_Shaders` et `MixinRenderSystem_Clipping` exclus.

### Ordre de risque

**Sodium doit etre le premier module reactive.** Il remplace directement le
rendu terrain, mais son perimetre est plus petit et mesurable que celui d'Iris.
Le premier jalon doit etre compilation + menu + monde sans chercher tout de
suite le rendu recursif parfait.

**Iris doit attendre Sodium.** Ses sept mixins touchent le pipeline Iris, les
shadow targets, le transform patcher et des shaders Sodium. Commencer Iris avant
de stabiliser la couche Sodium melangerait deux sources de regression.

**DimLib doit encore attendre.** La version configuree cible Minecraft 1.21.1,
pas 26.1. Son port concerne registres, dimensions dynamiques, creation de mondes
secondaires et teleportation ; il est orthogonal au renderer minimal stable.

### Incompatibilites probables

- noms, packages et contrats internes Sodium 0.8.7 modifies ;
- contextes de rendu/chunks et culling a remapper ;
- shaders Sodium ne suivant plus l'ancien transform textuel ;
- API pipeline/shadow d'Iris 1.10.8 differente des interfaces historiques ;
- absence d'un DimLib 26.1 correspondant a la version actuellement referencee.

## Plan recommande

### Phase 5.17 - gel technique, sans nouveau rendu

- faire un commit de reference du profil vanilla stable ;
- conserver les rapports 5.15 et 5.16 comme baseline ;
- etiqueter les flags dev et le renderer minimal comme experimental ;
- ne pas introduire de nouveau shader.

### Phase 6.0 - profil Sodium compile-only

- creer un profil separe, sans modifier le profil vanilla ;
- activer la dependance Sodium seule ;
- compiler d'abord les facades et les 9 mixins une par une ;
- garder Iris, DimLib et le clipping shader desactives ;
- verifier menu, chargement monde, rendu minimal et fallback cyan a chaque lot.

### Phases suivantes

- Phase 6.1 : runtime Sodium stable et contexte de rendu portail minimal ;
- Phase 6.2 : audit Iris sur la base Sodium stabilisee ;
- DimLib/AlternateDimensions dans une branche ou phase ulterieure independante.

## Risques evites par cette decision

- reimplementation prematuree de dizaines de pipelines vanilla ;
- double travail lorsque Sodium remplacera les pipelines terrain ;
- melange des regressions shader vanilla, Sodium et Iris ;
- destabilisation du renderer minimal valide des phases 5.0 a 5.15.

## Validation Phase 5.16

- aucun changement de code de rendu ;
- aucun mixin reactive ;
- Sodium, Iris, DimLib et AlternateDimensions non reactives ;
- compilation vanilla executee apres documentation ;
- decision : figer le socle, puis passer progressivement a Sodium.
