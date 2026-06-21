# Phase 6.0 - Profil Sodium compile-only separe

Date : 20 juin 2026

Baseline vanilla :

`5545115e50e2b98b46303d7cf319d5d96f41ba5b`

## Resultat

Le profil Sodium compile-only est disponible avec :

```text
./gradlew compileJava processResources --rerun-tasks --console=plain -Penable_sodium_compat=true
```

Le profil vanilla reste le profil par defaut. La propriete
`enable_sodium_compat` vaut `false` dans `gradle.properties` et n'affecte donc
pas une invocation Gradle normale.

## Separation des profils

### Profil vanilla par defaut

- aucune dependance Sodium ;
- sources et mixins Sodium exclus de `sourceSets.main` ;
- `imm_ptl_compat.mixins.json` absent des ressources produites ;
- facade `SodiumInterface` no-op conservee ;
- renderer minimal vanilla inchange.

### Profil Sodium compile-only

- Sodium `mc26.1-0.8.7-fabric` ajoute en `compileOnly` ;
- Iris non ajoute ;
- sources Sodium non-shader compilees ;
- `imm_ptl_compat.mixins.json` toujours absent des ressources produites ;
- aucun mixin de compatibilite Immersive Portals/Sodium applique au runtime ;
- `SodiumInterface` reste no-op tant que la couche runtime n'est pas activee.

Le `compileOnly` DimLib 1.21.1 existant dans la baseline vanilla reste visible
dans le classpath pour les signatures `dim_stack`, mais DimLib n'est ni ajoute
au runtime ni reactive. Aucune erreur DimLib n'a ete introduite.

### Porte runtime explicite

Le test menu utilise deux flags :

```text
./gradlew runClient --console=plain \
  -Penable_sodium_compat=true -Penable_sodium=true
```

`enable_sodium_compat` seul reste compile-only. `enable_sodium=true` ajoute le
jar Sodium au runtime uniquement sur demande. Le fichier de mixins Immersive
Portals/Sodium reste exclu dans les deux cas pendant la Phase 6.0.

## Adaptations API Sodium 0.8.7

La premiere compilation a produit deux erreurs Sodium :

1. `OcclusionCuller.Visitor` n'existe plus ; il est remplace par
   `RenderSectionVisitor`.
2. `Camera.getPosition()` est remplace par `Camera.position()` en Minecraft
   26.1.

Les deux corrections sont locales aux mixins Sodium. Resultat final : zero
erreur Sodium.

## Mixins Sodium compiles mais desactives au runtime

1. `sodium.IESodiumWorldRenderer`
2. `sodium.MixinSodiumFlawlessFrames`
3. `sodium.MixinSodiumOcclusionCuller`
4. `sodium.MixinSodiumRenderRegion`
5. `sodium.MixinSodiumRenderSectionManager`
6. `sodium.MixinSodiumViewport`
7. `sodium.MixinSodiumWorldRenderer`

Ils sont valides au niveau compilation seulement. Leurs cibles et injections
runtime seront activees et testees par petits groupes pendant la Phase 6.1.

## Mixins Sodium encore exclus de la compilation

1. `sodium.MixinSodiumDefaultShaderInterface`
2. `sodium.MixinSodiumShaderLoader`

Ces deux classes appartiennent a l'ancien clipping shader et dependent encore
de `Program` ou d'interfaces shader Sodium historiques. Elles sont hors scope :
aucun clipping shader n'est commence en Phase 6.0.

## Validation

### Vanilla

- `compileJava` : BUILD SUCCESSFUL ;
- `processResources` : BUILD SUCCESSFUL ;
- comportement par defaut inchange ;
- aucune source Sodium compilee dans ce profil.

### Sodium compile-only

- dependance Sodium resolue : oui ;
- `compileJava` : BUILD SUCCESSFUL ;
- `processResources` : BUILD SUCCESSFUL ;
- erreurs Sodium restantes : 0 ;
- erreurs Iris introduites : 0 ;
- erreurs DimLib introduites : 0.

### Menu Sodium explicite

- Sodium 0.8.7 charge : oui ;
- menu Minecraft atteint : oui ;
- fermeture normale : oui ;
- `runClient` : BUILD SUCCESSFUL ;
- mixins compat Immersive Portals/Sodium : non actifs ;
- monde non teste dans cette phase compile-only.

## Limites et prochaine etape

La Phase 6.0 ne valide pas encore les injections Sodium. La Phase 6.1 devra
produire une ressource compat Sodium-only, puis activer les mixins dans cet
ordre prudent : facade/accessor, mise a jour camera/frustum, contexte de listes,
culling. Les deux mixins shader resteront exclus.

Iris, DimLib, AlternateDimensions, clipping shader, fog mixin et ancien renderer
avance restent hors scope.
