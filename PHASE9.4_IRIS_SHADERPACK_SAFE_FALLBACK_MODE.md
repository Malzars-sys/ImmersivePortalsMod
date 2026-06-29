# Phase 9.4 - Iris shaderpack-safe fallback mode

## Objectif

Formaliser `no_depth` comme fallback manuel shaderpack-safe pour le rendu
minimal Iris + shaderpack, sans changer le comportement par defaut global.

Cette phase ne promeut pas automatiquement `no_depth`, ne supprime pas `default`,
`lequal` ou `always`, ne reactive pas le renderer Iris avance, shader clipping,
les mixins shader Sodium, DimLib ou AlternateDimensions.

## Changements

Fichier modifie :

- `src/main/java/qouteall/imm_ptl/core/render/renderer/RendererUsingFrameBuffer.java`

Le parsing de `IMM_PTL_FRAMEBUFFER_DEPTH_MODE` est maintenant explicite :

- `default` : baseline historique, depth mask + textured pass `EQUAL`.
- `no_depth` : fallback shaderpack-safe manuel, robuste mais occlusion reduite.
- `lequal` : mode teste, visible mais intermittent sous Complementary.
- `always` : diagnostic seulement.

Compatibilite conservee :

- `IMM_PTL_FORCE_FRAMEBUFFER_NO_DEPTH_MASK=true` agit encore comme alias de
  `no_depth` uniquement si `IMM_PTL_FRAMEBUFFER_DEPTH_MODE` n'est pas defini.

Valeur invalide :

- ne crash pas ;
- loggue un warning unique ;
- revient a `default` / `depth-masked-equal`.

Nouveaux logs non spammy :

- mode choisi ;
- test de profondeur ;
- origine du mode :
  - `default implicit` ;
  - `IMM_PTL_FRAMEBUFFER_DEPTH_MODE` ;
  - `IMM_PTL_FORCE_FRAMEBUFFER_NO_DEPTH_MASK` ;
  - `invalid IMM_PTL_FRAMEBUFFER_DEPTH_MODE fallback`.
- warning quand `no_depth` est actif :

```text
Minimal recursive portal no_depth is a compatibility fallback and may reduce portal occlusion.
```

## Validations compilation

- `compile-phase9.4-vanilla.txt` : BUILD SUCCESSFUL
- `compile-phase9.4-sodium.txt` : BUILD SUCCESSFUL
- `compile-phase9.4-iris.txt` : BUILD SUCCESSFUL

## Tests runtime

Flags communs :

```text
IMM_PTL_AUTO_VISIBLE_TEST_PORTAL=true
IMM_PTL_AUTO_MINIMAL_TRAVERSAL_TEST=false
IMM_PTL_CAPTURE_MINIMAL_RECURSIVE_PORTAL=false
```

### A. MakeUp default

Log :

- `runclient-phase9.4-makeup-default.txt`

Resultat :

- shaderpack : `MakeUp-UltraFast-9.5c.zip`
- mode : `default`
- source : `default implicit`
- pipeline : `depth-masked-equal`
- framebuffer : `854x480`
- quad SubmitNodeCollector soumis : oui
- portail present cote client : oui

### B. Complementary no_depth

Log :

- `runclient-phase9.4-complementary-no-depth.txt`

Resultat :

- shaderpack : `ComplementaryReimagined_r5.8.1.zip`
- mode : `no_depth`
- source : `IMM_PTL_FRAMEBUFFER_DEPTH_MODE`
- warning fallback compatibilite : oui
- pipeline : `non-depth-masked`
- depth mask intentionnellement desactive : oui
- framebuffer : `854x480`
- quad SubmitNodeCollector soumis : oui
- portail present cote client : oui

### C. Valeur invalide

Log :

- `runclient-phase9.4-invalid-depth-mode.txt`

Configuration :

```text
IMM_PTL_FRAMEBUFFER_DEPTH_MODE=banana
```

Resultat :

- shaderpack : `MakeUp-UltraFast-9.5c.zip`
- warning unique : `Unknown IMM_PTL_FRAMEBUFFER_DEPTH_MODE 'banana'`
- fallback : `default`
- source : `invalid IMM_PTL_FRAMEBUFFER_DEPTH_MODE fallback`
- pipeline : `depth-masked-equal`
- framebuffer : `854x480`
- quad SubmitNodeCollector soumis : oui
- portail present cote client : oui

## Erreurs runtime

Sur les trois runs :

- `Missing program` : 0
- `Buffer already closed` : 0
- `ConcurrentModificationException` : 0
- `Duplicate entity UUID` : 0
- `UnsupportedOperationException` : 0
- crash Minecraft : 0

## Etat final

`run/config/iris.properties` a ete restaure sur :

```text
shaderPack=MakeUp-UltraFast-9.5c.zip
```

Aucun flag global `IMM_PTL_FRAMEBUFFER_DEPTH_MODE` ou
`IMM_PTL_FORCE_FRAMEBUFFER_NO_DEPTH_MASK` n'a ete laisse actif.

## Conclusion

`no_depth` est maintenant un fallback manuel propre, documente dans les logs et
sur contre les valeurs invalides. Il reste volontairement manuel/dev : le mode
par defaut global reste `default` / `depth-masked-equal`.

Pour Complementary et les shaderpacks similaires, `no_depth` est le fallback le
plus robuste connu, avec le compromis explicite d'une occlusion moins stricte.
