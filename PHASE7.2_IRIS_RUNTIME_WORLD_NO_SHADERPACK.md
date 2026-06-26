# Phase 7.2 - Iris runtime world without shaderpack

Date: 25 juin 2026

## Resultat

Le profil Iris runtime atteint maintenant un monde solo sans shaderpack et se
ferme normalement.

Validations finales :

- Vanilla `compileJava processResources` : BUILD SUCCESSFUL ;
- Sodium compile-only : BUILD SUCCESSFUL ;
- Iris compile-only : BUILD SUCCESSFUL ;
- Iris runtime menu sans shaderpack : BUILD SUCCESSFUL ;
- Iris runtime monde sans shaderpack : BUILD SUCCESSFUL ;
- monde temoin : `Phase72IrisNoShaderTest` ;
- joueur connecte : oui ;
- stabilite apres connexion : plus de 20 secondes ;
- fermeture normale : oui ;
- shaderpack charge : non ;
- mixins Iris Immersive Portals actifs au runtime : aucun ;
- DimLib / AlternateDimensions : inactifs ;
- mixins shader Sodium : inactifs ;
- crash : 0.

## Mods charges

Le runtime monde charge 58 mods. Les composants importants sont :

- Iris `1.10.8+mc26.1` ;
- Sodium `0.8.7+mc26.1` ;
- Immersive Portals `7.0.0-alpha.1` ;
- Fabric API `0.145.1+26.1`.

Le log confirme :

```text
Shaders are disabled because no valid shaderpack is selected
```

`imm_ptl_compat.mixins.json` reste exclu des ressources traitees. Les classes
Iris Immersive Portals restent des facades/no-op compile-only ; aucun renderer
Iris avance n'est restaure.

## Blocage rencontre

Le premier lancement monde a charge le joueur puis a crashe sur :

```text
java.lang.UnsupportedOperationException:
Attempted to retrieve active rendering plug-in before one was registered.
```

Chemin responsable :

```text
RendererManager.getRenderer
Renderer.get
BlockFeatureRenderer.renderMovingBlockSubmits
ItemInHandRenderer.renderHandsWithItems
GameRenderer.renderLevel
```

Cause exacte : Iris 1.10.8 doit utiliser Sodium 0.8.7 au runtime. Cette version
de Sodium declare `fabric-renderer-api-v1:contains_renderer=true`, ce qui
desactive Indigo, mais elle ne fournit pas le service FRAPI ajoute dans Sodium
0.8.9. Fabric Renderer API se retrouve donc sans provider actif des que le monde
rend un bloc ou un objet via ses hooks.

## Correctif minimal

Ajout de `IrisSodiumFrapiFallbackRenderer`, enregistre uniquement quand :

- l'environnement est un environnement de developpement ;
- Iris est charge ;
- Sodium est charge ;
- aucun provider Fabric Renderer API n'est deja actif.

Ce fallback ne restaure pas Indigo, ne touche pas au renderer Iris avance et ne
remplace pas la baseline Sodium 0.8.9. Il fournit seulement un provider FRAPI
minimal pour eviter le crash du profil Iris/Sodium 0.8.7 sans shaderpack.

Log attendu et observe :

```text
Registered minimal Fabric Renderer API fallback for Iris with Sodium 0.8.7
```

## Monde temoin

`--quickPlaySingleplayer=Phase72IrisNoShaderTest` ne cree pas automatiquement
une sauvegarde absente. Le monde temoin a donc ete prepare comme sauvegarde
isolee `Phase72IrisNoShaderTest` a partir du temoin propre `Phase49Test`, avec
les dossiers d'entites supprimes et les anciens `session.lock` retires avant le
test. `Phase50Test` n'a pas ete utilise comme temoin final.

Le log conserve un nom interne herite de l'ancien niveau (`Phase43Test3`) dans
certaines lignes serveur, mais le dossier charge par quick-play est bien
`Phase72IrisNoShaderTest`.

## Validation runtime monde

Extraits importants :

```text
iris 1.10.8+mc26.1
sodium 0.8.7+mc26.1
Registered minimal Fabric Renderer API fallback for Iris with Sodium 0.8.7
Shaders are disabled because no valid shaderpack is selected
Starting integrated minecraft server version 26.1
Player764 joined the game
Stopping!
BUILD SUCCESSFUL
```

Verifications negatives :

- `UnsupportedOperationException` : 0 apres correctif ;
- `Duplicate entity UUID` : 0 ;
- `ConcurrentModificationException` : 0 ;
- `Buffer already closed` : 0 ;
- `Mixin apply failed` : 0 ;
- crash report final : 0.

## Fichiers produits

- `compile-phase7.2-vanilla.txt`
- `compile-phase7.2-sodium.txt`
- `compile-phase7.2-iris.txt`
- `runclient-phase7.2-iris-menu.txt`
- `runclient-phase7.2-iris-world.txt`
- `git-diff-phase7.2.txt`

## Ce qui reste volontairement inactif

- shaderpack Iris ;
- renderer Iris avance Immersive Portals ;
- mixins Iris Immersive Portals runtime ;
- mixins shader Sodium ;
- DimLib ;
- AlternateDimensions dynamique ;
- clipping shader ;
- ancien renderer avance complet.

## Etape suivante

La Phase 7.3 peut ouvrir un test monde Iris plus exigeant ou commencer
l'activation d'un groupe Iris explicitement choisi. Le provider FRAPI fallback
doit rester considere comme une rustine de compatibilite Iris/Sodium 0.8.7,
pas comme une base de rendu avance.
