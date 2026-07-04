# Phase 13.0 - Vanilla Portal Render Visual Audit

Date: 2026-07-04

Release baseline:

- Branch: `1.21`
- Release tag: `v7.0.0-alpha.1-mc26.1-vanilla`
- Release commit: `ce44e6c33818033292f7ef3ca8832601bd7f1fb4`

## Scope

This was an audit-only phase. No renderer, runtime, Sodium, Iris, DimLib,
AlternateDimensions, shaderpack, chunk tracking, tag, or release state was changed.

## Initial State

- `git status --short`: clean before the audit runs.
- `git log --oneline -5`:
  - `ce44e6c3 Commit validated dimension traversal runtime fixes`
  - `f976e263 Organize migration documentation`
  - `1567a11a Document vanilla alpha release candidate`
  - `e76ea09b Document external vanilla alpha smoke test`
  - `de59ac04 Prepare minimal vanilla alpha package`
- `git rev-parse HEAD`: `ce44e6c33818033292f7ef3ca8832601bd7f1fb4`

## Compilation

Command:

```powershell
.\gradlew.bat compileJava processResources --console=plain
```

Result: `BUILD SUCCESSFUL`.

## Test Runs

### Absent Quick-Play World

Runs against missing worlds (`Phase130VanillaVisualAudit`,
`Phase130VanillaVisualAudit2`) did not reach the world. Minecraft loaded to the
menu/resource stage because `--quickPlaySingleplayer` does not create a missing
save automatically.

### Clean Control Copy

To get a real world without reusing a portal-polluted save, a local audit-only
copy was made:

- Source: `run/saves/Phase116OverworldControl`
- Audit copy: `run/saves/Phase130VanillaVisualAuditClean`

`rg -a` did not find Immersive Portals strings in the selected control saves
before the copy. The audit copy is a run artifact and should not be committed.

Command shape:

```powershell
IMM_PTL_AUTO_VISIBLE_TEST_PORTAL=true
IMM_PTL_CAPTURE_MINIMAL_RECURSIVE_PORTAL=true
IMM_PTL_MINIMAL_RECURSIVE_PORTAL_SCREENSHOT=phase13.0-vanilla-visual-audit.png
.\gradlew.bat runClient --args="--quickPlaySingleplayer Phase130VanillaVisualAuditClean" --console=plain
```

The client was stopped after the audit evidence was generated.

## Runtime Evidence

Relevant log evidence from `runclient-phase13.0-vanilla-visual-audit-clean.txt`:

- Minecraft 26.1 loaded with Fabric Loader 0.19.3.
- `immersive_portals 7.0.0-alpha.1` loaded.
- Sodium was not loaded.
- Iris was not loaded.
- Dimensional Threading / DimLib was not present.
- Dev auto visible portal command ran.
- Minimal test portal created:
  - origin: `(-515.5, 85.5, -82.5)` in `minecraft:overworld`
  - destination: `(-515.5, 85.5, -72.5)` in `minecraft:overworld`
  - portal id: `251`
- `PortalEntityRenderer` queued the minimal recursive portal.
- `IEGameRenderer`: active.
- `IECamera`: active.
- `IEWorldRenderer`: active.
- `IEParticleManager`: active.
- Secondary framebuffer initialized.
- Vanilla fog fallback used.
- Minimal recursive render began in `minecraft:overworld`.
- Main depth: available.
- Secondary depth: available.
- Public stencil: unavailable.
- Framebuffer texture available: `854x480`.
- Pipeline mode: `depth-masked-equal`.
- Depth mask applied: yes.
- Textured quad submitted through `SubmitNodeCollector`: yes.
- Capture generated: `run/screenshots/phase13.0-vanilla-visual-audit.png`.

Noise / non-blocking log entries:

- Realms/authentication failures from the dev/offline session.
- `IPModInfoChecking` 404.
- The framebuffer code logged `MakeUp-UltraFast-9.5c.zip` from Iris config
  detection, but Iris itself was not in the loaded mod list. Treat this as a
  misleading config-detection note, not an active shaderpack in this vanilla run.

Error counters in the clean-control run:

- `Buffer already closed`: 0
- `ConcurrentModificationException`: 0
- `Duplicate entity UUID`: 0
- `Missing program`: 0
- `Network Protocol Error`: 0
- `Error deserializing chunk packet`: 0
- `UnsupportedOperationException`: 0
- Crash: 0
- `ERROR`: 2, both known non-render network/auth/update noise.

## Visual Evidence

### Framebuffer Capture

Capture:

```text
run/screenshots/phase13.0-vanilla-visual-audit.png
```

Observation:

- The destination framebuffer content is visible.
- The image is upright.
- The captured terrain is not obviously inverted.
- No black border is visible in this framebuffer capture.
- This capture does not show the portal frame in the main world view; it appears
  to be the captured destination content, not a full on-screen portal-in-frame
  proof.

### Window Captures

Two window-level capture attempts were made without modifying code:

- `run/screenshots/phase13.0-vanilla-visual-audit-printwindow.png`
- `run/screenshots/phase13.0-vanilla-visual-audit-phase50-printwindow.png`

The clean-control capture shows Minecraft, but the portal is not visible. The
player appears to be under or near dense leaves, so the automatically created
portal is likely obstructed or outside the visible portion of the captured view.

The historical `Phase50Test` capture is also not a clean proof. It shows a dark
foliage-heavy view with only a small cyan edge at the right side, and that world
is known to be polluted by previous portal experiments.

Conclusion: the runtime path and destination framebuffer are proven, but this
phase did not obtain a clean full-window visual proof of the portal quad inside
the cyan frame.

## Render Geometry Assessment

- Portal visible: runtime yes; clean full-window visual proof not obtained.
- Cyan frame aligned: not conclusively auditable from the clean-control capture.
- Destination view visible: yes, in the framebuffer capture and logs.
- Quad framebuffer placement: runtime submitted, but not visually proven in the
  full-window capture.
- Orientation: destination framebuffer appears upright; main quad orientation is
  not conclusively revalidated in this phase.
- Depth / occlusion: depth mask applied; visual correctness not conclusively
  revalidated.
- Clipping: still expected to be incomplete from Phase 10.x conclusions.
- Recursion: still minimal / single-layer.
- Playability: remains a proof-grade minimal renderer, not a polished complete
  Immersive Portals renderer.

## API / Reuse Audit for Future Mods

Target use case: another mod, for example WHA / Witch Hate Atelier, draws a
magic pentacle on the ground and opens a portal toward a matching pentacle.

Already reusable or close to reusable:

- Server-side portal entity creation through `Portal.ENTITY_TYPE`.
- Setting origin, destination dimension, destination position, orientation, and
  size.
- Same-dimension and vanilla interdimension traversal paths validated in earlier
  Phase 11.x work.
- Portal synchronization to the client.
- Minimal cyan-frame fallback renderer.
- Minimal framebuffer-backed destination view.

Too internal or fragile for public API exposure as-is:

- Direct `Portal` field mutation order.
- Renderer queueing through `PortalEntityRenderer` / `RendererUsingFrameBuffer`.
- Environment-variable driven QA flags.
- Dev-only commands in `imm_ptl_debug`.
- Internal framebuffer/depth/shaderpack fallback modes.
- Low-level chunk packet and world-switch recovery details.

Future public API candidates:

- `PortalApi.createPortal(...)` style builder with explicit:
  - source dimension;
  - source center;
  - destination dimension;
  - destination center;
  - width / height;
  - axis vectors or plane normal;
  - tag / owner mod id.
- A horizontal portal helper:
  - source pentacle center;
  - destination pentacle center;
  - up-facing or down-facing plane;
  - collision policy;
  - persistence policy.
- A safe save/sync helper that hides the raw entity spawn details.
- A renderer capability query:
  - minimal framebuffer available;
  - advanced clipping unavailable;
  - Sodium/Iris mode active;
  - shaderpack fallback mode.

Do not expose directly:

- Internal framebuffer texture aliases.
- `SubmitNodeCollector` bridge internals.
- `RendererUsingFrameBuffer` implementation knobs.
- Packet redirection internals.
- Raw mixin duck interfaces.
- Chunk map recovery heuristics.

Current blockers for a horizontal magic portal API:

- The minimal renderer is oriented around rectangular portal planes, but the
  public API needs a clean way to define horizontal planes.
- Full clipping is incomplete; a ground portal would visibly need stronger
  clipping/occlusion than a vertical debug rectangle.
- The current minimal frame is cyan/debug-like, not an API-level visual surface.
- The dev command path is not suitable for production mods.
- Renderer behavior under shaderpacks remains fallback-based, not final.

## Recommendation

Phase 13.0 result:

- Render geometry clean: partial / not fully proven by this audit.
- Destination view visible: yes, at framebuffer level.
- Full on-screen portal quad proof: inconclusive in this automated audit.
- Main remaining defects:
  - clean QA world/camera setup is not robust enough;
  - clipping remains incomplete;
  - depth/occlusion needs focused visual validation;
  - API surface for external mods is still too internal.

Recommended next micro-phase:

**E. Expose or improve a QA command / harness outside fragile camera conditions.**

Before fixing geometry, create a reliable visual QA setup that:

- places the player in an open area;
- clears immediate obstructions or uses a controlled flat platform;
- creates exactly one portal;
- points the camera at the portal;
- optionally captures a full-window proof;
- reports the portal id being audited.

After that, use the improved QA harness to choose between:

- A. correcting geometry/quad if the frame mapping is wrong;
- B. correcting depth/occlusion if the quad is placed but visually unstable;
- C. revisiting clipping with a larger design phase.
