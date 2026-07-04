# Phase 6.1 - Activation runtime progressive des mixins Sodium non-shader

Date : 20 juin 2026

Baseline vanilla :

`5545115e50e2b98b46303d7cf319d5d96f41ba5b`

## Resultat

La progression s'est arretee au Groupe A, conformement a la regle de la phase.

- Groupe A configure au runtime : oui ;
- menu Sodium avec Groupe A : atteint, fermeture normale ;
- monde avec Groupe A : joueur connecte, puis crash FRAPI ;
- reproduction avec groupe `none` : crash identique ;
- Groupe B : non tente ;
- Groupe C : non tente ;
- portail Sodium : non teste.

Le blocage ne vient pas d'un mixin Immersive Portals. Il existe avant Groupe A
et se reproduit quand `imm_ptl_compat.mixins.json` est entierement absent.

## Profil runtime controle

Une propriete cumulative est ajoutee :

```text
-Psodium_compat_runtime_group=none|A|B|C
```

Les mixins runtime ne sont inclus que si les trois conditions sont remplies :

```text
-Penable_sodium_compat=true
-Penable_sodium=true
-Psodium_compat_runtime_group=A|B|C
```

Le `processResources` genere alors un `imm_ptl_compat.mixins.json` reduit a la
liste exacte du groupe. Aucun mixin Iris, Flywheel, Cardinal Components ou
shader Sodium n'est present dans cette ressource.

Avec `none`, ou dans le profil vanilla, la ressource compat est absente.

## Groupes definis

### Groupe A - active et teste

1. `sodium.IESodiumWorldRenderer`
2. `sodium.MixinSodiumFlawlessFrames`

Le fichier genere contient exactement ces deux entrees. Le menu fonctionne et
se ferme normalement.

### Groupe B - defini, non active

Ajoute cumulativement :

1. `sodium.MixinSodiumWorldRenderer`
2. `sodium.MixinSodiumViewport`

Ce groupe n'a pas ete lance car le monde du Groupe A n'est pas stable.

### Groupe C - defini, non active

Ajoute cumulativement :

1. `sodium.MixinSodiumOcclusionCuller`
2. `sodium.MixinSodiumRenderRegion`
3. `sodium.MixinSodiumRenderSectionManager`

Ce groupe n'a pas ete lance.

### Toujours exclus

1. `sodium.MixinSodiumDefaultShaderInterface`
2. `sodium.MixinSodiumShaderLoader`

Ils ne figurent dans aucun groupe et ne sont toujours pas compiles dans le
profil Sodium. Le clipping shader n'a pas ete commence.

## Blocage monde exact

Premier crash Groupe A :

```text
java.lang.UnsupportedOperationException:
Attempted to retrieve active rendering plug-in before one was registered.
at net.fabricmc.fabric.impl.client.renderer.RendererManager.getRenderer
at net.fabricmc.fabric.api.client.renderer.v1.Renderer.get
at net.minecraft.client.renderer.feature.BlockFeatureRenderer...
```

Le joueur rejoint bien le serveur integre, puis le premier rendu d'objet tenu
demande le renderer Fabric API.

Le meme crash est reproduit avec :

```text
-Psodium_compat_runtime_group=none
```

Il n'est donc cause ni par `IESodiumWorldRenderer`, ni par
`MixinSodiumFlawlessFrames`.

## Cause technique

Sodium 0.8.7 declare dans ses metadonnees :

```text
fabric-renderer-api-v1:contains_renderer = true
```

Fabric Indigo voit cette declaration et desactive son enregistrement ainsi que
ses trois mixins. Dans ce jar Sodium, `FRAPIProvider` ne trouve aucune
implementation de service et utilise son fallback no-op. Aucun renderer FRAPI
n'est donc enregistre.

Un prototype a tente d'enregistrer directement `IndigoRenderer.INSTANCE`. Il a
ete retire : les mixins Indigo ayant deja ete desactives, cette instance plante
sur `BlockModelLighterAccessor` avec `IllegalClassLoadError`. Fabric API n'expose
pas de mecanisme supporte pour forcer Indigo apres la selection des mixins.

Le correctif devra venir d'une combinaison Sodium/Fabric API qui fournit un
renderer FRAPI coherent, ou d'un module FRAPI Sodium dedie. Il ne doit pas etre
masque par un patch renderer ad hoc dans Immersive Portals.

## Validation

- profil vanilla `compileJava processResources` : BUILD SUCCESSFUL ;
- profil Sodium compile-only : BUILD SUCCESSFUL ;
- Groupe A `processResources` : deux mixins exacts, aucun shader/Iris ;
- Groupe A menu : BUILD SUCCESSFUL ;
- Groupe A monde : blocage documente ;
- groupe `none` monde : meme blocage confirme ;
- Iris : non charge ;
- DimLib : non reactive ;
- AlternateDimensions : non reactive ;
- renderer minimal vanilla : non modifie ;
- B, C et portail : non testes apres le stop condition.

## Etape suivante

Avant de reprendre Groupe A monde, auditer une version Sodium 26.1 qui fournit
effectivement FRAPI avec Fabric API 0.145.1, ou aligner la version Fabric API
sur celle attendue par Sodium. Une fois un monde `none` stable, reprendre la
matrice A, puis B, puis C sans modifier le renderer minimal vanilla.
