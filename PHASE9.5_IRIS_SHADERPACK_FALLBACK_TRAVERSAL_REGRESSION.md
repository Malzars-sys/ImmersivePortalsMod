# Phase 9.5 - Iris shaderpack fallback traversal regression

## Objectif

Verifier que la baseline Iris + MakeUp en mode `default` et le fallback manuel
Iris + Complementary en mode `no_depth` permettent toujours la traversee du
portail sans crash ni regression runtime.

Cette phase ne modifie pas le renderer, ne change pas le mode par defaut, ne
promeut pas `no_depth` automatiquement, et ne reactive pas le renderer Iris
avance, shader clipping, les mixins shader Sodium, DimLib ou AlternateDimensions.

## Etat initial

Commits recents observes :

- `57968c54 Formalize manual Iris shaderpack-safe framebuffer fallback`
- `603d5020 Document LEQUAL visual instability under Complementary`
- `9a5e231a Add experimental Iris framebuffer depth modes`
- `76c2e497 Add dev toggle for portal framebuffer depth mask experiment`
- `a8803f1e Document Iris shaderpack framebuffer flicker audit`
- `45fc13b7 Document second Iris shaderpack portal test`
- `8c09eb37 Stabilize Iris shaderpack minimal portal rendering`

`run/config/iris.properties` etait au depart sur :

```text
shaderPack=MakeUp-UltraFast-9.5c.zip
```

## Validations compilation

- `compile-phase9.5-vanilla.txt` : BUILD SUCCESSFUL
- `compile-phase9.5-sodium.txt` : BUILD SUCCESSFUL
- `compile-phase9.5-iris.txt` : BUILD SUCCESSFUL

Note : PowerShell peut classer les warnings JVM `restricted method` en
`NativeCommandError`, mais les trois logs Gradle finissent bien par
`BUILD SUCCESSFUL`.

## Tests runtime

Flags communs :

```text
IMM_PTL_AUTO_VISIBLE_TEST_PORTAL=true
IMM_PTL_AUTO_MINIMAL_TRAVERSAL_TEST=true
IMM_PTL_CAPTURE_MINIMAL_RECURSIVE_PORTAL=false
```

### A. MakeUp default + traversee

Monde :

- `Phase95MakeUpDefaultTraversal`

Log :

- `runclient-phase9.5-makeup-default-traversal.txt`

Resultat :

- shaderpack actif : `MakeUp-UltraFast-9.5c.zip`
- mode framebuffer : `default`
- source : `default implicit`
- pipeline : `depth-masked-equal`
- framebuffer : `854x480`
- quad SubmitNodeCollector soumis : oui
- portail cree : oui
- portail present cote client : oui
- traversee automatique lancee : oui
- `Client Teleported Statically` : oui

### B. Complementary no_depth + traversee

Monde :

- `Phase95ComplementaryNoDepthTraversal`

Log :

- `runclient-phase9.5-complementary-no-depth-traversal.txt`

Resultat :

- shaderpack actif : `ComplementaryReimagined_r5.8.1.zip`
- mode framebuffer : `no_depth`
- source : `IMM_PTL_FRAMEBUFFER_DEPTH_MODE`
- warning compatibilite `no_depth` : oui
- pipeline : `non-depth-masked`
- depth mask intentionnellement desactive : oui
- framebuffer : `854x480`
- quad SubmitNodeCollector soumis : oui
- portail cree : oui
- portail present cote client : oui
- traversee automatique lancee : oui
- `Client Teleported Statically` : oui

### C. Complementary alias historique + traversee

Monde :

- `Phase95ComplementaryLegacyAliasTraversal`

Configuration :

```text
IMM_PTL_FRAMEBUFFER_DEPTH_MODE absent
IMM_PTL_FORCE_FRAMEBUFFER_NO_DEPTH_MASK=true
```

Log :

- `runclient-phase9.5-complementary-legacy-alias-traversal.txt`

Resultat :

- shaderpack actif : `ComplementaryReimagined_r5.8.1.zip`
- mode reellement utilise : `no_depth`
- source : `IMM_PTL_FORCE_FRAMEBUFFER_NO_DEPTH_MASK`
- warning compatibilite `no_depth` : oui
- pipeline : `non-depth-masked`
- framebuffer : `854x480`
- quad SubmitNodeCollector soumis : oui
- portail cree : oui
- portail present cote client : oui
- traversee automatique lancee : oui
- `Client Teleported Statically` : oui

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

Aucun flag global `IMM_PTL_FRAMEBUFFER_DEPTH_MODE`,
`IMM_PTL_FORCE_FRAMEBUFFER_NO_DEPTH_MASK`,
`IMM_PTL_AUTO_VISIBLE_TEST_PORTAL`,
`IMM_PTL_AUTO_MINIMAL_TRAVERSAL_TEST` ou
`IMM_PTL_CAPTURE_MINIMAL_RECURSIVE_PORTAL` n'a ete laisse actif.

## Conclusion

La baseline MakeUp `default` et le fallback manuel Complementary `no_depth`
permettent encore la traversee du portail.

L'ancien alias `IMM_PTL_FORCE_FRAMEBUFFER_NO_DEPTH_MASK=true` reste egalement
fonctionnel et traverse correctement. Le fallback `no_depth` manuel est donc
valide cote traversal/runtime, avec le compromis visuel deja documente :
occlusion moins stricte mais meilleure robustesse shaderpack.
