# Phase 6.3 - Activation runtime des groupes Sodium B et C

Date : 21 juin 2026

## Resultat

Les groupes B et C chargent Sodium 0.8.9, rejoignent `Phase50Test`, restent
stables pendant au moins 15 secondes et ferment normalement. Le groupe C est
la nouvelle baseline runtime Sodium non-shader.

Le profil vanilla reste separe. Iris, DimLib, AlternateDimensions et les deux
mixins shader Sodium restent inactifs.

## Corrections API Sodium 0.8.9

Deux incompatibilites de mixin ont ete trouvees successivement pendant le test
du groupe B :

1. `SodiumWorldRenderer.setupTerrain` accepte maintenant `FogParameters` et
   une matrice de culling `Matrix4f`. La signature de l'injection a ete alignee.
2. `Frustum.testAab` n'est plus appele depuis `Viewport.isBoxVisible`, mais
   depuis `Viewport.isBoxVisibleDirect`. La redirection a ete deplacee vers
   cette methode.

Les deux crashs initiaux sont conserves dans :

- `runclient-phase6.3-group-B-initial-crash.txt` ;
- `runclient-phase6.3-group-B-viewport-crash.txt`.

## Mixins valides

Groupe B :

- `sodium.IESodiumWorldRenderer` ;
- `sodium.MixinSodiumFlawlessFrames` ;
- `sodium.MixinSodiumWorldRenderer` ;
- `sodium.MixinSodiumViewport`.

Groupe C ajoute, sans crash :

- `sodium.MixinSodiumOcclusionCuller` ;
- `sodium.MixinSodiumRenderRegion` ;
- `sodium.MixinSodiumRenderSectionManager`.

Toujours exclus :

- `sodium.MixinSodiumDefaultShaderInterface` ;
- `sodium.MixinSodiumShaderLoader` ;
- tous les mixins Iris et DimLib.

## Validation runtime

### Groupe B

- Sodium 0.8.9 charge : oui ;
- quatre mixins exacts dans la ressource generee : oui ;
- monde et joueur charges : oui ;
- stabilite 15 secondes : oui ;
- fermeture normale et `BUILD SUCCESSFUL` : oui ;
- erreur FRAPI, erreur mixin, `ConcurrentModificationException`,
  `Buffer already closed`, `Duplicate entity UUID` : 0.

Log : `runclient-phase6.3-group-B-world.txt`.

### Groupe C

- sept mixins exacts dans la ressource generee : oui ;
- monde et joueur charges : oui ;
- culling et gestion des sections actifs sans crash : oui ;
- stabilite 15 secondes : oui ;
- fermeture normale et `BUILD SUCCESSFUL` : oui ;
- erreur FRAPI, erreur mixin, `ConcurrentModificationException`,
  `Buffer already closed`, `Duplicate entity UUID` : 0.

Log : `runclient-phase6.3-group-C-world.txt`.

## Test portail optionnel

Le portail de developpement est cree sous le groupe C et le test de traversee
declenche `Client Teleported Statically`. Aucun crash, doublon UUID, buffer
ferme ou modification concurrente n'est observe.

En revanche, aucun marqueur `PortalEntityRenderer`, collecte, framebuffer ou
blit n'apparait, et aucune capture n'est produite. Le chemin visuel minimal
vanilla n'est donc pas encore raccorde au renderer Sodium. Le cadre cyan et la
texture framebuffer ne sont pas confirmes sous Sodium dans cette phase.

Le processus de ce test a ete ferme par le script apres la fenetre
d'observation ; son code Gradle `-1` n'est pas un crash Minecraft. La validation
de fermeture normale repose sur le test monde Groupe C precedent.

Log : `runclient-phase6.3-group-C-portal.txt`.

## Conclusion

Le critere complet de la Phase 6.3 est atteint pour l'activation non-shader :
le groupe C est stable. La prochaine phase Sodium devra auditer uniquement le
pont de rendu des entites portail, sans activer Iris, DimLib ou les mixins
shader.
