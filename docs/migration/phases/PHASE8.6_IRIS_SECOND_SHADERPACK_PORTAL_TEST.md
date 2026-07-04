# Phase 8.6 - Deuxieme shaderpack Iris avec portail minimal

## Objectif

Tester un deuxieme shaderpack avec le portail minimal Iris pour verifier que le
pont Iris reflectif de la Phase 8.2 fonctionne au-dela de MakeUp-UltraFast, sans
modifier le renderer, les mappings Iris ou les pipelines Immersive Portals.

## Baseline

Commit de depart :

```text
8c09eb37 Stabilize Iris shaderpack minimal portal rendering
```

Baseline conservee :

- shaderpack valide : `MakeUp-UltraFast-9.5c.zip`
- portail visible en observation interactive : oui
- `Missing program` : 0
- renderer avance Iris, shader clipping, Sodium shader mixins, DimLib et
  AlternateDimensions : non actifs

## Shaderpacks locaux

```text
ComplementaryReimagined_r5.8.1.zip
MakeUp-UltraFast-9.5c.zip
```

Shaderpack teste :

```text
ComplementaryReimagined_r5.8.1.zip
```

`run/config/iris.properties` a ete temporairement modifie pour activer
Complementary Reimagined, puis restaure sur :

```text
shaderPack=MakeUp-UltraFast-9.5c.zip
```

## Compilation

- vanilla : `BUILD SUCCESSFUL`
- Sodium compile-only : `BUILD SUCCESSFUL`
- Iris compile-only : `BUILD SUCCESSFUL`

Logs :

```text
compile-phase8.6-vanilla.txt
compile-phase8.6-sodium.txt
compile-phase8.6-iris.txt
```

## Monde de test

Monde :

```text
Phase86IrisSecondShaderpackPortalTest
```

Preparation :

- copie de `Phase81IrisShaderpackPortalTest` ;
- suppression des dossiers `entities` de la copie pour eviter les anciens
  portails ;
- suppression de `session.lock`.

## Run visuel Complementary

Commande :

```powershell
.\gradlew.bat runClient --console=plain -Penable_iris_compat=true -Penable_iris=true --args=--quickPlaySingleplayer=Phase86IrisSecondShaderpackPortalTest
```

Flags :

```text
IMM_PTL_AUTO_VISIBLE_TEST_PORTAL=true
IMM_PTL_AUTO_MINIMAL_TRAVERSAL_TEST=false
IMM_PTL_CAPTURE_MINIMAL_RECURSIVE_PORTAL=false
```

Resultat logs :

- Iris charge : oui
- shaderpack actif : `ComplementaryReimagined_r5.8.1.zip`
- mapping Iris fallback enregistre : oui
- erreurs `Missing program` : 0
- portail cree : oui
- portail present cote client : oui
- `PortalEntityRenderer.submit` appele : oui
- framebuffer minimal atteint : oui
- texture framebuffer disponible : oui, `854x480`
- depth mask applique : oui
- quad texture `SubmitNodeCollector` soumis : oui
- `Duplicate entity UUID` : 0
- `ConcurrentModificationException` : 0
- `Buffer already closed` : 0
- `UnsupportedOperationException` : 0
- crash Minecraft : 0

Observation interactive :

- cadre cyan visible : oui
- rendu framebuffer/shaderpack : instable
- symptome : le rendu clignote et peut etre invisible au moment de la capture
- capture utilisateur : le portail montre le cadre cyan, mais pas le contenu
  framebuffer au moment capture

Le run visuel a ete arrete de facon controlee apres observation ; le code de
sortie Gradle `-1` correspond a cet arret, pas a un crash Minecraft.

## Traversée

Second run avec :

```text
IMM_PTL_AUTO_VISIBLE_TEST_PORTAL=true
IMM_PTL_AUTO_MINIMAL_TRAVERSAL_TEST=true
IMM_PTL_CAPTURE_MINIMAL_RECURSIVE_PORTAL=false
```

Resultat :

- shaderpack actif : `ComplementaryReimagined_r5.8.1.zip`
- mapping Iris fallback enregistre : oui
- portail present cote client : oui
- framebuffer minimal atteint : oui
- `Client Teleported Statically` : oui
- crash Minecraft : 0

Le run a ete arrete de facon controlee apres detection du log de traversee.

## Conclusion

Verdict : partiellement compatible.

Complementary Reimagined valide les couches logiques et pipeline minimal :

- Iris charge avec le shaderpack ;
- aucun `Missing program` ;
- portail cree et synchronise ;
- renderer de portail appele ;
- framebuffer minimal et quad SubmitNodeCollector atteints ;
- traversee fonctionnelle.

Mais le rendu visuel n'est pas stable avec ce shaderpack : le contenu
framebuffer clignote et peut etre invisible en capture. MakeUp-UltraFast reste
la baseline shaderpack visuelle validee. Complementary doit etre traite comme un
cas de compatibilite shaderpack specifique a auditer plus tard, sans modifier la
baseline 8.5.
