# Phase 5.10 - Inspection visuelle du rendu recursif minimal

## Resultat

Phase terminee avec preuve visuelle native Minecraft.

- `compileJava processResources` : BUILD SUCCESSFUL
- `runClient --quickPlaySingleplayer=Phase50Test` : BUILD SUCCESSFUL
- portail visible dans le chemin de rendu : oui
- portail collecte par `PortalEntityRenderer` : oui
- rendu declenche depuis `GameRenderer.renderLevel` : oui
- blit framebuffer tente : oui
- blit framebuffer reussi : oui
- capture Minecraft native obtenue : oui
- `Buffer already closed` : 0
- `ConcurrentModificationException` : 0
- `Duplicate entity UUID` : 0
- crash : non
- Sodium, Iris, DimLib, shaders, fog mixins et clipping mixins : non touches

Capture obtenue :

```text
run/screenshots/phase5.10-minimal-recursive-portal.png
```

## Methode

Le test utilise les outils de Phase 5.9 :

```text
IMM_PTL_AUTO_VISIBLE_TEST_PORTAL=true
```

et ajoute une capture native opt-in :

```text
IMM_PTL_CAPTURE_MINIMAL_RECURSIVE_PORTAL=true
```

Quand le blit framebuffer minimal reussit, `RendererUsingFrameBuffer` appelle
`Screenshot.grab(...)` une seule fois pour capturer le framebuffer principal de
Minecraft. Cette methode evite les problemes de fenetre Windows au premier plan
et capture directement ce que Minecraft a rendu.

## Logs clefs

Depuis `runclient-phase5.10-visual.txt` :

```text
Running dev auto visible test portal command
Placed player at (-507.5, 91.0, -95.5) facing visible minimal test portal at (-507.5, 92.5, -90.5)
Created minimal test portal at (-507.5, 92.5, -90.5) targeting (-507.5, 92.5, -80.5)
Queued minimal recursive portal from PortalEntityRenderer: Portal{358,south,...}
Rendering minimal recursive portal from GameRenderer renderLevel hook Portal{358,south,...}
Advanced fog context unavailable; using vanilla fog fallback
Attempting minimal recursive portal framebuffer blit
Minimal recursive portal framebuffer blit succeeded
Captured minimal recursive portal screenshot: Saved screenshot as phase5.10-minimal-recursive-portal.png
BUILD SUCCESSFUL in 48s
```

Les logs ne contiennent pas :

- `Buffer already closed`
- `ConcurrentModificationException`
- `Duplicate entity UUID`
- `Game crashed`
- `Reported exception`

## Observation visuelle

La capture montre bien une texture framebuffer dans la zone centrale du portail.
La vue destination est donc visible au sens minimal : le contenu rendu dans le
framebuffer secondaire est blitte vers l'image principale.

Artefacts observes :

- l'image destination est fortement retournee/inversee ;
- le cadrage est mauvais ;
- le contenu rendu apparait dans un petit rectangle central ;
- l'exterieur de l'image est noir dans la capture native ;
- le clipping du portail n'est pas correct ;
- la profondeur n'est pas fiable ;
- le fog utilise le fallback vanilla ;
- la recursion reste limitee a une seule couche ;
- le cadre cyan minimal est encore le fallback de securite.

Non observe dans cette phase :

- crash ;
- freeze ;
- `Buffer already closed` ;
- `ConcurrentModificationException` ;
- retour au fallback apres le succes du blit.

## Interpretation

Le rendu recursif minimal vanilla n'est plus seulement un chemin runtime
theorique : il produit une image destination visible. Le probleme prioritaire
n'est plus le blit GPU, mais la composition visuelle :

1. matrice de vue/projection incorrecte ou appliquee au mauvais moment ;
2. blit du quad portail mal cadre ;
3. absence de clipping/stencil avance ;
4. ordre de rendu principal incomplet dans la capture native.

## Prochain micro-correctif recommande

Avant de toucher au stencil avance ou a Sodium/Iris, le prochain travail devrait
viser le cadrage du quad et l'orientation de la texture :

- verifier l'ordre des sommets `p0..p3` dans `drawPortalAreaWithFramebuffer` ;
- verifier les coordonnees UV ;
- verifier si la projection utilisee dans le blit est celle du rendu principal
  au bon moment ;
- verifier si le framebuffer secondaire est blitte avec une orientation Y
  inversee.

## Fichiers modifies

- `src/main/java/qouteall/imm_ptl/core/render/renderer/RendererUsingFrameBuffer.java`
- `MIGRATION_PLAN_26.1.md`

## Statut

Phase 5.10 valide que la vue destination minimale est visible, mais avec des
artefacts de cadrage/orientation majeurs. Aucun travail Sodium, Iris, DimLib,
stencil avance ou clipping avance n'a ete commence.
