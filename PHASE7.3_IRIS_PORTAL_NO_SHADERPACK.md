# Phase 7.3 - Iris runtime portal without shaderpack

Date: 26 juin 2026

## Resultat

Le test portail sous Iris runtime sans shaderpack est termine. La chaine
fonctionnelle minimale du portail est validee cote creation, synchronisation et
traversee. Le rendu visuel du portail reste bloque par le filtrage Sodium
0.8.7, avant `PortalEntityRenderer`.

Validations :

- Vanilla `compileJava processResources` : BUILD SUCCESSFUL ;
- Sodium compile-only : BUILD SUCCESSFUL ;
- Iris compile-only : BUILD SUCCESSFUL ;
- Iris runtime monde sans shaderpack : BUILD SUCCESSFUL ;
- Iris runtime portail sans shaderpack : BUILD SUCCESSFUL ;
- Iris runtime : `1.10.8+mc26.1` ;
- Sodium runtime Iris : `0.8.7+mc26.1` ;
- shaderpack charge : non ;
- fallback FRAPI Iris/Sodium 0.8.7 : enregistre ;
- DimLib : inactif ;
- AlternateDimensions : inactif ;
- renderer Iris avance : no-op ;
- mixins compat Immersive Portals Iris runtime : aucun ;
- crash final : 0.

## Compilations

Commandes validees :

```text
.\gradlew.bat compileJava processResources --rerun-tasks --console=plain
.\gradlew.bat compileJava processResources --rerun-tasks --console=plain -Penable_sodium_compat=true
.\gradlew.bat compileJava processResources --rerun-tasks --console=plain -Penable_iris_compat=true
```

Resultats :

- `compile-phase7.3-vanilla.txt` : BUILD SUCCESSFUL ;
- `compile-phase7.3-sodium.txt` : BUILD SUCCESSFUL ;
- `compile-phase7.3-iris.txt` : BUILD SUCCESSFUL.

## Monde Iris sans shaderpack

Commande :

```text
.\gradlew.bat runClient --console=plain -Penable_iris_compat=true -Penable_iris=true --args=--quickPlaySingleplayer=Phase72IrisNoShaderTest
```

Resultat :

- Iris charge : oui ;
- Sodium 0.8.7 charge : oui ;
- fallback FRAPI enregistre : oui ;
- shaderpack charge : non ;
- monde charge : oui ;
- joueur connecte : oui ;
- fermeture normale : oui ;
- `BUILD SUCCESSFUL`.

Extraits :

```text
iris 1.10.8+mc26.1
sodium 0.8.7+mc26.1
Registered minimal Fabric Renderer API fallback for Iris with Sodium 0.8.7
Shaders are disabled because no valid shaderpack is selected
Player927 joined the game
BUILD SUCCESSFUL
```

## Test portail

Flags utilises :

```text
IMM_PTL_AUTO_VISIBLE_TEST_PORTAL=true
IMM_PTL_AUTO_MINIMAL_TRAVERSAL_TEST=true
IMM_PTL_CAPTURE_MINIMAL_RECURSIVE_PORTAL=true
IMM_PTL_MINIMAL_RECURSIVE_PORTAL_SCREENSHOT=phase7.3-iris-no-shaderpack-portal.png
```

Commande :

```text
.\gradlew.bat runClient --console=plain -Penable_iris_compat=true -Penable_iris=true --args=--quickPlaySingleplayer=Phase72IrisNoShaderTest
```

Resultat fonctionnel :

- portail cree : oui ;
- portail present cote client : oui ;
- traversee declenchee : oui ;
- `Client Teleported Statically` : oui ;
- fermeture normale : oui ;
- `BUILD SUCCESSFUL`.

Extraits :

```text
Created minimal test portal at (-508.5, 94.5, -88.5) targeting (-508.5, 94.5, -98.5)
Portal present client-side under Sodium: true
Running dev auto minimal portal traversal command
Client Teleported Statically
BUILD SUCCESSFUL
```

## Rendu portail

Le rendu portail n'est pas atteint dans ce profil :

- `PortalEntityRenderer submit called under Sodium` : absent ;
- `Queued minimal recursive portal` : absent ;
- `Minimal recursive portal framebuffer blit succeeded` : absent ;
- capture `phase7.3-iris-no-shaderpack-portal.png` : non obtenue.

Cause exacte : le profil Iris runtime n'active pas `imm_ptl_compat.mixins.json`.
Les deux bypass portail ajoutes pendant la Phase 6.4 pour Sodium Groupe C ne
sont donc pas actifs :

- `MixinSodiumPortalEntityRenderer` ;
- `MixinSodiumPortalLevelRenderer`.

Ces mixins avaient corrige le filtrage Sodium avant `PortalEntityRenderer` sous
Sodium 0.8.9. Sous Iris runtime, Sodium 0.8.7 applique le meme type de filtrage
avant l'extraction/soumission du renderer portail. Le portail est bien present
cote client et la collision/traversee fonctionne, mais il est elimine du chemin
de rendu avant `PortalEntityRenderer.submit`.

Cette phase n'active pas le Groupe C complet, conformement a la consigne. La
suite devrait etre un mini-groupe Iris/Sodium 0.8.7 limite aux portails :

- bypass `Portal` dans `EntityRenderer.shouldRender` ;
- bypass `Portal` dans `LevelRenderer.isSectionCompiledAndVisible` si necessaire ;
- aucun mixin shader Sodium ;
- aucun culling/region avance ;
- aucun renderer Iris avance.

## Verifications negatives

Dans `runclient-phase7.3-iris-portal.txt` :

- `UnsupportedOperationException` : 0 ;
- `Duplicate entity UUID` : 0 ;
- `ConcurrentModificationException` : 0 ;
- `Buffer already closed` : 0 ;
- `Mixin apply failed` : 0 ;
- crash marker `#@!@#` : 0 ;
- `BUILD FAILED` : 0.

La seule ligne `Exception` observee est l'erreur Realms externe habituelle :

```text
Could not authorize you against Realms server
```

Elle n'affecte pas le test.

## Mixins et ressources

Apres `processResources` Iris :

- `build/resources/main/imm_ptl_compat.mixins.json` : absent ;
- mixins compat actifs Immersive Portals Iris/Sodium : aucun ;
- metadata runtime Iris autorise Sodium `0.8.7 <= version < 0.9.0` ;
- shaderpack : non charge.

## Fichiers produits

- `compile-phase7.3-vanilla.txt`
- `compile-phase7.3-sodium.txt`
- `compile-phase7.3-iris.txt`
- `runclient-phase7.3-iris-world.txt`
- `runclient-phase7.3-iris-portal.txt`
- `git-diff-phase7.3.txt`

Capture demandee non produite, car le renderer portail n'a pas ete appele.

## Conclusion

Critere minimal atteint : sous Iris runtime sans shaderpack, le monde reste
stable, le portail est present cote client et la traversee est testee avec
succes. Le blocage visuel est precisement identifie : absence des bypass portail
Sodium dans le profil Iris/Sodium 0.8.7. Aucune correction lourde n'a ete lancee
dans cette phase.
