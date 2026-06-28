# Phase 9.2 - Iris shaderpack-safe framebuffer depth modes

## Objectif

Concevoir un chemin framebuffer plus robuste pour les shaderpacks Iris sans
modifier le renderer avance, sans reactiver les mixins shader Sodium, sans
shader clipping, sans DimLib et sans AlternateDimensions.

La Phase 9.1 avait confirme que le clignotement Complementary etait fortement
lie au masque de profondeur minimal `CompareOp.EQUAL`. Cette phase transforme
l'experience booleenne en modes de profondeur explicites et comparables.

## Changements

Fichiers modifies :

- `src/main/java/qouteall/imm_ptl/core/render/renderer/RendererUsingFrameBuffer.java`
- `src/main/java/qouteall/imm_ptl/core/render/pipeline/IPRenderPipelines.java`
- `MIGRATION_PLAN_26.1.md`

Nouveau flag :

```text
IMM_PTL_FRAMEBUFFER_DEPTH_MODE=<mode>
```

Modes disponibles :

- `default` : comportement historique, masque profondeur + textured pass
  `CompareOp.EQUAL`.
- `no_depth` : pas de masque profondeur, textured pass direct.
- `lequal` : masque profondeur + textured pass `CompareOp.LESS_THAN_OR_EQUAL`.
- `always` : masque profondeur + textured pass `CompareOp.ALWAYS_PASS`.

Compatibilite Phase 9.1 conservee :

- `IMM_PTL_FORCE_FRAMEBUFFER_NO_DEPTH_MASK=true` reste pris en charge comme
  alias de `no_depth` quand `IMM_PTL_FRAMEBUFFER_DEPTH_MODE` n'est pas defini.

Le mode par defaut reste strictement `default`, donc la baseline MakeUp/Iris
existante n'est pas changee.

## Details techniques

`IPRenderPipelines` enregistre maintenant deux pipelines texturés de test :

- `pipeline/imm_ptl_draw_framebuffer_in_area_depth_lequal`
- `pipeline/imm_ptl_draw_framebuffer_in_area_depth_always`

Ces pipelines restent mappes vers `ShaderKey.TEXTURED` via le pont reflectif
Iris ajoute en Phase 8.2.

`RendererUsingFrameBuffer` choisit le render type selon le mode demande et log
une seule fois :

- le mode de profondeur ;
- le test de profondeur ;
- le mode de pipeline soumis.

## Validations compilation

- `compile-phase9.2-vanilla.txt` : BUILD SUCCESSFUL
- `compile-phase9.2-sodium.txt` : BUILD SUCCESSFUL
- `compile-phase9.2-iris.txt` : BUILD SUCCESSFUL

## Runs Iris shaderpack

Les runs ont ete lances avec :

```text
IMM_PTL_AUTO_VISIBLE_TEST_PORTAL=true
IMM_PTL_AUTO_MINIMAL_TRAVERSAL_TEST=false
IMM_PTL_CAPTURE_MINIMAL_RECURSIVE_PORTAL=false
```

La fenetre client a ete arretee par le harnais apres observation des logs clefs.
Les logs ne montrent pas de crash Minecraft, pas de `Buffer already closed`, pas
de `ConcurrentModificationException`, pas de `Duplicate entity UUID` et pas
d'`UnsupportedOperationException`.

| Run | Shaderpack | Mode | Pipeline log | Resultat runtime |
| --- | --- | --- | --- | --- |
| `runclient-phase9.2-makeup-default.txt` | MakeUp-UltraFast-9.5c.zip | `default` | `depth-masked-equal` | portail client present, quad soumis |
| `runclient-phase9.2-complementary-default.txt` | ComplementaryReimagined_r5.8.1.zip | `default` | `depth-masked-equal` | portail client present, quad soumis |
| `runclient-phase9.2-complementary-no-depth.txt` | ComplementaryReimagined_r5.8.1.zip | `no_depth` | `non-depth-masked` | portail client present, quad soumis |
| `runclient-phase9.2-complementary-lequal.txt` | ComplementaryReimagined_r5.8.1.zip | `lequal` | `depth-masked-lequal` | portail client present, quad soumis |
| `runclient-phase9.2-complementary-always.txt` | ComplementaryReimagined_r5.8.1.zip | `always` | `depth-masked-always` | portail client present, quad soumis |

## Analyse

Le diagnostic Phase 9.1 reste valide : le couple masque profondeur + comparaison
stricte `EQUAL` est le suspect principal du clignotement Complementary.

Les nouveaux modes donnent trois chemins de comparaison :

- `no_depth` est le fallback le plus robuste contre le clignotement, mais il
  sacrifie l'occlusion minimale.
- `lequal` est le meilleur candidat technique pour un mode shaderpack-safe :
  il conserve une relation avec le masque de profondeur tout en evitant la
  comparaison trop fragile `EQUAL`.
- `always` est un mode de diagnostic utile : il garde la structure avec masque,
  mais rend le textured pass permissif.

Cette phase n'a pas produit de capture visuelle comparative. Les cinq modes sont
compilables et atteignent le chemin runtime attendu ; la validation visuelle de
stabilite doit etre faite en phase suivante, idealement en comparant
Complementary `default`, `lequal` et `always`.

## Etat final

- Vanilla compile : OK.
- Sodium compile-only : OK.
- Iris compile-only : OK.
- Iris runtime avec MakeUp default : chemin portail atteint.
- Iris runtime avec Complementary default/no_depth/lequal/always : chemins
  portail atteints.
- `run/config/iris.properties` restaure sur `MakeUp-UltraFast-9.5c.zip`.
- Renderer avance Iris : non reactive.
- Sodium shader mixins : non reactives.
- DimLib / AlternateDimensions : non reactives.
- Shader clipping / stencil avance : non reactives.

## Recommandation

Phase 9.3 devrait etre une validation visuelle manuelle ciblee :

1. Complementary `default` comme temoin du clignotement.
2. Complementary `lequal` comme candidat principal.
3. Complementary `always` comme diagnostic permissif.
4. `no_depth` conserve comme fallback shaderpack-safe de secours.

Ne pas promouvoir `lequal` comme defaut global avant preuve visuelle.
