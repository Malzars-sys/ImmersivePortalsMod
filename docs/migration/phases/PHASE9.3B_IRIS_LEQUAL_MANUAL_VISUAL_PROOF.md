# Phase 9.3B - Preuve visuelle manuelle LEQUAL sous Complementary

## Objectif

Obtenir une preuve visuelle fiable du portail sous Iris + Complementary avec :

```text
IMM_PTL_FRAMEBUFFER_DEPTH_MODE=lequal
shaderPack=ComplementaryReimagined_r5.8.1.zip
```

Cette phase ne modifie pas le renderer, ne change pas le mode par defaut global,
ne reactive pas le renderer Iris avance, shader clipping, les mixins shader
Sodium, DimLib ou AlternateDimensions.

## Run

Commande :

```text
.\gradlew.bat runClient --console=plain -Penable_iris_compat=true -Penable_iris=true --args=--quickPlaySingleplayer=Phase93ComplementaryLequalVisual
```

Flags :

```text
IMM_PTL_AUTO_VISIBLE_TEST_PORTAL=true
IMM_PTL_AUTO_MINIMAL_TRAVERSAL_TEST=false
IMM_PTL_CAPTURE_MINIMAL_RECURSIVE_PORTAL=false
IMM_PTL_FRAMEBUFFER_DEPTH_MODE=lequal
```

Log :

- `runclient-phase9.3B-complementary-lequal-manual-visual.txt`

## Validation runtime

Logs observes :

- shaderpack actif : `ComplementaryReimagined_r5.8.1.zip`
- mode framebuffer : `lequal`
- pipeline : `depth-masked-lequal`
- framebuffer disponible : `854x480`
- quad SubmitNodeCollector soumis : oui
- portail present cote client : oui
- `Missing program` : 0
- `Buffer already closed` : 0
- `ConcurrentModificationException` : 0
- `Duplicate entity UUID` : 0
- `UnsupportedOperationException` : 0

## Captures F2 retenues

Captures fournies pour la preuve manuelle :

- `run/screenshots/2026-06-29_02.00.30.png`
- `run/screenshots/2026-06-29_02.00.30_2.png`
- `run/screenshots/2026-06-29_02.03.31.png`

Observation :

- `2026-06-29_02.00.30.png` montre le portail avec cadre cyan et contenu
  framebuffer visible.
- `2026-06-29_02.00.30_2.png` montre une frame sans portail/framebuffer visible
  au meme test.
- `2026-06-29_02.03.31.png` montre egalement une frame sans portail/framebuffer
  visible.

Ces captures sont une preuve suffisante que le mode `lequal` atteint bien le
rendu visuel, mais qu'il reste intermittent sous Complementary.

Une video serait meilleure pour mesurer la frequence du clignotement, mais elle
n'est pas indispensable pour la conclusion technique de cette phase : `lequal`
ne stabilise pas completement le contenu framebuffer.

## Conclusion

- `lequal` visible : oui, sur au moins une capture.
- `lequal` stable : non, intermittent.
- cadre cyan visible : oui quand le portail est visible.
- contenu framebuffer visible : intermittent.
- clignotement : oui, encore present.
- occlusion utile : partielle/non fiable a cause de l'intermittence.
- candidat Phase 9.4 automatique : non comme defaut global.

`lequal` reste un candidat technique interessant, mais il ne suffit pas a rendre
Complementary stable. `no_depth` reste le fallback shaderpack-safe le plus
robuste connu, au prix d'une occlusion moins stricte.

## Etat final

`run/config/iris.properties` a ete restaure sur :

```text
shaderPack=MakeUp-UltraFast-9.5c.zip
```

Le run client Phase 9.3B a ete ferme apres capture et verification des logs.
