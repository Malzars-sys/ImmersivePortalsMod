# Phase 6.4 - Pont visuel minimal des portails sous Sodium

Date : 21 juin 2026

## Resultat

Le renderer minimal des portails est de nouveau actif sous Sodium 0.8.9,
Groupe C. Le cadre cyan, la collecte non reentrante, le framebuffer secondaire,
le masque profondeur et le quad texture via `SubmitNodeCollector` sont atteints.

Le profil vanilla n'utilise pas les deux nouveaux mixins. Iris, DimLib,
AlternateDimensions et les mixins shader Sodium restent inactifs.

## Cause exacte

Le portail etait bien present cote client, mais Sodium ajoutait deux filtres
avant `PortalEntityRenderer.extractRenderState` :

1. `EntityRenderer.shouldRender` appelle
   `SodiumWorldRenderer.isEntityVisible`. Le portail, entite sans volume de
   section conventionnel, etait rejete par ce culling.
2. `LevelRenderer.extractVisibleEntities` appelle ensuite
   `isSectionCompiledAndVisible`. Une section d'air portant un portail peut ne
   pas avoir de mesh terrain Sodium et etait rejetee ici aussi.

Sodium ne remplace donc pas le renderer d'entites. Il eliminait le portail
avant l'extraction de son etat de rendu.

## Correctif minimal

Deux mixins reserves au Groupe C ont ete ajoutes :

- `MixinSodiumPortalEntityRenderer` autorise uniquement les entites `Portal`
  dans `EntityRenderer.shouldRender` ;
- `MixinSodiumPortalLevelRenderer` autorise uniquement ces portails lorsque
  leur section terrain Sodium n'est pas compilee/visible.

Le frustum, l'extraction d'etat et la soumission restent ensuite dans le chemin
Minecraft/Immersive Portals existant. Aucun dessin direct, rendu monde
reentrant, shader ou framebuffer supplementaire n'a ete ajoute.

Le Groupe C contient maintenant neuf mixins non-shader. Les Groupes A et B
restent inchanges.

## Instrumentation

Des logs uniques confirment :

- portail present cote client sous Sodium ;
- bypass frustum Sodium actif ;
- bypass de section Sodium actif ;
- `PortalEntityRenderer.extractRenderState` appele ;
- `PortalEntityRenderer.submit` appele ;
- portail collecte pour le renderer minimal.

L'instrumentation de presence client ne s'active que pour le test dev
`IMM_PTL_AUTO_VISIBLE_TEST_PORTAL` avec Sodium charge.

## Rendu valide

Le log runtime confirme :

- rendu destination declenche depuis le hook non reentrant `GameRenderer` ;
- texture framebuffer disponible en 854 x 480 ;
- masque profondeur minimal applique ;
- quad texture soumis via `SubmitNodeCollector` ;
- capture native enregistree.

La capture montre le cadre cyan et la scene destination. Les cadres imbriques,
le clipping incomplet et la recursion unique restent les limites connues du
renderer minimal ; ils ne sont pas traites dans cette phase.

Capture : `run/screenshots/phase6.4-sodium-visual-portal.png`.

## Traversée

La traversée Overworld vers Overworld est revalidee dans `Phase49Test` :
`Client Teleported Statically` est observe. Le profil isole encore le mixin
lourd de tracking d'entites. `McHelper.sendToTrackers` traite donc
`AbstractMethodError` comme une facade optionnelle, comme le faisait deja le
chemin voisin `resendSpawnPacketToTrackers`.

## Validation

- vanilla `compileJava processResources` : BUILD SUCCESSFUL ;
- Sodium compile-only : BUILD SUCCESSFUL ;
- Groupe C monde : joueur connecte, stable, fermeture normale,
  BUILD SUCCESSFUL ;
- portail present cote client : oui ;
- cadre cyan visible : oui ;
- framebuffer minimal soumis : oui ;
- capture native : oui ;
- traversee : oui ;
- `ConcurrentModificationException` : 0 ;
- `Buffer already closed` : 0 ;
- `Duplicate entity UUID` : 0 ;
- `AbstractMethodError` final : 0 ;
- crash Minecraft : 0.

Les erreurs 404 de verification de version et les timeouts du verificateur
ModMenu sont externes au renderer et n'affectent pas ces validations.

## Hooks Sodium inspectes

- `SodiumWorldRenderer.setupTerrain` et `isEntityVisible` ;
- `Viewport.isBoxVisibleDirect` ;
- `OcclusionCuller.findVisible` ;
- `RenderRegion.getRenderList` ;
- `RenderSectionManager.isSectionVisible` ;
- mixin Sodium `EntityRendererMixin` ;
- mixin Sodium `LevelRendererMixin` ;
- `LevelRenderer.extractVisibleEntities` et `submitEntities` 26.1.

## Hors scope conserve

- Iris et DimLib ;
- AlternateDimensions dynamique ;
- `MixinSodiumDefaultShaderInterface` ;
- `MixinSodiumShaderLoader` ;
- clipping shader, stencil avance et ancien renderer complet.
