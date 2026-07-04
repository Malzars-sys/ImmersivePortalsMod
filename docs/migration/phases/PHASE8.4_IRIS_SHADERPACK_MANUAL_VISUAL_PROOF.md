# Phase 8.4 - Preuve visuelle manuelle Iris avec shaderpack

## Objectif

Obtenir une preuve visuelle manuelle fiable du portail Iris avec shaderpack, sans
modifier le renderer, les mappings Iris, les pipelines, Sodium shader, DimLib ou
le renderer Iris avance.

## Regle appliquee

Aucun code de rendu n'a ete modifie pendant cette phase.

Le correctif Phase 8.2 reste intact :

- `DRAW_FRAMEBUFFER_IN_AREA` -> `ShaderKey.TEXTURED`
- `DRAW_FRAMEBUFFER_IN_AREA_DEPTH_MASKED` -> `ShaderKey.TEXTURED`
- `PORTAL_DEPTH_MASK` -> `ShaderKey.BASIC_COLOR`

## Commande utilisee

```powershell
.\gradlew.bat runClient --console=plain -Penable_iris_compat=true -Penable_iris=true --args=--quickPlaySingleplayer=Phase81IrisShaderpackPortalTest
```

Flags du run final :

```text
IMM_PTL_AUTO_VISIBLE_TEST_PORTAL=true
IMM_PTL_AUTO_MINIMAL_TRAVERSAL_TEST=false
IMM_PTL_CAPTURE_MINIMAL_RECURSIVE_PORTAL=false
```

## Logs runtime

Run final :

```text
runclient-phase8.4-iris-shaderpack-manual-visual.txt
runclient-phase8.4-iris-shaderpack-manual-visual-stderr.txt
```

Resultat :

- `BUILD SUCCESSFUL in 1m 40s`
- shaderpack actif : `MakeUp-UltraFast-9.5c.zip`
- mapping Iris fallback enregistre : oui
- `PortalEntityRenderer.submit` appele : oui
- framebuffer minimal atteint : oui
- texture framebuffer disponible : oui, `854x480`
- depth mask applique : oui
- quad texture `SubmitNodeCollector` soumis : oui
- portail cree par commande dev : oui
- portail present cote client sous Sodium/Iris : oui
- erreurs `Missing program` : 0
- `Duplicate entity UUID` : 0
- `ConcurrentModificationException` : 0
- `Buffer already closed` : 0
- `UnsupportedOperationException` : 0
- crash : 0

## Captures

Captures F2 natives locales obtenues :

```text
run/screenshots/2026-06-28_20.01.16.png
run/screenshots/2026-06-28_20.03.33.png
```

Ces captures locales prouvent que la capture F2 fonctionne, mais elles ne
cadrent pas le portail de facon suffisamment lisible. La premiere est prise sur
l'ecran `Loading terrain...`; la seconde regarde surtout le ciel shaderpack avec
un bord de geometrie clair.

Preuve visuelle fiable :

- capture manuelle fournie dans le chat pendant la Phase 8.4 ;
- portail visible en observation interactive : oui ;
- cadre cyan visible : oui ;
- texture framebuffer/shaderpack visible dans le panneau de portail : oui ;
- plusieurs cadres/portails imbriques visibles : oui ;
- capture automatique Phase 8.3 : non concluante, trop sombre.

## Traversée

La traversee n'a pas ete relancee dans le run final 8.4 pour garder la scene
stable pendant la capture. Elle reste valide via la Phase 8.3, qui a produit :

```text
Client Teleported Statically
```

## Conclusion

Le portail Iris avec shaderpack est officiellement visible en observation
interactive. Le renderer minimal shaderpack fonctionne au niveau preuve
visuelle manuelle :

- portail present cote client ;
- pipeline framebuffer minimal actif ;
- depth mask applique ;
- quad texture soumis ;
- portail visible dans la capture manuelle utilisateur ;
- aucun `Missing program` apres le correctif Phase 8.2 ;
- aucune regression runtime observee.

La preuve automatique reste imparfaite. Les prochaines phases peuvent traiter la
fiabilite du cadrage/capture ou les artefacts visuels, mais pas comme un probleme
d'invisibilite du portail.
