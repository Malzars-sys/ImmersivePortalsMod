# Phase 8.3 - Validation visuelle Iris avec shaderpack

## Objectif

Valider plus proprement le portail Iris avec shaderpack apres la correction des
overrides Iris de la Phase 8.2, sans modifier le renderer, les mappings Iris, les
pipelines Immersive Portals ou le mini-groupe Portal Iris/Sodium.

## Fichiers inspectes

- `PHASE8.2_IRIS_PIPELINE_OVERRIDE_AUDIT.md`
- `MIGRATION_PLAN_26.1.md`
- `src/main/java/qouteall/imm_ptl/core/platform_specific/IPModEntryClient.java`
- `src/main/java/qouteall/imm_ptl/core/commands/PortalDebugCommands.java`
- `src/main/java/qouteall/imm_ptl/core/render/renderer/RendererUsingFrameBuffer.java`

## Ajustement documentaire Phase 8.2

La conclusion visuelle de Phase 8.2 a ete precisee :

- la capture automatique Phase 8.2 est non concluante ;
- elle ne doit pas etre interpretee comme une preuve d'invisibilite ;
- une observation interactive apres rotation camera a permis de voir le portail ;
- Phase 8.3 reste chargee d'ameliorer la preuve visuelle automatique.

## Test visuel automatique

Commande :

```powershell
.\gradlew.bat runClient --console=plain -Penable_iris_compat=true -Penable_iris=true --args=--quickPlaySingleplayer=Phase81IrisShaderpackPortalTest
```

Flags :

```text
IMM_PTL_AUTO_VISIBLE_TEST_PORTAL=true
IMM_PTL_AUTO_MINIMAL_TRAVERSAL_TEST=false
IMM_PTL_CAPTURE_MINIMAL_RECURSIVE_PORTAL=true
IMM_PTL_MINIMAL_RECURSIVE_PORTAL_SCREENSHOT=phase8.3-iris-shaderpack-visual-validation.png
```

Resultat :

- shaderpack actif : `MakeUp-UltraFast-9.5c.zip`
- mapping Iris fallback enregistre : oui
- `PortalEntityRenderer.submit` appele : oui
- framebuffer minimal atteint : oui
- texture framebuffer disponible : oui, `854x480`
- depth mask applique : oui
- quad texture `SubmitNodeCollector` soumis : oui
- portail present cote client sous Sodium/Iris : oui
- capture automatique obtenue : oui
- capture automatique concluante : non
- erreurs `Missing program` : 0
- `Duplicate entity UUID` : 0
- `ConcurrentModificationException` : 0
- `Buffer already closed` : 0
- `UnsupportedOperationException` : 0
- crash : 0

Capture :

```text
run/screenshots/phase8.3-iris-shaderpack-visual-validation.png
```

La capture automatique est encore trop sombre et ne montre pas clairement le
portail. Le joueur a bien ete place face au portail d'apres les logs, mais la
preuve image automatique reste insuffisante sous ce shaderpack et cet etat de
monde. Ce n'est pas classe comme une regression renderer.

## Traversée

La traversee etait desactivee au depart pour stabiliser la capture, mais le run
visuel a tout de meme produit :

```text
Client Teleported Statically
```

La traversee Overworld vers Overworld reste donc valide dans le run 8.3.

Un second run avec `IMM_PTL_AUTO_MINIMAL_TRAVERSAL_TEST=true` a ete tente. Le
client est reste ouvert jusqu'au timeout de l'automatisation, donc ce second run
n'est pas utilise comme validation `BUILD SUCCESSFUL`. Il a toutefois confirme
le chargement Iris/shaderpack et le chemin framebuffer avant l'arret force du
processus de test.

## Compilation

- vanilla : `BUILD SUCCESSFUL`
- Sodium compile-only : `BUILD SUCCESSFUL`
- Iris compile-only : `BUILD SUCCESSFUL`

## Conclusion

- portail vu en observation interactive : oui, selon l'observation manuelle
  deja notee apres rotation de la camera ;
- capture automatique concluante : non ;
- portail present cote client : oui ;
- chemin framebuffer minimal sous Iris/shaderpack : oui ;
- `Missing program` : 0 ;
- traversee toujours valide : oui ;
- renderer modifie dans cette phase : non.

La suite doit ameliorer la preuve visuelle automatique ou fournir une capture
manuelle F2 fiable, idealement dans un etat de monde plus lisible, sans changer
le renderer tant que le probleme est seulement la capture.
