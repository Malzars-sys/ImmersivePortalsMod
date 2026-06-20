# Phase 5.9 - Validation controlee du rendu recursif minimal

## Resultat

Phase terminee avec validation runtime controlee.

- `compileJava processResources` : BUILD SUCCESSFUL
- `runClient --quickPlaySingleplayer=Phase50Test` : BUILD SUCCESSFUL
- portail cree explicitement par commande dev : oui
- joueur place face au portail : oui
- portail collecte par `PortalEntityRenderer` : oui
- rendu declenche depuis le hook `GameRenderer.renderLevel` : oui
- tentative de blit framebuffer : oui
- blit framebuffer reussi : oui
- `Buffer already closed` : 0
- `ConcurrentModificationException` : 0
- `Duplicate entity UUID` : 0
- crash : non
- Sodium, Iris, DimLib, shaders, fog mixins et clipping mixins : non touches

La validation visuelle par capture d'ecran n'a pas ete obtenue : la tentative
de capture systeme a pris la fenetre au premier plan au lieu de Minecraft. Le
rapport ne l'utilise donc pas comme preuve. En revanche, les logs confirment
que le portail etait cree, que le joueur etait replace face a lui, que le
portail etait collecte, et que le blit du framebuffer a reussi.

## Correctifs de test ajoutes

### Commande serveur dev

Commande ajoutee :

```text
/imm_ptl_debug create_visible_test_portal
```

Elle est enregistree uniquement avec les commandes de developpement
`imm_ptl_debug`. Elle cree un portail minimal Overworld vers Overworld comme
`create_minimal_test_portal`, puis replace le joueur face au portail pour que le
renderer puisse le collecter de maniere reproductible.

Logs ajoutes :

```text
Placed player at ... facing visible minimal test portal at ...
Created minimal test portal at ... targeting ...
```

### Declencheur client dev

Pour eviter l'injection clavier fragile pendant les tests automatises, un
declencheur client dev a ete ajoute. Il ne s'active que si :

```text
IMM_PTL_AUTO_VISIBLE_TEST_PORTAL=true
```

et uniquement en environnement de developpement. Apres quelques ticks en monde,
il envoie :

```text
imm_ptl_debug create_visible_test_portal
```

via `ClientPacketListener.sendCommand(...)`.

### Log de collecte renderer

`RendererUsingFrameBuffer.queueMinimalPortalFromEntityRenderer(...)` logge une
fois :

```text
Queued minimal recursive portal from PortalEntityRenderer: ...
```

## Logs clefs

Depuis `runclient-phase5.9-portal.txt` :

```text
Running dev auto visible test portal command
Queued minimal recursive portal from PortalEntityRenderer: Portal{340,north,...}
Placed player at (...) facing visible minimal test portal at (...)
Created minimal test portal at (...) targeting (...)
Rendering minimal recursive portal from GameRenderer renderLevel hook Portal{340,north,...}
Advanced fog context unavailable; using vanilla fog fallback
Attempting minimal recursive portal framebuffer blit
Minimal recursive portal framebuffer blit succeeded
BUILD SUCCESSFUL in 55s
```

Depuis `runclient-phase5.9-visual.txt` :

```text
Queued minimal recursive portal from PortalEntityRenderer: Portal{1,north,...}
Rendering minimal recursive portal from GameRenderer renderLevel hook Portal{1,north,...}
Attempting minimal recursive portal framebuffer blit
Minimal recursive portal framebuffer blit succeeded
Running dev auto visible test portal command
Placed player at (...) facing visible minimal test portal at (...)
Created minimal test portal at (...) targeting (...)
BUILD SUCCESSFUL in 43s
```

Les logs de validation ne contiennent pas :

- `Buffer already closed`
- `ConcurrentModificationException`
- `Duplicate entity UUID`
- `Game crashed`
- `Reported exception`

## Interpretation

Le blocage de la phase 5.8 est leve : le pipeline minimal peut creer le
framebuffer secondaire, rendre la vue destination, puis blitter le resultat
sans casser le cycle de vie des buffers GPU immediats.

La preuve visuelle humaine reste a faire fenetre Minecraft au premier plan. Les
artefacts attendus restent probables et non corriges dans cette phase :

- pas de stencil avance
- pas de clipping avance
- cadrage/projection possiblement imparfaits
- profondeur probablement imparfaite
- fog vanilla fallback
- une seule recursion

## Fichiers modifies

- `src/main/java/qouteall/imm_ptl/core/commands/PortalDebugCommands.java`
- `src/main/java/qouteall/imm_ptl/core/platform_specific/IPModEntryClient.java`
- `src/main/java/qouteall/imm_ptl/core/render/renderer/RendererUsingFrameBuffer.java`
- `MIGRATION_PLAN_26.1.md`

## Commandes de validation

```text
.\gradlew.bat compileJava processResources --rerun-tasks --console=plain
IMM_PTL_AUTO_VISIBLE_TEST_PORTAL=true .\gradlew.bat runClient --console=plain --args=--quickPlaySingleplayer=Phase50Test
```

## Statut

Phase 5.9 valide le chemin runtime du rendu recursif minimal jusqu'au blit
framebuffer reussi. La prochaine etape peut se concentrer sur une vraie
inspection visuelle interactive ou sur les artefacts de projection/clipping,
sans rouvrir Sodium, Iris ou DimLib.
