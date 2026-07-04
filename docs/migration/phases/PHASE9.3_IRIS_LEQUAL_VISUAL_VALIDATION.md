# Phase 9.3 - Validation visuelle du mode LEQUAL shaderpack-safe

## Objectif

Verifier si `IMM_PTL_FRAMEBUFFER_DEPTH_MODE=lequal` peut devenir le candidat
shaderpack-safe principal pour Iris + Complementary, sans changer le mode par
defaut global.

Cette phase ne modifie pas le renderer, ne reactive pas le renderer Iris avance,
ne reactive pas shader clipping, ne reactive pas les mixins shader Sodium et ne
touche pas DimLib/AlternateDimensions.

## Etat Git et contexte

Commits recents observes :

- `9a5e231a Add experimental Iris framebuffer depth modes`
- `76c2e497 Add dev toggle for portal framebuffer depth mask experiment`
- `a8803f1e Document Iris shaderpack framebuffer flicker audit`
- `45fc13b7 Document second Iris shaderpack portal test`
- `8c09eb37 Stabilize Iris shaderpack minimal portal rendering`

`run/config/iris.properties` etait au depart sur :

```text
shaderPack=MakeUp-UltraFast-9.5c.zip
```

Il a ete restaure sur cette valeur a la fin.

## Mondes prepares

Les mondes suivants ont ete prepares depuis `Phase81IrisShaderpackPortalTest` :

- `Phase93MakeUpDefaultVisual`
- `Phase93ComplementaryDefaultVisual`
- `Phase93ComplementaryLequalVisual`
- `Phase93ComplementaryNoDepthVisual`

Les `session.lock` et dossiers `entities` generes ont ete supprimes lors de la
preparation.

## Flags de test

Tous les runs ont utilise :

```text
IMM_PTL_AUTO_VISIBLE_TEST_PORTAL=true
IMM_PTL_AUTO_MINIMAL_TRAVERSAL_TEST=false
IMM_PTL_CAPTURE_MINIMAL_RECURSIVE_PORTAL=false
```

## Resultats runtime

| Test | Shaderpack | Mode | Pipeline | Resultat |
| --- | --- | --- | --- | --- |
| MakeUp default | `MakeUp-UltraFast-9.5c.zip` | `default` | `depth-masked-equal` | portail cree, present client, framebuffer `854x480`, quad soumis |
| Complementary default | `ComplementaryReimagined_r5.8.1.zip` | `default` | `depth-masked-equal` | portail cree, present client, framebuffer `854x480`, quad soumis |
| Complementary lequal | `ComplementaryReimagined_r5.8.1.zip` | `lequal` | `depth-masked-lequal` | portail cree, present client, framebuffer `854x480`, quad soumis |
| Complementary no_depth | `ComplementaryReimagined_r5.8.1.zip` | `no_depth` | `non-depth-masked` | portail cree, present client, framebuffer `854x480`, quad soumis |

Signatures d'erreur dans les logs Phase 9.3 :

- `Buffer already closed` : 0
- `ConcurrentModificationException` : 0
- `Duplicate entity UUID` : 0
- `UnsupportedOperationException` : 0
- `Missing program` : 0

## Captures

Captures F2 obtenues :

- `run/screenshots/2026-06-29_01.30.26.png`
- `run/screenshots/2026-06-29_01.31.45.png`

Ces deux captures ne sont pas concluantes : elles montrent l'ecran
`Loading terrain...` avec un carre blanc central, pas le portail en observation.
Les logs indiquent que la capture a ete prise alors que Minecraft attendait
encore les chunks client.

Une tentative de capture Windows ciblee a produit :

- `phase9.3-complementary-lequal-window.png`
- `phase9.3-complementary-lequal-postload-window.png`

Ces captures ne sont pas utilisables comme preuve visuelle du portail :

- la premiere a capture une autre fenetre au premier plan ;
- la seconde a capture un onglet terminal intitule par le run, pas la surface
  Minecraft.

## Observation importante

Les logs montrent que le portail est bien cree et oriente par la commande dev :

```text
Placed player ... facing visible minimal test portal ...
Created minimal test portal ...
Portal present client-side under Sodium: true
```

Cependant, l'automatisation de capture n'a pas fourni une image fiable de la
surface Minecraft avec le portail dans le champ. La validation visuelle stricte
du clignotement `default` vs `lequal` reste donc inconclusive dans cette session.

## Interpretation

Ce que Phase 9.3 valide solidement :

- `lequal` compile et fonctionne au runtime avec Complementary ;
- `lequal` garde le depth mask et le textured pass ;
- le chemin framebuffer atteint la texture `854x480` ;
- le quad SubmitNodeCollector est soumis ;
- aucune erreur runtime interdite n'est observee ;
- le mode par defaut global n'a pas ete change.

Ce que Phase 9.3 ne valide pas encore :

- stabilite visuelle du contenu framebuffer sous Complementary `lequal` ;
- reduction effective du clignotement par rapport a `default` ;
- qualite d'occlusion visuelle par rapport a `no_depth`.

## Conclusion

`lequal` reste le meilleur candidat technique shaderpack-safe, mais il ne doit
pas encore etre promu comme strategie par defaut ou automatique.

La prochaine phase doit etre une validation manuelle ou semi-manuelle plus
fiable de la fenetre Minecraft au premier plan, idealement avec :

1. attente explicite apres la sortie de `Loading terrain...` ;
2. verification humaine que le portail est dans le champ ;
3. capture F2 manuelle ou capture window handle ciblee sur la vraie fenetre
   LWJGL Minecraft ;
4. comparaison directe :
   - Complementary `default` ;
   - Complementary `lequal` ;
   - Complementary `no_depth`.

`no_depth` reste le fallback de compatibilite robuste connu. `lequal` reste le
candidat principal, mais la preuve visuelle decisive manque encore.
