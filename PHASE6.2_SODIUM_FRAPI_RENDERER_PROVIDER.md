# Phase 6.2 - Provider FRAPI Sodium

Date : 21 juin 2026

## Resultat

Le blocage FRAPI est resolu par le passage de Sodium
`0.8.7+mc26.1` a `0.8.9+mc26.1.1`.

La version 0.8.9 est declaree compatible avec Minecraft 26.1, 26.1.1 et 26.1.2
dans les metadonnees Modrinth. Elle exige Fabric API 0.145.1 ou plus recent ;
le projet utilise exactement `0.145.1+26.1`.

Validations :

- Sodium `group=none` : monde charge, stable 15 secondes, fermeture normale ;
- Sodium Groupe A : monde charge, stable 15 secondes, fermeture normale ;
- crash `Renderer.get()` : 0 avec Sodium 0.8.9 ;
- Groupe B : non teste ;
- Groupe C : non teste ;
- portail Sodium : non teste.

## Cause exacte avec Sodium 0.8.7

Sodium 0.8.7 declare :

```text
fabric-renderer-api-v1:contains_renderer = true
```

Cette declaration desactive l'initialisation et les mixins Indigo. Cependant,
le jar 0.8.7 ne contient aucun service :

```text
META-INF/services/net.caffeinemc.mods.sodium.client.services.FRAPIProvider
```

`FRAPIProvider.getInstance()` utilise donc son fallback no-op. Aucun renderer
Fabric API n'est enregistre et le premier rendu d'objet tenu echoue :

```text
java.lang.UnsupportedOperationException:
Attempted to retrieve active rendering plug-in before one was registered.
```

Le crash se reproduisait sans aucun mixin Immersive Portals/Sodium avec
`sodium_compat_runtime_group=none`.

## Difference dans Sodium 0.8.9

Le jar 0.8.9 contient le service manquant :

```text
META-INF/services/net.caffeinemc.mods.sodium.client.services.FRAPIProvider
```

Son contenu est :

```text
net.caffeinemc.mods.sodium.client.render.frapi.SodiumProvider
```

Sodium enregistre donc son propre renderer FRAPI. Indigo reste correctement
desactive et `Renderer.get()` retourne un provider valide.

## Alignement de versions

- Minecraft : 26.1 ;
- Fabric Loader : 0.19.3 ;
- Fabric API : 0.145.1+26.1 ;
- Sodium avant : 0.8.7+mc26.1 ;
- Sodium apres : 0.8.9+mc26.1.1.

Le `sodium_path` de `gradle.properties` pointe maintenant vers 0.8.9.

La contrainte `breaks` historique d'Immersive Portals autorisait uniquement
0.8.7. Dans le profil Sodium de test seulement, `processResources` l'assouplit
pour accepter la serie 0.8.x et refuser 0.9.0 ou plus recent. Le profil vanilla
par defaut conserve sa metadonnee historique et ne charge pas Sodium.

## Hypotheses testees

### A - Mauvaise combinaison de versions

Confirmee. Sodium 0.8.9 fournit le provider absent et fonctionne avec Fabric API
0.145.1.

### B - Sodium 0.8.7 n'installe pas de provider FRAPI

Confirmee par inspection du jar. Le service est absent en 0.8.7 et present en
0.8.9.

### C - Forcer Indigo

Rejetee. Indigo desactive aussi ses propres mixins lorsqu'un mod declare
`contains_renderer`. Enregistrer manuellement `IndigoRenderer.INSTANCE` produit
un `IllegalClassLoadError` sur `BlockModelLighterAccessor`. Le prototype de la
Phase 6.1 a ete entierement retire et n'est pas revenu.

## Tests runtime

### Groupe none

Commande :

```text
./gradlew runClient --console=plain \
  -Penable_sodium_compat=true \
  -Penable_sodium=true \
  -Psodium_compat_runtime_group=none \
  --args=--quickPlaySingleplayer=Phase50Test
```

Resultat : joueur connecte, chunks rendus, 15 secondes stables, sauvegarde et
fermeture normales, `BUILD SUCCESSFUL`.

### Groupe A

Commande identique avec `sodium_compat_runtime_group=A`.

Resultat : `IESodiumWorldRenderer` et `MixinSodiumFlawlessFrames` actifs,
joueur connecte, monde stable, fermeture normale, `BUILD SUCCESSFUL`.

## Ce qui reste inactif

- Groupe B ;
- Groupe C ;
- `MixinSodiumDefaultShaderInterface` ;
- `MixinSodiumShaderLoader` ;
- Iris ;
- DimLib et AlternateDimensions ;
- clipping shader et renderer avance.

## Validation de compilation

- profil vanilla : BUILD SUCCESSFUL ;
- profil Sodium compile-only : BUILD SUCCESSFUL ;
- erreurs Iris introduites : 0 ;
- erreurs DimLib introduites : 0 ;
- renderer minimal vanilla : inchange.

## Etape suivante

La Phase 6.3 peut reprendre la progression au Groupe B, avec menu puis monde,
sans modifier le provider FRAPI ni ouvrir les mixins shader.
