# Phase 8.0B - Iris shaderpack world smoke test reprise

Date : 28 juin 2026

## Objectif

Reprendre le test Iris runtime avec shaderpack local, sans portail, apres la
Phase 8.0 qui s'etait arretee parce que `run/shaderpacks` etait vide.

## Baseline verifiee

`git log --oneline -3` confirme :

- `bd3a080c` - `Stabilize Iris no-shaderpack portal rendering baseline`
- `545169c1` - `Stabilize Sodium non-shader portal rendering baseline`
- `5545115e` - `Stabilize vanilla minimal portal renderer baseline`

## Verification shaderpack local

Commande :

```powershell
Get-ChildItem run\shaderpacks -Force
```

Resultat :

```text
COUNT=0
```

Aucun shaderpack `.zip` local n'est present dans `run/shaderpacks`.

## Decision

La Phase 8.0B ne peut pas lancer le test runtime shaderpack, car la
precondition manuelle n'est pas remplie.

Conformement a la consigne :

- aucun shaderpack n'a ete telecharge automatiquement ;
- aucun shaderpack n'a ete selectionne ;
- aucune configuration Iris shaderpack n'a ete modifiee ;
- aucun monde shaderpack n'a ete lance ;
- aucun portail n'a ete cree ou teste.

## Variables dev portail

Les variables suivantes ne sont pas presentes dans l'environnement courant :

- `IMM_PTL_AUTO_VISIBLE_TEST_PORTAL`
- `IMM_PTL_AUTO_MINIMAL_TRAVERSAL_TEST`
- `IMM_PTL_CAPTURE_MINIMAL_RECURSIVE_PORTAL`
- `IMM_PTL_MINIMAL_RECURSIVE_PORTAL_SCREENSHOT`

## Validations non relancees

Les compilations et le runtime Iris sans shaderpack avaient deja ete valides
dans la Phase 8.0 juste avant cette reprise. Pour 8.0B, le blocage intervient
avant compilation/runtime shaderpack, a la verification du dossier
`run/shaderpacks`.

## Blocage exact

Placer manuellement un seul shaderpack `.zip` dans :

```text
run/shaderpacks/
```

Puis relancer la Phase 8.0B.

## Livrables

- `PHASE8.0B_IRIS_SHADERPACK_WORLD_SMOKE_TEST.md`
- `MIGRATION_PLAN_26.1.md`
- `git-diff-phase8.0B.txt`
