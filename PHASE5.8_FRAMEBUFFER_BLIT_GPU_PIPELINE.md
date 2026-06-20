# Phase 5.8 - Blit framebuffer avec le pipeline GPU 26.1

## Resultat

Phase terminee avec correctif minimal applique.

- `compileJava processResources` : BUILD SUCCESSFUL
- `runClient` menu vanilla : BUILD SUCCESSFUL, aucun crash
- `runClient --quickPlaySingleplayer=Phase50Test` : BUILD SUCCESSFUL, monde charge
- `ConcurrentModificationException` : 0 dans les logs de validation
- `Buffer already closed` : 0 dans les logs de validation apres correctif
- `Duplicate entity UUID` : 0 dans les logs de validation
- Sodium, Iris, DimLib, shaders, fog mixins et clipping mixins : non touches
- cadre cyan fallback : conserve

Le blit visible dans le rectangle du portail reste a revalider en jeu avec le
portail explicitement dans le champ de vision. La tentative d'injection clavier
de `/imm_ptl_debug create_minimal_test_portal` n'a pas ete confirmee dans les
logs automatises, donc le test final valide la stabilite et l'absence de
regression runtime, pas encore l'apparition visuelle de la vue destination.

## Fichiers inspectes

- `src/main/java/qouteall/imm_ptl/core/render/renderer/RendererUsingFrameBuffer.java`
- `src/main/java/qouteall/imm_ptl/core/render/pipeline/IPRenderPipelines.java`
- `src/main/java/qouteall/imm_ptl/core/render/PortalEntityRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/mixin/client/render/MixinGameRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/render/MyGameRenderer.java`
- `src/main/java/qouteall/imm_ptl/core/render/MyRenderHelper.java`
- `src/main/java/qouteall/imm_ptl/core/render/SecondaryFrameBuffer.java`
- `com.mojang.blaze3d.vertex.MeshData` via `javap`
- `com.mojang.blaze3d.vertex.VertexFormat` via `javap`
- `com.mojang.blaze3d.opengl.GlCommandEncoder` via `javap`
- `com.mojang.blaze3d.buffers.GpuBuffer` via `javap`

## Cause exacte

`IPRenderPipelines.drawMesh` utilisait :

```java
try (mesh; vertexBuffer; indexBuffer) {
    ...
}
```

Or `VertexFormat.uploadImmediateVertexBuffer(...)` et
`VertexFormat.uploadImmediateIndexBuffer(...)` renvoient les buffers immediats
internes du `VertexFormat`. Ces buffers sont caches et reutilises par Mojang
entre les draws immediats.

En les fermant apres chaque draw, le draw suivant essayait de recharger des
donnees dans un `GpuBuffer` deja ferme. Le crash venait ensuite de :

```text
GlCommandEncoder.writeToBuffer(...)
java.lang.IllegalStateException: Buffer already closed
```

Le `MeshData`, lui, reste bien propriete du draw appelant et doit etre ferme
apres l'upload.

## Correctif minimal

`IPRenderPipelines.drawMesh` ferme maintenant uniquement le `MeshData` :

```java
try (mesh) {
    ...
}
```

Les `GpuBuffer` immediats retournes par `VertexFormat` ne sont plus fermes par
Immersive Portals. Leur cycle de vie reste gere par le cache immediat Mojang.

Des logs uniques ont aussi ete ajoutes autour du blit :

- `Attempting minimal recursive portal framebuffer blit`
- `Minimal recursive portal framebuffer blit succeeded`
- `Minimal recursive portal framebuffer blit failed; using cyan frame fallback`

## Validation

Commandes executees :

```text
.\gradlew.bat compileJava processResources --rerun-tasks --console=plain
.\gradlew.bat runClient --console=plain
.\gradlew.bat runClient --console=plain --args=--quickPlaySingleplayer=Phase50Test
```

Resultats :

- `compile-phase5.8-26.1.txt` : BUILD SUCCESSFUL in 10s
- `runclient-phase5.8-final.txt` : BUILD SUCCESSFUL in 25s
- `runclient-phase5.8-world.txt` : BUILD SUCCESSFUL in 37s
- `runclient-phase5.8-portal.txt` : BUILD SUCCESSFUL in 56s

Les logs de validation ne contiennent pas :

- `Buffer already closed`
- `ConcurrentModificationException`
- `Duplicate entity UUID`
- `Game crashed`
- `Reported exception`

## Etat restant

Le blocage `Buffer already closed` est corrige au niveau du cycle de vie des
buffers immediats GPU.

La vue destination n'est pas encore confirmee visible dans un test automatise.
Le prochain blocage eventuel devra etre observe en forcant le portail dans le
champ de vision et en verifiant les logs de tentative/succes du blit.
